// backend/src/main/java/com/iot/dto/LoginRequest.java
package com.iot.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}
