package com.iot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.repository.*;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.charset.StandardCharsets;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class MqttCommandTest {
    @Test void publishesExactOriginalTextPayloadForExistingFirmware() throws Exception {
        CommandService commands = mock(CommandService.class);
        MqttService mqtt = new MqttService(mock(DataSensorRepository.class), mock(SensorRepository.class),
                mock(DeviceRepository.class), new ObjectMapper(), commands);
        MqttClient client = mock(MqttClient.class);
        when(client.isConnected()).thenReturn(true);
        ReflectionTestUtils.setField(mqtt, "mqttClient", client);
        ReflectionTestUtils.setField(mqtt, "commandFormat", "text");
        ReflectionTestUtils.setField(mqtt, "deviceCommandTopic", "iot/devices/command");
        try {
            mqtt.dispatchCommand(new CommandService.SendCommand("CMD-36", "LED3", "ON"));
            var message = org.mockito.ArgumentCaptor.forClass(MqttMessage.class);
            verify(client, timeout(2000)).publish(eq("iot/devices/command"), message.capture());
            assertEquals("LED3 ON", new String(message.getValue().getPayload(), StandardCharsets.UTF_8));
            assertEquals(1, message.getValue().getQos());
            assertFalse(message.getValue().isRetained());
            verify(commands).sendingLegacy(new CommandService.SendCommand("CMD-36", "LED3", "ON"));
            verify(commands, never()).acknowledge(any());
        } finally { mqtt.cleanup(); }
    }

    @Test void jsonModePreservesCommandIdAndTextModeRoutesLegacyConfirmation() throws Exception {
        CommandService commands = mock(CommandService.class);
        ObjectMapper json = new ObjectMapper();
        MqttService mqtt = new MqttService(mock(DataSensorRepository.class), mock(SensorRepository.class),
                mock(DeviceRepository.class), json, commands);
        try {
            ReflectionTestUtils.setField(mqtt, "commandFormat", "json");
            var payload = json.readTree(mqtt.encodeCommand(new CommandService.SendCommand("CMD-37", "LED2", "OFF")));
            assertEquals("CMD-37", payload.path("commandId").asText());
            assertEquals("LED2", payload.path("deviceCode").asText());
            assertEquals("OFF", payload.path("action").asText());
            ReflectionTestUtils.setField(mqtt, "commandFormat", "text");
            mqtt.handleDeviceStatus("{\"led1\":\"ON\",\"led2\":\"OFF\",\"led3\":\"ON\"}");
            verify(commands).synchronizeLegacyState(any());
            verify(commands, never()).acknowledge(any());
        } finally { mqtt.cleanup(); }
    }

    @Test void retainedAckCannotConfirmCommandButLiveAckCan() {
        CommandService commands = mock(CommandService.class);
        MqttService mqtt = new MqttService(mock(DataSensorRepository.class), mock(SensorRepository.class),
                mock(DeviceRepository.class), new ObjectMapper(), commands);
        ReflectionTestUtils.setField(mqtt, "sensorDataTopic", "iot/sensors/data");
        ReflectionTestUtils.setField(mqtt, "deviceStatusTopic", "iot/devices/status");
        String payload = "{\"commandId\":\"test\",\"deviceCode\":\"LED3\",\"status\":\"ON\",\"success\":true}";
        MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
        message.setRetained(true);
        mqtt.messageArrived("iot/devices/status", message);
        verifyNoInteractions(commands);
        message.setRetained(false);
        mqtt.messageArrived("iot/devices/status", message);
        verify(commands).acknowledge(any());
        mqtt.cleanup();
    }

    @Test void retainedLegacyStatusCannotConfirmTextCommand() {
        CommandService commands = mock(CommandService.class);
        MqttService mqtt = new MqttService(mock(DataSensorRepository.class), mock(SensorRepository.class),
                mock(DeviceRepository.class), new ObjectMapper(), commands);
        ReflectionTestUtils.setField(mqtt, "sensorDataTopic", "iot/sensors/data");
        ReflectionTestUtils.setField(mqtt, "deviceStatusTopic", "iot/devices/status");
        ReflectionTestUtils.setField(mqtt, "commandFormat", "text");
        MqttMessage message = new MqttMessage("{\"led3\":\"ON\"}".getBytes(StandardCharsets.UTF_8));
        message.setRetained(true);
        mqtt.messageArrived("iot/devices/status", message);
        verifyNoInteractions(commands);
        message.setRetained(false);
        mqtt.messageArrived("iot/devices/status", message);
        verify(commands).synchronizeLegacyState(any());
        mqtt.cleanup();
    }
}
