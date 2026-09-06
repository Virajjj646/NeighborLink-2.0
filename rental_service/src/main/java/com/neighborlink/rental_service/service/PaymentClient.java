package com.neighborlink.rental_service.service;

import com.neighborlink.rental_service.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class PaymentClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${PAYMENT_SERVICE_URL}")
    private String paymentServiceUrl;

    @Value("${PAYMENT_INTERNAL_SERVICE_KEY}")
    private String internalServiceKey;

    public PaymentResponse createPayment(
            Long rentalId,
            String userId,
            java.math.BigDecimal amount,
            String idempotencyKey) {

        return restClientBuilder
                .baseUrl(paymentServiceUrl)
                .build()
                .post()
                .uri("/internal")
                .header(
                        "X-Internal-Service-Key",
                        internalServiceKey
                )
                .header(
                        HttpHeaders.CONTENT_TYPE,
                        "application/json"
                )
                .body(
                        new InternalPaymentRequest(
                                rentalId,
                                userId,
                                amount,
                                idempotencyKey
                        )
                )
                .retrieve()
                .body(PaymentResponse.class);
    }

    private record InternalPaymentRequest(
            Long rentalId,
            String userId,
            java.math.BigDecimal amount,
            String idempotencyKey
    ) {
    }

    public void refundForRental(Long rentalId) {

        restClientBuilder
                .baseUrl(paymentServiceUrl)
                .build()
                .post()
                .uri("/internal/rental/{rentalId}/refund", rentalId)
                .header("X-Internal-Service-Key", internalServiceKey)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .retrieve()
                .toBodilessEntity();
    }
}