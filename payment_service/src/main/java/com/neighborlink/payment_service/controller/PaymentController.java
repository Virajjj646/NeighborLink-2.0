package com.neighborlink.payment_service.controller;

import com.neighborlink.payment_service.dto.InternalPaymentRequest;
import com.neighborlink.payment_service.dto.PaymentResponse;
import com.neighborlink.payment_service.dto.ProviderCallbackRequest;
import com.neighborlink.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                paymentService.getPayment(
                        id,
                        authentication.getName(),
                        extractRole(authentication)
                )
        );
    }

    @GetMapping("/my")
    public ResponseEntity<List<PaymentResponse>> getMyPayments(
            Authentication authentication) {

        return ResponseEntity.ok(
                paymentService.getMyPayments(
                        authentication.getName()
                )
        );
    }

    @PutMapping("/{id}/failed")
    public ResponseEntity<PaymentResponse> markFailed(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                paymentService.markFailed(
                        id,
                        extractRole(authentication)
                )
        );
    }

    @PutMapping("/{id}/refund")
    public ResponseEntity<PaymentResponse> refund(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                paymentService.refund(
                        id,
                        extractRole(authentication)
                )
        );
    }

    private String extractRole(
            Authentication authentication) {

        return authentication
                .getAuthorities()
                .stream()
                .findFirst()
                .map(authority ->
                        authority.getAuthority()
                                .replace("ROLE_", "")
                )
                .orElse("USER");
    }

    @PostMapping("/internal")
    public ResponseEntity<PaymentResponse> createInternalPayment(
            @Valid @RequestBody InternalPaymentRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        paymentService.createInternalPayment(
                                request
                        )
                );
    }

    @GetMapping("/rental/{rentalId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByRental(
            @PathVariable Long rentalId,
            Authentication authentication) {

        return ResponseEntity.ok(
                paymentService.getPaymentsByRental(
                        rentalId,
                        authentication.getName(),
                        extractRole(authentication)
                )
        );
    }

    @PutMapping("/{id}/initiate")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                paymentService.initiatePayment(
                        id,
                        authentication.getName()
                )
        );
    }

    @PostMapping("/internal/provider/callback")
    public ResponseEntity<PaymentResponse> providerCallback(
            @Valid @RequestBody ProviderCallbackRequest request) {

        return ResponseEntity.ok(
                paymentService.settleByProviderReference(
                        request.providerReference(),
                        request.status(),
                        request.providerTransactionId()
                )
        );
    }

    @PostMapping("/internal/rental/{rentalId}/refund")
    public ResponseEntity<Void> refundForRental(
            @PathVariable Long rentalId) {

        paymentService.refundForRental(rentalId);

        return ResponseEntity.noContent().build();
    }
}