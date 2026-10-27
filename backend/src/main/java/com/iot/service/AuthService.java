// backend/src/main/java/com/iot/service/AuthService.java
package com.iot.service;

import com.iot.dto.LoginRequest;
import com.iot.model.User;
import com.iot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Optional<User> authenticate(LoginRequest request) {
        if (request == null || request.getUsername() == null || request.getPassword() == null) {
            return Optional.empty();
        }

        Optional<User> userOpt = userRepository.findByUsername(request.getUsername().trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Kiểm tra mật khẩu khớp qua BCryptPasswordEncoder hoặc so sánh trực tiếp nếu mật khẩu thô
            if (passwordEncoder.matches(request.getPassword(), user.getPassword())
                    || request.getPassword().equals(user.getPassword())) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    public User createUserIfNotExist(String username, String rawPassword) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(rawPassword));
            return userRepository.save(user);
        });
    }
}
