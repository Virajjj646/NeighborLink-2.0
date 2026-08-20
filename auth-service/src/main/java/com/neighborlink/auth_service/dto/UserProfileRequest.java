package com.neighborlink.auth_service.dto;

public record UserProfileRequest(
        String userId,
        String displayName,
        String phone
) {
}