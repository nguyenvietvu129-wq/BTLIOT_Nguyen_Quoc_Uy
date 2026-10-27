// backend/src/main/java/com/iot/repository/ActiveHistoryRepository.java
package com.iot.repository;

import com.iot.model.ActiveHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ActiveHistoryRepository extends JpaRepository<ActiveHistory, Long> {
    boolean existsByDeviceIdAndStatus(Long deviceId, String status);
    @Query("select h.deviceId from ActiveHistory h where h.id = :id")
    java.util.Optional<Long> findDeviceIdByHistoryId(@Param("id") Long id);
    java.util.List<ActiveHistory> findByStatus(String status);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from ActiveHistory h where h.id = :id")
    java.util.Optional<ActiveHistory> lockById(@Param("id") Long id);

    // Tìm kiếm kết hợp: deviceName, username, action, status, executedAt (string LIKE)
    @EntityGraph(attributePaths = {"device", "user"})
    @Query(value = "SELECT h FROM ActiveHistory h LEFT JOIN h.device d LEFT JOIN h.user u WHERE " +
           "(:deviceName IS NULL OR :deviceName = '' OR d.name LIKE CONCAT('%', :deviceName, '%')) AND " +
           "(:username IS NULL OR :username = '' OR u.username LIKE CONCAT('%', :username, '%')) AND " +
           "(:action IS NULL OR :action = '' OR h.action = :action) AND " +
           "(:status IS NULL OR :status = '' OR h.status = :status) AND " +
           "(:executedAt IS NULL OR :executedAt = '' OR CAST(h.executedAt AS string) LIKE CONCAT('%', :executedAt, '%'))",
           countQuery = "SELECT COUNT(h) FROM ActiveHistory h LEFT JOIN h.device d LEFT JOIN h.user u WHERE " +
           "(:deviceName IS NULL OR :deviceName = '' OR d.name LIKE CONCAT('%', :deviceName, '%')) AND " +
           "(:username IS NULL OR :username = '' OR u.username LIKE CONCAT('%', :username, '%')) AND " +
           "(:action IS NULL OR :action = '' OR h.action = :action) AND " +
           "(:status IS NULL OR :status = '' OR h.status = :status) AND " +
           "(:executedAt IS NULL OR :executedAt = '' OR CAST(h.executedAt AS string) LIKE CONCAT('%', :executedAt, '%'))")
    Page<ActiveHistory> searchHistory(
            @Param("deviceName") String deviceName,
            @Param("username") String username,
            @Param("action") String action,
            @Param("status") String status,
            @Param("executedAt") String executedAt,
            Pageable pageable);
}
