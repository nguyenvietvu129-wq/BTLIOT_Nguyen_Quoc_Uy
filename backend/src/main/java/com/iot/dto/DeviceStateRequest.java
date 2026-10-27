// backend/src/main/java/com/iot/dto/DeviceStateRequest.java
package com.iot.dto;

import lombok.Data;

@Data
public class DeviceStateRequest {
    private String state; // ON hoặc OFF
}
