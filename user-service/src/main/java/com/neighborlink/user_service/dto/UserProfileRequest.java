package com.neighborlink.user_service.dto;

import jakarta.validation.constraints.NotBlank;

public record UserProfileRequest(

        @NotBlank(message = "Display name is required")
        String displayName,

        @NotBlank(message = "Phone is required")
        String phone,

        String profileImage,

        String bio,

        String addressReference
) {
}