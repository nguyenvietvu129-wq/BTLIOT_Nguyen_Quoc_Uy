// backend/src/main/java/com/iot/controller/DeviceController.java
package com.iot.controller;

import com.iot.dto.ActiveHistoryDTO;
import com.iot.dto.DeviceStateRequest;
import com.iot.dto.PaginationResponse;
import com.iot.model.User;
import com.iot.service.DeviceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceService deviceService;
    private final com.iot.service.CommandService commandService;

    @GetMapping("/activehistories/{historyId}")
    public ResponseEntity<Map<String, Object>> commandResult(@PathVariable Long historyId) {
        return ResponseEntity.ok(commandService.result(historyId));
    }

    /**
     * POST /api/devices/{deviceId}/state
     * Điều khiển thiết bị ON/OFF.
     * Yêu cầu đăng nhập (session user).
     */
    @PostMapping("/devices/{deviceId}/state")
    public ResponseEntity<Map<String, Object>> controlDevice(
            @PathVariable Long deviceId,
            @RequestBody DeviceStateRequest request,
            HttpServletRequest httpRequest) {
        try {
            // Lấy thông tin user từ session
            HttpSession session = httpRequest.getSession(false);
            User sessionUser = (session != null) ? (User) session.getAttribute("user") : null;
            if (sessionUser == null) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "Chưa đăng nhập! Vui lòng đăng nhập trước."));
            }
            Long userId = sessionUser.getId();

            return ResponseEntity.accepted().body(deviceService.controlDevice(deviceId, request, userId));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason()));
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * GET /api/activehistories
     * Lấy toàn bộ lịch sử điều khiển (có phân trang).
     */
    @GetMapping("/activehistories")
    public ResponseEntity<PaginationResponse<ActiveHistoryDTO>> getHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "executedAt") String sortKey,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        return ResponseEntity.ok(
                deviceService.getHistory(null, null, null, null, null, page, limit, sortKey, sortDir));
    }

    /**
     * GET /api/activehistories/search
     * Tìm kiếm lịch sử theo deviceName, username, action, status, executedAt.
     * Nếu để trống tất cả → trả về toàn bộ lịch sử.
     *
     * Ví dụ:
     * /api/activehistories/search?deviceName=LED xanh&action=ON&executedAt=2026-09-08
     */
    @GetMapping("/activehistories/search")
    public ResponseEntity<PaginationResponse<ActiveHistoryDTO>> searchHistory(
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String executedAt,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "executedAt") String sortKey,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        return ResponseEntity.ok(
                deviceService.getHistory(deviceName, username, action, status, executedAt, page, limit, sortKey, sortDir));
    }
}
