package com.iot.service;

import com.iot.controller.DeviceController;
import com.iot.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DeviceCommandApiTest {
    @Test void returnsAcceptedWithCommandIdentityAndPollsDatabaseResult() throws Exception {
        DeviceService devices = mock(DeviceService.class);
        CommandService commands = mock(CommandService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new DeviceController(devices, commands)).build();
        User user = new User(); user.setId(2L);
        MockHttpSession session = new MockHttpSession(); session.setAttribute("user", user);
        when(devices.controlDevice(eq(1L), any(), eq(2L))).thenReturn(Map.of(
                "historyId", 10L, "commandId", "unique-command", "action", "ON", "status", "LOADING"));
        mvc.perform(post("/api/devices/1/state").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"state\":\"ON\"}"))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.historyId").value(10))
                .andExpect(jsonPath("$.commandId").value("unique-command"))
                .andExpect(jsonPath("$.status").value("LOADING"));
        when(commands.result(10L)).thenReturn(Map.of("status", "SUCCESS", "state", "ON"));
        mvc.perform(get("/api/activehistories/10")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS")).andExpect(jsonPath("$.state").value("ON"));
    }
}
