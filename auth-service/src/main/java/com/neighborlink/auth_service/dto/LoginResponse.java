package com.neighborlink.auth_service.dto;

public record LoginResponse(
        String userId,
        String email,
        String accessToken,
        String refreshToken,
        String tokenType,
        String role) {
}
