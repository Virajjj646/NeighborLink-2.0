package com.neighborlink.society_service.dto;

import com.neighborlink.society_service.entity.Society;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocietyResponse {

    private Long id;
    private String name;
    private String addressReference;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SocietyResponse from(Society society) {

        return SocietyResponse.builder()
                .id(society.getId())
                .name(society.getName())
                .addressReference(society.getAddressReference())
                .createdAt(society.getCreatedAt())
                .updatedAt(society.getUpdatedAt())
                .build();
    }
}