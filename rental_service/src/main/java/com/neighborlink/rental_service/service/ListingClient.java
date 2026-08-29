package com.neighborlink.rental_service.service;

import com.neighborlink.rental_service.dto.ListingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
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

        System.out.println("====================================");
        System.out.println("Rental Service -> Listing Service");
        System.out.println("URL: " + listingServiceUrl + "/" + listingId);
        System.out.println("Authorization present: "
                + (authorizationHeader != null));
        System.out.println("====================================");

        try {

            ResponseEntity<ListingResponse> response =
                    restClientBuilder
                            .baseUrl(listingServiceUrl)
                            .build()
                            .get()
                            .uri("/" + listingId)
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    authorizationHeader
                            )
                            .retrieve()
                            .toEntity(ListingResponse.class);

            System.out.println(
                    "Listing Service response: "
                            + response.getStatusCode()
            );

            System.out.println(
                    "Listing Service body: "
                            + response.getBody()
            );

            return response.getBody();

        } catch (Exception ex) {

            System.out.println("====================================");
            System.out.println("LISTING SERVICE CALL FAILED");
            System.out.println("Exception: "
                    + ex.getClass().getName());
            System.out.println("Message: "
                    + ex.getMessage());
            System.out.println("====================================");

            throw ex;
        }
    }
}