package com.neighborlink.rental_service.controller;

import com.neighborlink.rental_service.dto.RentalAvailabilityResponse;
import com.neighborlink.rental_service.dto.RentalRequest;
import com.neighborlink.rental_service.dto.RentalResponse;
import com.neighborlink.rental_service.entity.RentalStatus;
import com.neighborlink.rental_service.service.RentalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RentalController {

    private final RentalService rentalService;

    @PostMapping
    public ResponseEntity<RentalResponse> createRental(
            @Valid @RequestBody RentalRequest request,
            Authentication authentication,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        rentalService.createRental(
                                request,
                                authentication.getName(),
                                authorizationHeader
                        )
                );
    }

    @GetMapping
    public ResponseEntity<List<RentalResponse>> getMyRentals(
            Authentication authentication) {

        return ResponseEntity.ok(
                rentalService.getMyRentals(
                        extractUserId(authentication)
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<RentalResponse> getRentalById(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                rentalService.getRentalById(
                        id,
                        extractUserId(authentication),
                        extractRole(authentication)
                )
        );
    }

    @GetMapping("/listing/{listingId}")
    public ResponseEntity<List<RentalResponse>> getRentalsByListing(
            @PathVariable Long listingId,
            Authentication authentication,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {

        return ResponseEntity.ok(
                rentalService.getRentalsByListing(
                        listingId,
                        extractUserId(authentication),
                        extractRole(authentication),
                        authorizationHeader
                )
        );
    }

    @GetMapping("/listing/{listingId}/availability")
    public ResponseEntity<List<RentalAvailabilityResponse>> getListingAvailability(
            @PathVariable Long listingId) {

        return ResponseEntity.ok(
                rentalService.getListingAvailability(listingId)
        );
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<RentalResponse> cancelRental(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                rentalService.cancelRental(
                        id,
                        extractUserId(authentication),
                        extractRole(authentication)
                )
        );
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<RentalResponse> completeRental(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                rentalService.completeRental(
                        id,
                        extractRole(authentication)
                )
        );
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<RentalResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam RentalStatus status,
            Authentication authentication) {

        return ResponseEntity.ok(
                rentalService.updateStatus(
                        id,
                        status,
                        extractRole(authentication)
                )
        );
    }

    private String extractUserId(
            Authentication authentication) {

        return authentication.getName();
    }

    private String extractRole(
            Authentication authentication) {

        return authentication.getAuthorities()
                .stream()
                .findFirst()
                .map(authority ->
                        authority.getAuthority()
                                .replace("ROLE_", ""))
                .orElse("USER");
    }

    @PutMapping("/internal/{id}/confirm")
    public ResponseEntity<RentalResponse> confirmRentalInternal(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                rentalService.confirmRental(id)
        );
    }

    @PutMapping("/internal/{id}/cancel")
    public ResponseEntity<RentalResponse> cancelRentalInternal(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                rentalService.cancelRentalInternal(id)
        );
    }

    @GetMapping("/all")
    public ResponseEntity<List<RentalResponse>> getAllRentals(
            @RequestParam(required = false) RentalStatus status,
            Authentication authentication) {

        return ResponseEntity.ok(
                rentalService.getAllRentals(
                        extractRole(authentication),
                        status
                )
        );
    }

    @PutMapping("/internal/{id}/fail")
    public ResponseEntity<RentalResponse> failRentalInternal(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                rentalService.failRentalInternal(id)
        );
    }
}