package com.neighborlink.rental_service.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private Long id;

    private Long rentalId;

    private String userId;

    private BigDecimal amount;

    private String status;

    private String providerTransactionId;

    private String idempotencyKey;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}