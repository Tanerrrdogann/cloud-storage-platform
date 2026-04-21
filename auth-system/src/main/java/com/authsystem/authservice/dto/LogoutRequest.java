package com.authsystem.authservice.dto;

import lombok.Data;

@Data
public class LogoutRequest {
    private String refreshToken;
}