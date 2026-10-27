// backend/src/main/java/com/iot/model/ActiveHistory.java
package com.iot.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@Entity
// Bảng lịch sử hiện có trong MySQL là `activehistory` (không có dấu gạch dưới).
@Table(name = "activehistory")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ActiveHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "INT")
    private Long id;

    @Column(name = "device_id", nullable = false, columnDefinition = "INT")
    private Long deviceId;

    @Column(name = "user_id", nullable = false, columnDefinition = "INT")
    private Long userId;

    @Column(name = "action", nullable = false, length = 255)
    private String action;

    @Column(name = "status", length = 50)
    private String status;

    // Derived from the existing primary key; no extra database column.
    @Transient
    public String getCommandId() {
        return id == null ? null : "CMD-" + id;
    }

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "device_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_activehistory_device"))
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_activehistory_user"))
    private User user;
}
