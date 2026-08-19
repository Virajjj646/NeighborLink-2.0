package com.neighborlink.user_service.dto;

import java.time.LocalDateTime;

public record UserProfileResponse(
        String userId,
        String displayName,
        String phone,
        String profileImage,
        String bio,
        String addressReference,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}