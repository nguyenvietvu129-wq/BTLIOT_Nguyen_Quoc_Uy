package com.iot.dto;

import lombok.Data;

@Data
public class SensorIngestRequest {
    private Float temperature;
    private Float humidity;
    private Float light;
}
