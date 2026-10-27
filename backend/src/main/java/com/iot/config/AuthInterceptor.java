// backend/src/main/java/com/iot/config/AuthInterceptor.java
package com.iot.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Cho phép các yêu cầu OPTIONS (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();
        // Cho phép không cần đăng nhập đối với: /login, /logout, và API nhận dữ liệu từ cảm biến /ingest/
        if (uri.endsWith("/login") || uri.endsWith("/logout") || uri.contains("/ingest/")) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            return true;
        }

        // Nếu chưa đăng nhập, trả về HTTP 401 Unauthorized đối với API
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"Chưa đăng nhập! Vui lòng đăng nhập trước.\"}");
        return false;
    }
}
