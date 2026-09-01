package com.neighborlink.auth_service.dto;

public record RegisterResponse (
        String userId,
        String name,
        String email,
        String role
){}

