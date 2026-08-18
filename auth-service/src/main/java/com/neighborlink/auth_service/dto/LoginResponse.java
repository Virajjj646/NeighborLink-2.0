package com.neighborlink.auth_service.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        String role) {
}
