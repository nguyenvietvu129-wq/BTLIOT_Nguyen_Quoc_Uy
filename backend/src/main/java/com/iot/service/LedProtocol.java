package com.iot.service;

import com.iot.model.Device;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class LedProtocol {
    private LedProtocol() {}

    // Existing hardware mapping: yellow=LED1, red=LED2, green=LED3.
    public static String deviceCode(Device device) {
        String name = device.getName() == null ? "" : device.getName().toLowerCase(Locale.ROOT);
        if (name.contains("vàng") || name.contains("yellow")) return "LED1";
        if (name.contains("đỏ") || name.contains("red")) return "LED2";
        if (name.contains("xanh") || name.contains("green")) return "LED3";
        if (Long.valueOf(3).equals(device.getId())) return "LED1";
        if (Long.valueOf(2).equals(device.getId())) return "LED2";
        if (Long.valueOf(1).equals(device.getId())) return "LED3";
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiết bị chưa có ánh xạ LED");
    }
}
