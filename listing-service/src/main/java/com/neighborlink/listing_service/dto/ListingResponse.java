package com.neighborlink.listing_service.dto;

import com.neighborlink.listing_service.entity.Listing;
import com.neighborlink.listing_service.entity.ListingStatus;
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
    private ListingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ListingResponse from(Listing listing) {

        return ListingResponse.builder()
                .id(listing.getId())
                .ownerId(listing.getOwnerId())
                .societyId(listing.getSocietyId())
                .title(listing.getTitle())
                .description(listing.getDescription())
                .category(listing.getCategory())
                .pricePerDay(listing.getPricePerDay())
                .status(listing.getStatus())
                .createdAt(listing.getCreatedAt())
                .updatedAt(listing.getUpdatedAt())
                .build();
    }
}