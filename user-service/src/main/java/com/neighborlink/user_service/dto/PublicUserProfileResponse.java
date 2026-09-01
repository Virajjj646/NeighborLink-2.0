package com.neighborlink.user_service.dto;

public record PublicUserProfileResponse(
        String userId,
        String displayName,
        String profileImage
) {
}
