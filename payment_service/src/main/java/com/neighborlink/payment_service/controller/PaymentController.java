package com.neighborlink.payment_service.controller;

import com.neighborlink.payment_service.dto.InternalPaymentRequest;
import com.neighborlink.payment_service.dto.PaymentRequest;
import com.neighborlink.payment_service.dto.PaymentResponse;
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

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        paymentService.createPayment(
                                request,
                                authentication.getName()
                        )
                );
    }

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

    @PutMapping("/{id}/pending")
    public ResponseEntity<PaymentResponse> markPending(
            @PathVariable Long id,
            Authentication authentication) {

        System.out.println("===== PAYMENT PENDING DEBUG =====");
        System.out.println("User: " + authentication.getName());
        System.out.println("Authorities: " + authentication.getAuthorities());
        System.out.println("=================================");

        return ResponseEntity.ok(
                paymentService.markPending(
                        id,
                        extractRole(authentication)
                )
        );
    }

    @PutMapping("/{id}/success")
    public ResponseEntity<PaymentResponse> markSuccess(
            @PathVariable Long id,
            @RequestParam String providerTransactionId,
            Authentication authentication) {

        System.out.println("===== PAYMENT SUCCESS DEBUG =====");
        System.out.println("User: " + authentication.getName());
        System.out.println("Authorities: " + authentication.getAuthorities());
        System.out.println("================================");

        return ResponseEntity.ok(
                paymentService.markSuccess(
                        id,
                        providerTransactionId,
                        extractRole(authentication)
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
}