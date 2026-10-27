// backend/src/main/java/com/iot/repository/DataSensorRepository.java
package com.iot.repository;

import com.iot.model.DataSensor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DataSensorRepository extends JpaRepository<DataSensor, Long> {

    // Lấy bản ghi mới nhất của mỗi sensor (dùng cho dashboard)
    @EntityGraph(attributePaths = "sensor")
    @Query("SELECT d FROM DataSensor d WHERE d.measuredAt = " +
           "(SELECT MAX(d2.measuredAt) FROM DataSensor d2 WHERE d2.sensorsId = d.sensorsId) " +
           "ORDER BY d.sensorsId ASC")
    List<DataSensor> findLatestPerSensor();

    // Lấy 20 bản ghi mới nhất (dùng cho biểu đồ dashboard)
    @EntityGraph(attributePaths = "sensor")
    List<DataSensor> findTop20ByOrderByMeasuredAtDesc();

    // Tìm kiếm kết hợp nhiều điều kiện: sensorName, measuredAt (string LIKE), value
    @EntityGraph(attributePaths = "sensor")
    @Query(value = "SELECT d FROM DataSensor d JOIN d.sensor s WHERE " +
           "(:sensorName IS NULL OR :sensorName = '' OR s.name LIKE CONCAT('%', :sensorName, '%')) AND " +
           "(:measuredAt IS NULL OR :measuredAt = '' OR CAST(d.measuredAt AS string) LIKE CONCAT('%', :measuredAt, '%')) AND " +
           "(:value IS NULL OR :value = '' OR CAST(d.value AS string) LIKE CONCAT('%', :value, '%'))",
           countQuery = "SELECT COUNT(d) FROM DataSensor d JOIN d.sensor s WHERE " +
           "(:sensorName IS NULL OR :sensorName = '' OR s.name LIKE CONCAT('%', :sensorName, '%')) AND " +
           "(:measuredAt IS NULL OR :measuredAt = '' OR CAST(d.measuredAt AS string) LIKE CONCAT('%', :measuredAt, '%')) AND " +
           "(:value IS NULL OR :value = '' OR CAST(d.value AS string) LIKE CONCAT('%', :value, '%'))")
    Page<DataSensor> searchDataSensor(
            @Param("sensorName") String sensorName,
            @Param("measuredAt") String measuredAt,
            @Param("value") String value,
            Pageable pageable);
}
