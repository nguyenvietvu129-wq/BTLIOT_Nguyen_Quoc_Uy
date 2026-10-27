// backend/src/main/java/com/iot/repository/SensorRepository.java
package com.iot.repository;

import com.iot.model.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, Long> {
    // Không còn findBySensorCode vì schema mới không có cột sensor_code
}
