// backend/src/main/java/com/iot/dto/DataSensorDTO.java
package com.iot.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DataSensorDTO {
    private Long id;
    private Long sensorsId;
    private String sensorName;
    private String unit;
    private Float value;
    private LocalDateTime measuredAt;
}
