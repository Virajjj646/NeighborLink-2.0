package com.neighborlink.rental_service.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingResponse {

    private Long id;

    private String ownerId;

    private Long societyId;

    private String title;

    private String description;

    private String category;

    private BigDecimal pricePerDay;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}