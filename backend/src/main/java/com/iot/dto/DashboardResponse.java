// backend/src/main/java/com/iot/dto/DashboardResponse.java
package com.iot.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DashboardResponse {
    private List<LatestSensorData> latestSensors;
    private List<ChartPoint> chartData;
    private List<DeviceInfo> devices;

    @Data
    public static class LatestSensorData {
        private Long sensorId;
        private String sensorName;
        private String unit;
        private Float value;
        private LocalDateTime measuredAt;
    }

    @Data
    public static class ChartPoint {
        private String sensorName;
        private Float value;
        private LocalDateTime measuredAt;
    }

    @Data
    public static class DeviceInfo {
        private Long id;
        private String name;
        private String type;
        private String currentStatus;
    }
}
