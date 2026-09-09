package com.neighborlink.rental_service.service;

import com.neighborlink.rental_service.dto.ListingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class ListingClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${listing.service.url}")
    private String listingServiceUrl;

    public ListingResponse getListing(
            Long listingId,
            String authorizationHeader) {

        return restClientBuilder
                .baseUrl(listingServiceUrl)
                .build()
                .get()
                .uri("/" + listingId)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .retrieve()
                .body(ListingResponse.class);
    }
}