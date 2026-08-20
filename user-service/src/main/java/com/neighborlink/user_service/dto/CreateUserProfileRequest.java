package com.neighborlink.user_service.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUserProfileRequest(

        @NotBlank(message = "User ID is required")
        String userId,

        @NotBlank(message = "Display name is required")
        String displayName,

        String phone
) {
}