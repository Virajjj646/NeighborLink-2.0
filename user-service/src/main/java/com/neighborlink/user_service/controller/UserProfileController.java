package com.neighborlink.user_service.controller;

import com.neighborlink.user_service.dto.CreateUserProfileRequest;
import com.neighborlink.user_service.dto.UserProfileRequest;
import com.neighborlink.user_service.dto.UserProfileResponse;
import com.neighborlink.user_service.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponse> getProfile(@PathVariable String id, Authentication authentication) {
        return ResponseEntity.ok(userProfileService.getProfile(id,authentication));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserProfileResponse> updateProfile(@PathVariable String id,
            @Valid @RequestBody UserProfileRequest request,Authentication authentication) {
        return ResponseEntity.ok(userProfileService.updateProfile(id, request,authentication));
    }

    @PostMapping("/internal/profile")
    public ResponseEntity<UserProfileResponse> createProfile(
            @Valid @RequestBody CreateUserProfileRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userProfileService.createProfile(request));
    }
}