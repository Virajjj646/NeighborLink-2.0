package com.neighborlink.auth_service.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        String role) {
}
