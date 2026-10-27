package com.iot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.iot.model.ActiveHistory;
import com.iot.model.Device;
import com.iot.repository.ActiveHistoryRepository;
import com.iot.repository.DeviceRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Service
public class CommandService {
    public static final String LOADING = "LOADING", SUCCESS = "SUCCESS", FAILURE = "FAILURE";
    public record SendCommand(String commandId, String deviceCode, String action) {}
    private final ActiveHistoryRepository histories;
    private final DeviceRepository devices;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate tx;
    private final ScheduledExecutorService timers = Executors.newScheduledThreadPool(2);
    private final ConcurrentMap<String, Long> deadlines = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, SendCommand> legacyCommands = new ConcurrentHashMap<>();
    private final long timeoutMs;

    public CommandService(ActiveHistoryRepository histories, DeviceRepository devices,
                          ApplicationEventPublisher events, PlatformTransactionManager manager,
                          @Value("${mqtt.command.timeout-ms:5000}") long timeoutMs) {
        if (timeoutMs <= 0) throw new IllegalArgumentException("Command timeout must be positive");
        this.histories = histories;
        this.devices = devices;
        this.events = events;
        this.tx = new TransactionTemplate(manager);
        this.timeoutMs = timeoutMs;
    }

    @PostConstruct
    public void recover() {
        // Single backend instance: interrupted commands cannot be safely replayed.
        tx.executeWithoutResult(s -> histories.findByStatus(LOADING).forEach(h -> {
            h.setStatus(FAILURE);
            histories.save(h);
            log.warn("Command {} failed: backend restarted before confirmation", h.getCommandId());
        }));
        timers.scheduleWithFixedDelay(this::expireCommands, 100, 100, TimeUnit.MILLISECONDS);
    }

    private void expireCommands() {
        deadlines.forEach((id, deadline) -> {
            if (System.nanoTime() < deadline) return;
            try {
                fail(id, "Timeout: chưa nhận xác nhận ESP32");
                deadlines.remove(id, deadline);
                legacyCommands.remove(id);
            } catch (Exception e) {
                log.error("Could not expire command {}; will retry", id, e);
            }
        });
    }

    public Map<String, Object> accept(Long deviceId, String state, Long userId) {
        String action = state == null ? "" : state.trim().toUpperCase(java.util.Locale.ROOT);
        if (!action.equals("ON") && !action.equals("OFF"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lệnh phải là ON hoặc OFF");
        return tx.execute(s -> {
            Device device = devices.lockById(deviceId).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thiết bị"));
            String code = LedProtocol.deviceCode(device);
            if (histories.existsByDeviceIdAndStatus(deviceId, LOADING))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Thiết bị đang xử lý lệnh");
            ActiveHistory h = new ActiveHistory();
            h.setDeviceId(deviceId);
            h.setUserId(userId);
            h.setAction(action);
            h.setStatus(LOADING);
            h.setExecutedAt(LocalDateTime.now());
            histories.saveAndFlush(h);
            SendCommand command = new SendCommand(h.getCommandId(), code, action);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { events.publishEvent(command); }
            });
            return Map.<String, Object>of("historyId", h.getId(), "commandId", h.getCommandId(),
                    "deviceId", deviceId, "action", action, "status", LOADING);
        });
    }

    // Called by the background publisher immediately before attempting MQTT publish.
    public void sending(String commandId) {
        deadlines.put(commandId, System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs));
    }

    public void sendingLegacy(SendCommand command) {
        sending(command.commandId());
        legacyCommands.put(command.commandId(), command);
    }

    public void fail(String commandId, String reason) {
        complete(commandId, null, reason);
    }

    public void acknowledge(JsonNode ack) {
        if (!ack.path("commandId").isTextual() || !ack.path("deviceCode").isTextual()
                || !ack.path("status").isTextual() || !ack.path("success").isBoolean()) return;
        complete(ack.get("commandId").asText(), ack, "ESP32 báo thực hiện thất bại");
    }

    public void synchronizeLegacyState(JsonNode telemetry) {
        // Capture before opening the transaction: a queued old packet must not
        // confirm a command registered while this callback waits for database locks.
        var awaiting = java.util.List.copyOf(legacyCommands.values());
        var completed = new java.util.ArrayList<String>();
        tx.executeWithoutResult(s -> {
            for (Long id : devices.findDeviceIds().stream().sorted().toList()) {
                Device device = devices.lockById(id).orElse(null);
                if (device == null) continue;
                final String code;
                try { code = LedProtocol.deviceCode(device); }
                catch (ResponseStatusException unsupported) { continue; }
                JsonNode value = telemetry.path(code.toLowerCase(java.util.Locale.ROOT));
                if (!value.isTextual()) continue;
                String state = value.textValue().trim().toUpperCase(java.util.Locale.ROOT);
                if (!state.equals("ON") && !state.equals("OFF")) continue;
                device.setCurrentStatus(state);
                devices.save(device);
                for (SendCommand command : awaiting) {
                    if (!code.equals(command.deviceCode()) || !state.equals(command.action())) continue;
                    Long deadline = deadlines.get(command.commandId());
                    if (deadline == null || System.nanoTime() >= deadline) continue;
                    Long historyId = parseHistoryId(command.commandId());
                    if (historyId == null) continue;
                    ActiveHistory h = histories.lockById(historyId).orElse(null);
                    if (h == null || !id.equals(h.getDeviceId()) || !LOADING.equals(h.getStatus())
                            || !state.equals(h.getAction())) continue;
                    h.setStatus(SUCCESS);
                    histories.save(h);
                    completed.add(command.commandId());
                    log.info("Command {} confirmed SUCCESS by legacy ESP32 status: {}={}",
                            command.commandId(), code, state);
                }
            }
        });
        // Remove only after successful commit; a failed transaction remains retryable.
        completed.forEach(legacyCommands::remove);
    }

    private Long parseHistoryId(String commandId) {
        if (commandId == null || !commandId.matches("CMD-[1-9][0-9]{0,18}")) return null;
        try { return Long.parseLong(commandId.substring(4)); }
        catch (NumberFormatException e) { return null; }
    }

    private void complete(String commandId, JsonNode ack, String failure) {
        Long historyId = parseHistoryId(commandId);
        if (historyId == null) return;
        tx.executeWithoutResult(s -> {
            Long deviceId = histories.findDeviceIdByHistoryId(historyId).orElse(null);
            if (deviceId == null) return;
            // Every competing path locks Device first, then ActiveHistory.
            Device device = devices.lockById(deviceId).orElse(null);
            ActiveHistory h = histories.lockById(historyId).orElse(null);
            if (device == null || h == null || !LOADING.equals(h.getStatus())) return;
            Long deadline = deadlines.get(commandId);
            if (ack != null) {
                if (deadline == null || !LedProtocol.deviceCode(device).equals(ack.path("deviceCode").asText())
                        || !h.getAction().equals(ack.path("status").asText())) return;
                if (System.nanoTime() >= deadline) {
                    log.warn("Late confirmation for command {}", commandId);
                } else if (ack.path("success").booleanValue()) {
                    h.setStatus(SUCCESS);
                    device.setCurrentStatus(h.getAction());
                    devices.save(device);
                    histories.save(h);
                    return;
                }
            }
            h.setStatus(FAILURE);
            histories.save(h);
            log.warn("Command {} failed: {}", commandId, failure);
        });
        // The expiry sweep retries database failures and removes terminal deadlines.
    }

    public Map<String, Object> result(Long historyId) {
        return tx.execute(s -> {
            ActiveHistory h = histories.findById(historyId).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lịch sử"));
            Device d = devices.findById(h.getDeviceId()).orElseThrow();
            return Map.<String, Object>of("historyId", h.getId(), "commandId", h.getCommandId() == null ? "" : h.getCommandId(),
                    "deviceId", h.getDeviceId(), "action", h.getAction(), "status", h.getStatus(),
                    "state", d.getCurrentStatus());
        });
    }

    @PreDestroy public void close() { timers.shutdownNow(); }
}
