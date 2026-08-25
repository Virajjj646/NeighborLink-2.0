package com.neighborlink.society_service.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Service
public class JwtService {

    private final SecretKey key;

    public JwtService(
            @Value("${jwt.secret}") String secret) {

        this.key = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    public Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {

        return extractAllClaims(token)
                .getSubject();
    }

    public String extractRole(String token) {

        return extractAllClaims(token)
                .get("role", String.class);
    }

    public boolean isTokenValid(String token) {

        try {
            extractAllClaims(token);
            return true;

        } catch (Exception e) {

            System.err.println(
                    "===== SOCIETY JWT VALIDATION FAILED ====="
            );

            System.err.println(
                    "Exception: " + e.getClass().getName()
            );

            System.err.println(
                    "Message: " + e.getMessage()
            );

            System.err.println(
                    "=========================================="
            );

            return false;
        }
    }
}