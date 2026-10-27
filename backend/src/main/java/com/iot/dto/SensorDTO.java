package com.iot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SensorDTO {
    private Long id;
    private String name;
    private String unit;
}
