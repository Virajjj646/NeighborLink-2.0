package com.neighborlink.rental_service.dto;

import java.time.LocalDate;

public record RentalAvailabilityResponse (
        LocalDate startDate,
        LocalDate endDate
) {}
