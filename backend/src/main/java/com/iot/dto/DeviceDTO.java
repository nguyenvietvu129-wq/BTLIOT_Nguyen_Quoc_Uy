package com.iot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeviceDTO {
    private Long id;
    private String name;
    private String type;
    private String currentStatus;
}
