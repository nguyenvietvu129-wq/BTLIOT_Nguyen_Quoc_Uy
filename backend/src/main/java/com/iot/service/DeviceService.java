// backend/src/main/java/com/iot/service/DeviceService.java
package com.iot.service;

import com.iot.dto.ActiveHistoryDTO;
import com.iot.dto.DeviceStateRequest;
import com.iot.dto.PaginationResponse;
import com.iot.model.ActiveHistory;
import com.iot.model.Device;
import com.iot.model.User;
import com.iot.repository.ActiveHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceService {
    private final ActiveHistoryRepository activeHistoryRepository;
    private final CommandService commandService;

    public java.util.Map<String, Object> controlDevice(Long deviceId, DeviceStateRequest request, Long userId) {
        return commandService.accept(deviceId, request == null ? null : request.getState(), userId);
    }

    /**
     * Tìm kiếm lịch sử điều khiển với phân trang.
     * Tham số deviceName, username, action, status, executedAt đều là String (LIKE search).
     */
    public PaginationResponse<ActiveHistoryDTO> getHistory(
            String deviceName, String username, String action, String status, String executedAt,
            int page, int limit, String sortKey, String sortDir) {

        Sort sort = Sort.by(parseSortDirection(sortDir), mapSortKey(sortKey));
        Pageable pageable = PageRequest.of(page - 1, limit, sort);

        String dn = (deviceName != null && !deviceName.trim().isEmpty()) ? deviceName.trim() : null;
        String un = (username != null && !username.trim().isEmpty()) ? username.trim() : null;
        String ac = (action != null && !action.trim().isEmpty()) ? action.trim() : null;
        String st = (status != null && !status.trim().isEmpty()) ? status.trim() : null;
        String ea = (executedAt != null && !executedAt.trim().isEmpty()) ? executedAt.trim() : null;

        Page<ActiveHistory> pageResult = activeHistoryRepository.searchHistory(dn, un, ac, st, ea, pageable);

        List<ActiveHistoryDTO> dtos = pageResult.getContent().stream()
                .map(this::mapToActiveHistoryDTO)
                .collect(Collectors.toList());

        return buildPaginationResponse(dtos, pageResult, page, limit);
    }

    private ActiveHistoryDTO mapToActiveHistoryDTO(ActiveHistory h) {
        ActiveHistoryDTO dto = new ActiveHistoryDTO();
        dto.setId(h.getId());
        dto.setDeviceId(h.getDeviceId());
        Device d = h.getDevice();
        dto.setDeviceName(d != null ? d.getName() : "Device #" + h.getDeviceId());
        dto.setUserId(h.getUserId());
        User u = h.getUser();
        dto.setUsername(u != null ? u.getUsername() : "User #" + h.getUserId());
        dto.setAction(h.getAction());
        dto.setStatus(h.getStatus());
        dto.setCommandId(h.getCommandId());
        dto.setExecutedAt(h.getExecutedAt());
        return dto;
    }

    private String mapSortKey(String sortKey) {
        if (sortKey == null) return "executedAt";
        return switch (sortKey) {
            case "deviceName" -> "device.name";
            case "username" -> "user.username";
            case "action" -> "action";
            case "status" -> "status";
            case "executedAt" -> "executedAt";
            default -> "executedAt";
        };
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
