// backend/src/main/java/com/iot/service/MqttService.java
package com.iot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.model.DataSensor;
import com.iot.model.Device;
import com.iot.model.Sensor;
import com.iot.repository.DataSensorRepository;
import com.iot.repository.DeviceRepository;
import com.iot.repository.SensorRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttService implements MqttCallbackExtended {

    private final DataSensorRepository dataSensorRepository;
    private final SensorRepository sensorRepository;
    private final DeviceRepository deviceRepository;
    private final ObjectMapper objectMapper;
    private final CommandService commandService;
    private final java.util.concurrent.ExecutorService commandExecutor = Executors.newFixedThreadPool(3);

    @Value("${mqtt.broker.url:tcp://172.20.10.2:1900}")
    private String brokerUrl;

    @Value("${mqtt.client.id:SpringBoot-Backend-Server}")
    private String clientId;

    @Value("${mqtt.username:b23cn96}")
    private String username;

    @Value("${mqtt.password:12345678}")
    private String password;

    @Value("${mqtt.topic.sensor-data:iot/sensors/data}")
    private String sensorDataTopic;

    @Value("${mqtt.topic.device-command:iot/devices/command}")
    private String deviceCommandTopic;

    @Value("${mqtt.topic.device-status:iot/devices/status}")
    private String deviceStatusTopic;

    @Value("${mqtt.command.timeout-ms:5000}")
    private long commandTimeoutMs;

    @Value("${mqtt.command.format:json}")
    private String commandFormat;

    private volatile MqttClient mqttClient;
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void init() {
        if (!"json".equalsIgnoreCase(commandFormat) && !"text".equalsIgnoreCase(commandFormat))
            throw new IllegalArgumentException("mqtt.command.format must be json or text");
        // Khởi động kết nối trong thread riêng để không chặn ứng dụng nếu broker chưa bật
        reconnectExecutor.schedule(this::connectMqtt, 1, TimeUnit.SECONDS);
    }

    private synchronized void connectMqtt() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                return;
            }

            log.info("Dang ket noi den MQTT Broker: {}", brokerUrl);
            mqttClient = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
            mqttClient.setTimeToWait(commandTimeoutMs);
            mqttClient.setCallback(this);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(5);
            options.setKeepAliveInterval(30);

            if (username != null && !username.isBlank()) {
                options.setUserName(username);
            }
            if (password != null && !password.isBlank()) {
                options.setPassword(password.toCharArray());
            }

            mqttClient.connect(options);
            log.info("Ket noi MQTT Broker thanh cong!");
        } catch (MqttException e) {
            log.warn("Chua the ket noi MQTT Broker ({}): {}. Se thu lai sau 10 giay...", brokerUrl, e.getMessage());
            // Lên lịch thử lại
            reconnectExecutor.schedule(this::connectMqtt, 10, TimeUnit.SECONDS);
        }
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        log.info("MQTT Connect Complete: server={}, reconnect={}", serverURI, reconnect);
        subscribeTopics();
    }

    private void subscribeTopics() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.subscribe(sensorDataTopic, 1);
                log.info("Da subscribe topic sensor: {}", sensorDataTopic);

                mqttClient.subscribe(deviceStatusTopic, 1);
                log.info("Da subscribe topic status: {}", deviceStatusTopic);
            }
        } catch (MqttException e) {
            log.error("Loi khi subscribe cac topic: {}", e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("Mat ket noi MQTT Broker: {}", cause != null ? cause.getMessage() : "Unknown");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        log.info("MQTT Received [{}]: {}", topic, payload);

        try {
            if (topic.equals(sensorDataTopic)) {
                handleSensorData(payload);
            } else if (topic.equals(deviceStatusTopic)) {
                if (!message.isRetained()) handleDeviceStatus(payload);
            }
        } catch (Exception e) {
            log.error("Loi khi xu ly tin nhan MQTT [{}]: {}", topic, e.getMessage(), e);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Message delivery completed
    }

    /**
     * Xu ly du lieu sensor gui tu ESP32:
     * Payload mau: {"temperature":28.5,"humidity":65.2,"light":120}
     */
    @Transactional
    public void handleSensorData(String payload) {
        try {
            if (payload == null || payload.isBlank()) return;
            String clean = payload.trim();
            if (clean.contains("\\\"")) {
                clean = clean.replace("\\\"", "\"");
            }
            if (clean.startsWith("\"") && clean.endsWith("\"")) {
                clean = clean.substring(1, clean.length() - 1);
            }

            JsonNode root = objectMapper.readTree(clean);
            LocalDateTime now = LocalDateTime.now();

            // Nhiệt độ
            if (root.has("temperature") && !root.get("temperature").isNull()) {
                float temp = (float) root.get("temperature").asDouble();
                saveSensorValue("nhiệt", 1L, temp, now);
            }

            // Độ ẩm
            if (root.has("humidity") && !root.get("humidity").isNull()) {
                float hum = (float) root.get("humidity").asDouble();
                saveSensorValue("ẩm", 2L, hum, now);
            }

            // Ánh sáng
            if (root.has("light") && !root.get("light").isNull()) {
                float light = (float) root.get("light").asDouble();
                saveSensorValue("sáng", 3L, light, now);
            }
        } catch (Exception e) {
            log.error("Loi parse JSON sensor data: {}", e.getMessage());
        }
    }

    private void saveSensorValue(String keyword, Long defaultId, float value, LocalDateTime measuredAt) {
        Long sensorId = defaultId;
        List<Sensor> sensors = sensorRepository.findAll();
        for (Sensor s : sensors) {
            if (s.getName() != null && s.getName().toLowerCase().contains(keyword)) {
                sensorId = s.getId();
                break;
            }
        }

        DataSensor ds = new DataSensor();
        ds.setSensorsId(sensorId);
        ds.setValue(value);
        ds.setMeasuredAt(measuredAt);
        dataSensorRepository.save(ds);
        log.debug("Da luu DataSensor: sensorId={}, value={}", sensorId, value);
    }

    // Text firmware confirms the pending command by matching its reported LED state.
    public void handleDeviceStatus(String payload) throws Exception {
        JsonNode status = objectMapper.readTree(payload);
        if (status.has("commandId")) {
            commandService.acknowledge(status);
        } else if ("text".equalsIgnoreCase(commandFormat)) {
            commandService.synchronizeLegacyState(status);
        } else {
            log.warn("Device status has no commandId. Update ESP32 firmware for JSON command acknowledgements, "
                    + "or use mqtt.command.format=text for the original LED1 ON protocol");
        }
    }

    String encodeCommand(CommandService.SendCommand command) throws Exception {
        if ("text".equalsIgnoreCase(commandFormat))
            return command.deviceCode() + " " + command.action();
        return objectMapper.writeValueAsString(java.util.Map.of(
                "commandId", command.commandId(), "deviceCode", command.deviceCode(), "action", command.action()));
    }

    @org.springframework.context.event.EventListener
    public void dispatchCommand(CommandService.SendCommand command) {
        commandExecutor.execute(() -> {
            try {
                MqttClient client = mqttClient;
                if (client == null || !client.isConnected())
                    throw new IllegalStateException("MQTT Broker chưa kết nối");
                String payload = encodeCommand(command);
                MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
                message.setQos(1);
                message.setRetained(false);
                if ("text".equalsIgnoreCase(commandFormat)) commandService.sendingLegacy(command);
                else commandService.sending(command.commandId());
                client.publish(deviceCommandTopic, message);
                log.info("MQTT command published: commandId={}, topic={}, format={}, payload={}; awaiting ESP32 acknowledgement",
                        command.commandId(), deviceCommandTopic, commandFormat, payload);
                // Publish completion is never a GPIO confirmation.
            } catch (Exception e) {
                commandService.fail(command.commandId(), "MQTT publish: " + e.getMessage());
            }
        });
    }

    @PreDestroy
    public void cleanup() {
        try {
            reconnectExecutor.shutdownNow();
            commandExecutor.shutdownNow();
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                mqttClient.close();
                log.info("Da ngat ket noi MQTT client.");
            }
        } catch (Exception e) {
            log.error("Loi khi dong MQTT client: {}", e.getMessage());
        }
    }
}
