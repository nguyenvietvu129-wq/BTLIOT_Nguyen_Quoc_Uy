package com.iot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.model.ActiveHistory;
import com.iot.model.Device;
import com.iot.repository.ActiveHistoryRepository;
import com.iot.repository.DeviceRepository;
import org.junit.jupiter.api.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class CommandServiceTest {
    ActiveHistoryRepository histories;
    DeviceRepository devices;
    ApplicationEventPublisher events;
    CommandService service;
    Device device;
    ActiveHistory current;
    long nextHistoryId = 10L;
    final ObjectMapper json = new ObjectMapper();

    static class Transactions extends AbstractPlatformTransactionManager {
        protected Object doGetTransaction() { return new Object(); }
        protected void doBegin(Object t, org.springframework.transaction.TransactionDefinition d) {}
        protected void doCommit(DefaultTransactionStatus s) {}
        protected void doRollback(DefaultTransactionStatus s) {}
    }

    @BeforeEach void setup() {
        histories = mock(ActiveHistoryRepository.class);
        devices = mock(DeviceRepository.class);
        events = mock(ApplicationEventPublisher.class);
        service = new CommandService(histories, devices, events, new Transactions(), 5000);
        device = new Device(); device.setId(1L); device.setName("LED xanh"); device.setCurrentStatus("OFF");
        when(devices.lockById(1L)).thenReturn(Optional.of(device));
        when(histories.saveAndFlush(any())).thenAnswer(i -> {
            current = i.getArgument(0); current.setId(nextHistoryId++); return current;
        });
        when(histories.findDeviceIdByHistoryId(anyLong())).thenAnswer(i ->
                current != null && current.getId().equals(i.getArgument(0)) ? Optional.of(1L) : Optional.empty());
        when(histories.lockById(anyLong())).thenAnswer(i -> Optional.ofNullable(current));
    }
    @AfterEach void cleanup() { service.close(); }
    String accept(String action) {
        return (String) service.accept(1L, action, 2L).get("commandId");
    }
    void ack(String id, String code, String state, boolean success) throws Exception {
        service.acknowledge(json.readTree(json.writeValueAsString(Map.of(
                "commandId", id, "deviceCode", code, "status", state, "success", success))));
    }

    @Test void savesLoadingBeforeDispatchAndDoesNotChangeDevice() {
        doAnswer(i -> { assertEquals("LOADING", current.getStatus());
            verify(histories).saveAndFlush(current); return null;
        }).when(events).publishEvent(any(Object.class));
        var response = service.accept(1L, "ON", 2L);
        assertEquals("LOADING", response.get("status"));
        assertEquals(10L, response.get("historyId"));
        assertNotNull(response.get("commandId"));
        assertEquals("OFF", device.getCurrentStatus());
        verify(events).publishEvent(any(CommandService.SendCommand.class));
    }
    @Test void onSuccessAndDuplicateCannotChangeTerminalResult() throws Exception {
        String id = accept("ON"); service.sending(id); ack(id, "LED3", "ON", true);
        assertEquals("SUCCESS", current.getStatus()); assertEquals("ON", device.getCurrentStatus());
        ack(id, "LED3", "ON", false); service.fail(id, "timeout");
        assertEquals("SUCCESS", current.getStatus());
        verify(devices, times(1)).save(device);
    }
    @Test void offSuccess() throws Exception {
        device.setCurrentStatus("ON"); String id = accept("OFF"); service.sending(id);
        ack(id, "LED3", "OFF", true);
        assertEquals("SUCCESS", current.getStatus()); assertEquals("OFF", device.getCurrentStatus());
    }
    @Test void failedPublishKeepsConfirmedState() {
        String id = accept("ON"); service.sending(id); service.fail(id, "Broker disconnected");
        assertEquals("FAILURE", current.getStatus()); assertEquals("OFF", device.getCurrentStatus());
    }
    @Test void espFailureKeepsConfirmedState() throws Exception {
        String id = accept("ON"); service.sending(id); ack(id, "LED3", "ON", false);
        assertEquals("FAILURE", current.getStatus()); assertEquals("OFF", device.getCurrentStatus());
    }
    @Test void mismatchedAndUncorrelatedMessagesAreIgnored() throws Exception {
        String id = accept("ON"); service.sending(id);
        ack("unknown", "LED3", "ON", true); ack(id, "LED1", "ON", true); ack(id, "LED3", "OFF", true);
        service.acknowledge(json.readTree("{\"led3\":\"ON\"}"));
        service.acknowledge(json.readTree("{\"commandId\":\"x\",\"success\":\"true\"}"));
        assertEquals("LOADING", current.getStatus()); assertEquals("OFF", device.getCurrentStatus());
    }
    @Test void timeoutAndLateAckCannotOverwriteFailure() throws Exception {
        service.close(); service = new CommandService(histories, devices, events, new Transactions(), 20);
        when(histories.findByStatus("LOADING")).thenReturn(List.of()); service.recover();
        CountDownLatch failed = new CountDownLatch(1);
        when(histories.save(any())).thenAnswer(i -> {
            ActiveHistory h = i.getArgument(0);
            if ("FAILURE".equals(h.getStatus())) failed.countDown();
            return h;
        });
        String id = accept("ON"); service.sending(id);
        assertTrue(failed.await(2, TimeUnit.SECONDS));
        ack(id, "LED3", "ON", true);
        assertEquals("FAILURE", current.getStatus()); assertEquals("OFF", device.getCurrentStatus());
    }
    @Test void invalidMissingAndBusyRequestsDoNotCreateHistory() {
        assertThrows(ResponseStatusException.class, () -> accept(null));
        when(devices.lockById(1L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> accept("ON"));
        when(devices.lockById(1L)).thenReturn(Optional.of(device));
        when(histories.existsByDeviceIdAndStatus(1L, "LOADING")).thenReturn(true);
        assertThrows(ResponseStatusException.class, () -> accept("OFF"));
        verify(histories, never()).saveAndFlush(any());
    }
    @Test void restartFailsPendingWithoutConvertingLegacyValues() {
        ActiveHistory pending = new ActiveHistory(); pending.setStatus("LOADING");
        ActiveHistory legacy = new ActiveHistory(); legacy.setStatus("ON");
        when(histories.findByStatus("LOADING")).thenReturn(List.of(pending));
        service.recover();
        assertEquals("FAILURE", pending.getStatus()); assertEquals("ON", legacy.getStatus());
        verify(devices, never()).save(any());
    }
    @Test void eachAcceptedRequestGetsSeparateHistoryAndCommandId() {
        String first = accept("ON"); String second = accept("OFF");
        assertNotEquals(first, second); verify(histories, times(2)).saveAndFlush(any());
    }

    @Test void legacyTelemetryUpdatesOnlyObservedStateNotCommandOutcome() throws Exception {
        accept("ON");
        when(devices.findDeviceIds()).thenReturn(List.of(1L));
        service.synchronizeLegacyState(json.readTree("{\"led3\":\"ON\"}"));
        assertEquals("ON", device.getCurrentStatus());
        assertEquals("LOADING", current.getStatus());
        service.synchronizeLegacyState(json.readTree("{\"led3\":\"INVALID\"}"));
        assertEquals("ON", device.getCurrentStatus());
        verify(histories, never()).save(any());
    }

    @Test void legacyOnConfirmsLoadingAndDuplicateOrTimeoutCannotOverwriteSuccess() throws Exception {
        String id = accept("ON");
        when(devices.findDeviceIds()).thenReturn(List.of(1L));
        service.sendingLegacy(new CommandService.SendCommand(id, "LED3", "ON"));
        service.synchronizeLegacyState(json.readTree("{\"led1\":\"OFF\",\"led2\":\"OFF\",\"led3\":\"ON\"}"));
        assertEquals("SUCCESS", current.getStatus());
        assertEquals("ON", device.getCurrentStatus());
        service.synchronizeLegacyState(json.readTree("{\"led3\":\"ON\"}"));
        service.fail(id, "timeout");
        assertEquals("SUCCESS", current.getStatus());
        verify(histories, times(1)).save(current);
    }

    @Test void legacyOffConfirmsSuccess() throws Exception {
        device.setCurrentStatus("ON");
        String id = accept("OFF");
        when(devices.findDeviceIds()).thenReturn(List.of(1L));
        service.sendingLegacy(new CommandService.SendCommand(id, "LED3", "OFF"));
        service.synchronizeLegacyState(json.readTree("{\"led3\":\"OFF\"}"));
        assertEquals("SUCCESS", current.getStatus());
        assertEquals("OFF", device.getCurrentStatus());
    }

    @Test void legacyWrongLedWrongStateAndInvalidValueCannotConfirm() throws Exception {
        String id = accept("ON");
        when(devices.findDeviceIds()).thenReturn(List.of(1L));
        service.sendingLegacy(new CommandService.SendCommand(id, "LED3", "ON"));
        for (String packet : List.of("{\"led1\":\"ON\"}", "{\"led3\":\"OFF\"}",
                "{\"led3\":true}", "{\"led3\":\"UNKNOWN\"}")) {
            service.synchronizeLegacyState(json.readTree(packet));
            assertEquals("LOADING", current.getStatus());
        }
        verify(histories, never()).save(any());
    }

    @Test void legacyStatusCannotConfirmJsonCommand() throws Exception {
        String id = accept("ON");
        when(devices.findDeviceIds()).thenReturn(List.of(1L));
        service.sending(id);
        service.synchronizeLegacyState(json.readTree("{\"led3\":\"ON\"}"));
        assertEquals("LOADING", current.getStatus());
    }

    @Test void legacyConfirmationAfterTimeoutCannotOverwriteFailure() throws Exception {
        service.close(); service = new CommandService(histories, devices, events, new Transactions(), 20);
        when(histories.findByStatus("LOADING")).thenReturn(List.of());
        when(devices.findDeviceIds()).thenReturn(List.of(1L));
        service.recover();
        CountDownLatch failed = new CountDownLatch(1);
        when(histories.save(any())).thenAnswer(i -> {
            ActiveHistory h = i.getArgument(0);
            if ("FAILURE".equals(h.getStatus())) failed.countDown();
            return h;
        });
        String id = accept("ON");
        service.sendingLegacy(new CommandService.SendCommand(id, "LED3", "ON"));
        assertTrue(failed.await(2, TimeUnit.SECONDS));
        service.synchronizeLegacyState(json.readTree("{\"led3\":\"ON\"}"));
        assertEquals("FAILURE", current.getStatus());
    }
}
