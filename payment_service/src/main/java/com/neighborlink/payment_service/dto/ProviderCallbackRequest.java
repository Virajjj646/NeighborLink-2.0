package com.neighborlink.payment_service.dto;

import com.neighborlink.payment_service.entity.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProviderCallbackRequest(

        @NotBlank(message = "Provider reference is required")
        String providerReference,

        @NotNull(message = "Status is required")
        PaymentStatus status,

        String providerTransactionId
) {}