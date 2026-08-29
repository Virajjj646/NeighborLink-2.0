package com.neighborlink.rental_service.dto;

import com.neighborlink.rental_service.entity.Rental;
import com.neighborlink.rental_service.entity.RentalStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalResponse {

    private Long id;
    private String renterId;
    private Long listingId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalAmount;
    private RentalStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RentalResponse from(Rental rental) {

        return RentalResponse.builder()
                .id(rental.getId())
                .renterId(rental.getRenterId())
                .listingId(rental.getListingId())
                .startDate(rental.getStartDate())
                .endDate(rental.getEndDate())
                .totalAmount(rental.getTotalAmount())
                .status(rental.getStatus())
                .createdAt(rental.getCreatedAt())
                .updatedAt(rental.getUpdatedAt())
                .build();
    }
}