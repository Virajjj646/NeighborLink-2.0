package com.neighborlink.payment_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class RentalClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${RENTAL_SERVICE_URL}")
    private String rentalServiceUrl;

    @Value("${RENTAL_INTERNAL_SERVICE_KEY}")
    private String internalServiceKey;

    public void confirmRental(Long rentalId) {

        restClientBuilder
                .baseUrl(rentalServiceUrl)
                .build()
                .put()
                .uri("/internal/{id}/confirm", rentalId)
                .header(
                        "X-Internal-Service-Key",
                        internalServiceKey
                )
                .header(
                        HttpHeaders.CONTENT_TYPE,
                        "application/json"
                )
                .retrieve()
                .toBodilessEntity();
    }

    public void cancelRental(Long rentalId) {

        restClientBuilder
                .baseUrl(rentalServiceUrl)
                .build()
                .put()
                .uri("/internal/{id}/cancel", rentalId)
                .header(
                        "X-Internal-Service-Key",
                        internalServiceKey
                )
                .header(
                        HttpHeaders.CONTENT_TYPE,
                        "application/json"
                )
                .retrieve()
                .toBodilessEntity();
    }

    public void failRental(Long rentalId) {

        restClientBuilder
                .baseUrl(rentalServiceUrl)
                .build()
                .put()
                .uri("/internal/{id}/fail", rentalId)
                .header("X-Internal-Service-Key", internalServiceKey)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .retrieve()
                .toBodilessEntity();
    }
}