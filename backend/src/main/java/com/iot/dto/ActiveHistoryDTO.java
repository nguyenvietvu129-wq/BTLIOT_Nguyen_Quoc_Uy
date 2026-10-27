// backend/src/main/java/com/iot/dto/ActiveHistoryDTO.java
package com.iot.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ActiveHistoryDTO {
    private Long id;
    private Long deviceId;
    private String deviceName;
    private Long userId;
    private String username;
    private String action;
    private String status;
    private String commandId;
    private LocalDateTime executedAt;
}
