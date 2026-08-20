package com.neighborlink.auth_service.service;

import com.neighborlink.auth_service.dto.UserProfileRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class UserServiceClient {

    private final RestClient restClient;
    private final String userServiceUrl;
    private final String internalServiceKey;

    public UserServiceClient(
            RestClient restClient,
            @Value("${USER_SERVICE_URL}") String userServiceUrl,
            @Value("${USER_SERVICE_INTERNAL_KEY}") String internalServiceKey
    ) {
        this.restClient = restClient;
        this.userServiceUrl = userServiceUrl;
        this.internalServiceKey = internalServiceKey;
    }

    public void createProfile(
            String userId,
            String displayName
    ) {

        UserProfileRequest request =
                new UserProfileRequest(
                        userId,
                        displayName,
                        null
                );

        restClient.post()
                .uri(userServiceUrl + "/internal/profile")
                .header(
                        "X-Internal-Service-Key",
                        internalServiceKey
                )
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}