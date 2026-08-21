package com.neighborlink.society_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocietyRequest {

    private String name;

    private String addressReference;
}
