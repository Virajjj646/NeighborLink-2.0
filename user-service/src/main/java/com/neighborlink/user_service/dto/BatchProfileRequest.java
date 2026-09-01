package com.neighborlink.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchProfileRequest(

        @NotEmpty(message = "At least one user ID is required")
        @Size(max=100,message="At most 100 user IDs per request")
        List<String> userIds
) {
}
