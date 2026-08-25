package com.neighborlink.society_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocietyRequest {

    @NotBlank(message = "Society name is required")
    private String name;

    private String addressReference;
}
