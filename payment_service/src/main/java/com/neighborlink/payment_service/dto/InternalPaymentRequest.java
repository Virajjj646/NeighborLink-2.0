package com.neighborlink.payment_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternalPaymentRequest {

    @NotNull
    private Long rentalId;

    @NotBlank
    private String userId;

    @NotNull
    private BigDecimal amount;

    @NotBlank
    private String idempotencyKey;
}