// backend/src/main/java/com/iot/model/DataSensor.java
package com.iot.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@Entity
@Table(name = "datasensor")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DataSensor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "INT")
    private Long id;

    @Column(name = "sensors_id", nullable = false, columnDefinition = "INT")
    private Long sensorsId;

    @Column(name = "value", nullable = false)
    private Float value;

    @Column(name = "measured_at", nullable = false)
    private LocalDateTime measuredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "sensors_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_datasensor_sensor"))
    private Sensor sensor;
}
