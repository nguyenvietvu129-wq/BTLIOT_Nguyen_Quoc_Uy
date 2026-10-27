// backend/src/main/java/com/iot/service/SensorService.java
package com.iot.service;

import com.iot.dto.DashboardResponse;
import com.iot.dto.DataSensorDTO;
import com.iot.dto.DeviceDTO;
import com.iot.dto.PaginationResponse;
import com.iot.dto.SensorDTO;
import com.iot.dto.SensorIngestRequest;
import com.iot.model.DataSensor;
import com.iot.model.Device;
import com.iot.model.Sensor;
import com.iot.repository.DataSensorRepository;
import com.iot.repository.DeviceRepository;
import com.iot.repository.SensorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;
import java.util.ArrayList;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SensorService {
    private final SensorRepository sensorRepository;
    private final DataSensorRepository dataSensorRepository;
    private final DeviceRepository deviceRepository;

    public List<SensorDTO> getSensors() {
        return sensorRepository.findAll().stream()
                .map(s -> new SensorDTO(s.getId(), s.getName(), s.getUnit()))
                .collect(Collectors.toList());
    }

    public List<DeviceDTO> getDevices() {
        return deviceRepository.findAllByOrderByIdAsc().stream()
                .map(d -> new DeviceDTO(d.getId(), d.getName(), d.getType(), d.getCurrentStatus()))
                .collect(Collectors.toList());
    }

    /** Lưu một gói telemetry giống payload MQTT của ESP32. */
    @Transactional
    public List<DataSensorDTO> ingest(SensorIngestRequest request) {
        if (request == null || (request.getTemperature() == null
                && request.getHumidity() == null && request.getLight() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cần ít nhất một giá trị: temperature, humidity hoặc light");
        }

        LocalDateTime measuredAt = LocalDateTime.now();
        List<DataSensorDTO> saved = new ArrayList<>();
        saveReading(request.getTemperature(), "nhiệt", 1L, measuredAt, saved);
        saveReading(request.getHumidity(), "ẩm", 2L, measuredAt, saved);
        saveReading(request.getLight(), "sáng", 3L, measuredAt, saved);
        return saved;
    }

    private void saveReading(Float value, String sensorNameKeyword, Long fallbackId,
                             LocalDateTime measuredAt, List<DataSensorDTO> saved) {
        if (value == null) return;
        if (!Float.isFinite(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá trị cảm biến phải là số hữu hạn");
        }
        Sensor sensor = sensorRepository.findAll().stream()
                .filter(s -> s.getName() != null
                        && s.getName().toLowerCase().contains(sensorNameKeyword))
                .findFirst()
                .orElseGet(() -> sensorRepository.findById(fallbackId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Không tìm thấy cảm biến " + sensorNameKeyword)));
        DataSensor reading = new DataSensor();
        reading.setSensorsId(sensor.getId());
        reading.setValue(value);
        reading.setMeasuredAt(measuredAt);
        DataSensor stored = dataSensorRepository.save(reading);
        stored.setSensor(sensor);
        saved.add(mapToDataSensorDTO(stored));
    }

    /**
     * Lấy dữ liệu tổng hợp cho Dashboard.
     * - latestSensors: bản ghi mới nhất của từng cảm biến
     * - chartData: 20 bản ghi gần nhất để vẽ biểu đồ
     * - devices: danh sách thiết bị và trạng thái hiện tại
     */
    public DashboardResponse getDashboardSummary() {
        DashboardResponse response = new DashboardResponse();

        // Lấy bản ghi mới nhất theo từng sensor
        List<DataSensor> latestList = dataSensorRepository.findLatestPerSensor();
        List<DashboardResponse.LatestSensorData> latestDtos = latestList.stream().map(ds -> {
            DashboardResponse.LatestSensorData dto = new DashboardResponse.LatestSensorData();
            dto.setSensorId(ds.getSensorsId());
            Sensor s = ds.getSensor();
            dto.setSensorName(s != null ? s.getName() : "Sensor #" + ds.getSensorsId());
            dto.setUnit(s != null ? s.getUnit() : "");
            dto.setValue(ds.getValue());
            dto.setMeasuredAt(ds.getMeasuredAt());
            return dto;
        }).collect(Collectors.toList());
        response.setLatestSensors(latestDtos);

        // Lấy 20 bản ghi gần nhất cho biểu đồ
        List<DataSensor> recentList = dataSensorRepository.findTop20ByOrderByMeasuredAtDesc();
        List<DashboardResponse.ChartPoint> chartPoints = recentList.stream().map(ds -> {
            DashboardResponse.ChartPoint pt = new DashboardResponse.ChartPoint();
            Sensor s = ds.getSensor();
            pt.setSensorName(s != null ? s.getName() : "Sensor #" + ds.getSensorsId());
            pt.setValue(ds.getValue());
            pt.setMeasuredAt(ds.getMeasuredAt());
            return pt;
        }).collect(Collectors.toList());
        response.setChartData(chartPoints);

        // Lấy danh sách thiết bị
        List<Device> devices = deviceRepository.findAllByOrderByIdAsc();
        List<DashboardResponse.DeviceInfo> deviceInfos = devices.stream().map(d -> {
            DashboardResponse.DeviceInfo info = new DashboardResponse.DeviceInfo();
            info.setId(d.getId());
            info.setName(d.getName());
            info.setType(d.getType());
            info.setCurrentStatus(d.getCurrentStatus());
            return info;
        }).collect(Collectors.toList());
        response.setDevices(deviceInfos);

        return response;
    }

    /**
     * Tìm kiếm dữ liệu cảm biến với phân trang.
     * Tham số sensorName, measuredAt, value đều là String (LIKE search).
     * Nếu để trống → trả về tất cả.
     */
    public PaginationResponse<DataSensorDTO> getDataSensors(
            String sensorName, String measuredAt, String value,
            int page, int limit, String sortKey, String sortDir) {

        // Ánh xạ sortKey từ frontend sang field trong entity
        String mappedSortKey = mapSortKey(sortKey);
        Sort sort = Sort.by(parseSortDirection(sortDir), mappedSortKey);
        Pageable pageable = PageRequest.of(page - 1, limit, sort);

        String sn = (sensorName != null && !sensorName.trim().isEmpty()) ? sensorName.trim() : null;
        String ma = (measuredAt != null && !measuredAt.trim().isEmpty()) ? measuredAt.trim() : null;
        String v  = (value != null && !value.trim().isEmpty()) ? value.trim() : null;

        Page<DataSensor> pageResult = dataSensorRepository.searchDataSensor(sn, ma, v, pageable);

        List<DataSensorDTO> dtos = pageResult.getContent().stream()
                .map(this::mapToDataSensorDTO)
                .collect(Collectors.toList());

        return buildPaginationResponse(dtos, pageResult, page, limit);
    }

    private DataSensorDTO mapToDataSensorDTO(DataSensor ds) {
        DataSensorDTO dto = new DataSensorDTO();
        dto.setId(ds.getId());
        dto.setSensorsId(ds.getSensorsId());
        Sensor s = ds.getSensor();
        dto.setSensorName(s != null ? s.getName() : "Sensor #" + ds.getSensorsId());
        dto.setUnit(s != null ? s.getUnit() : "");
        dto.setValue(ds.getValue());
        dto.setMeasuredAt(ds.getMeasuredAt());
        return dto;
    }

    private String mapSortKey(String sortKey) {
        if (sortKey == null) return "measuredAt";
        switch (sortKey) {
            case "sensorName": return "sensor.name";
            case "measuredAt": return "measuredAt";
            case "value":      return "value";
            default:           return "measuredAt";
        }
    }

    private Sort.Direction parseSortDirection(String sortDir) {
        return "ASC".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    private <T> PaginationResponse<T> buildPaginationResponse(List<T> data, Page<?> page, int pageNum, int limit) {
        PaginationResponse<T> response = new PaginationResponse<>();
        response.setData(data);

        PaginationResponse.PaginationInfo pagination = new PaginationResponse.PaginationInfo();
        pagination.setPage(pageNum);
        pagination.setLimit(limit);
        pagination.setTotal(page.getTotalElements());
        pagination.setTotalPages(page.getTotalPages());
        response.setPagination(pagination);

        return response;
    }
}
