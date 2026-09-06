package com.neighborlink.payment_service.dto;

import com.neighborlink.payment_service.entity.Payment;
import com.neighborlink.payment_service.entity.PaymentStatus;
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
    private PaymentStatus status;
    private String providerTransactionId;
    private String idempotencyKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String providerReference;

    public static PaymentResponse from(Payment payment) {

        return PaymentResponse.builder()
                .id(payment.getId())
                .rentalId(payment.getRentalId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .providerTransactionId(
                        payment.getProviderTransactionId()
                )
                .idempotencyKey(
                        payment.getIdempotencyKey()
                )
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .providerReference(payment.getProviderReference())
                .build();
    }
}