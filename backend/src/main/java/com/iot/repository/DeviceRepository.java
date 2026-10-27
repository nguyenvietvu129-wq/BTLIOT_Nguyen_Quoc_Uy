// backend/src/main/java/com/iot/repository/DeviceRepository.java
package com.iot.repository;

import com.iot.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    @org.springframework.data.jpa.repository.Query("select d.id from Device d")
    List<Long> findDeviceIds();
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select d from Device d where d.id = :id")
    java.util.Optional<Device> lockById(@org.springframework.data.repository.query.Param("id") Long id);
    List<Device> findAllByOrderByIdAsc();
}
