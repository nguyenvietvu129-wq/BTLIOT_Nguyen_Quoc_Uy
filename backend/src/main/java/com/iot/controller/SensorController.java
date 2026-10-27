// backend/src/main/java/com/iot/controller/SensorController.java
package com.iot.controller;

import com.iot.dto.DashboardResponse;
import com.iot.dto.DataSensorDTO;
import com.iot.dto.PaginationResponse;
import com.iot.dto.SensorDTO;
import com.iot.dto.DeviceDTO;
import com.iot.dto.SensorIngestRequest;
import com.iot.service.SensorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SensorController {
    private final SensorService sensorService;

    @GetMapping("/sensors")
    public ResponseEntity<java.util.List<SensorDTO>> getSensors() {
        return ResponseEntity.ok(sensorService.getSensors());
    }

    @GetMapping("/devices")
    public ResponseEntity<java.util.List<DeviceDTO>> getDevices() {
        return ResponseEntity.ok(sensorService.getDevices());
    }

    /** HTTP endpoint để test payload ESP32 mà không cần MQTT. */
    @PostMapping("/datasensors/ingest")
    public ResponseEntity<java.util.Map<String, Object>> ingest(@RequestBody SensorIngestRequest request) {
        java.util.List<DataSensorDTO> data = sensorService.ingest(request);
        return ResponseEntity.status(201).body(java.util.Map.of(
                "success", true,
                "message", "Đã lưu dữ liệu cảm biến",
                "data", data));
    }

    /**
     * GET /api/dashboard/summary
     * Lấy dữ liệu tổng hợp cho trang Dashboard.
     */
    @GetMapping("/dashboard/summary")
    public ResponseEntity<DashboardResponse> getDashboardSummary() {
        return ResponseEntity.ok(sensorService.getDashboardSummary());
    }

    /**
     * GET /api/datasensors
     * Lấy toàn bộ dữ liệu cảm biến (có phân trang, sắp xếp).
     */
    @GetMapping("/datasensors")
    public ResponseEntity<PaginationResponse<DataSensorDTO>> getDataSensors(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "measuredAt") String sortKey,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        return ResponseEntity.ok(
                sensorService.getDataSensors(null, null, null, page, limit, sortKey, sortDir));
    }

    /**
     * GET /api/datasensors/search
     * Tìm kiếm dữ liệu cảm biến theo sensorName, measuredAt, value.
     * Nếu để trống tất cả → trả về toàn bộ dữ liệu.
     *
     * Ví dụ:
     * /api/datasensors/search?sensorName=nhiệt độ&measuredAt=2026-09-08&value=25.5
     */
    @GetMapping("/datasensors/search")
    public ResponseEntity<PaginationResponse<DataSensorDTO>> searchDataSensors(
            @RequestParam(required = false) String sensorName,
            @RequestParam(required = false) String measuredAt,
            @RequestParam(required = false) String value,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "measuredAt") String sortKey,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        return ResponseEntity.ok(
                sensorService.getDataSensors(sensorName, measuredAt, value, page, limit, sortKey, sortDir));
    }
}
