package com.neighborlink.user_service.controller;

import com.neighborlink.user_service.dto.UserProfileRequest;
import com.neighborlink.user_service.dto.UserProfileResponse;
import com.neighborlink.user_service.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponse> getProfile(@PathVariable String id) {
        return ResponseEntity.ok(userProfileService.getProfile(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserProfileResponse> updateProfile(@PathVariable String id,
            @Valid @RequestBody UserProfileRequest request) {
        return ResponseEntity.ok(userProfileService.updateProfile(id, request));
    }
}