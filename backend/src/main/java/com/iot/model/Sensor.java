// backend/src/main/java/com/iot/model/Sensor.java
package com.iot.model;

import jakarta.persistence.*;
import lombok.Data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@Entity
@Table(name = "sensor")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Sensor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "INT")
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    // Cột unit: nếu database thực tế có cột này hãy giữ, nếu không có thì comment lại
    @Column(name = "unit", length = 20)
    private String unit;
}
