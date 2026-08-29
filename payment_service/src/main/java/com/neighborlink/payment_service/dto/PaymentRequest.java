package com.neighborlink.payment_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

    @NotNull(message = "Rental ID is required")
    private Long rentalId;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;
}