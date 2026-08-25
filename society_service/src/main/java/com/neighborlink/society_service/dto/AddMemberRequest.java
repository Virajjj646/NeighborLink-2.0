package com.neighborlink.society_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddMemberRequest {

    @NotNull(message = "User ID is required")
    private String userId;

    private String role;
}
