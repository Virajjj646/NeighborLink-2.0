package com.neighborlink.payment_service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ErrorResponse (
        int status,
        String error,
        String message,
        LocalDateTime timestamp
) {
}
