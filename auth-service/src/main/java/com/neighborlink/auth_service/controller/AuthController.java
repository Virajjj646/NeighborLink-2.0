package com.neighborlink.auth_service.controller;

import com.neighborlink.auth_service.dto.LoginRequest;
import com.neighborlink.auth_service.dto.LoginResponse;
import com.neighborlink.auth_service.dto.RefreshRequest;
import com.neighborlink.auth_service.dto.RegisterRequest;
import com.neighborlink.auth_service.entity.RefreshToken;
import com.neighborlink.auth_service.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request){
        authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request){
        return ResponseEntity.ok(
                authService.login(request)
        );
    }
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(
                authService.refresh(request)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request){
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
