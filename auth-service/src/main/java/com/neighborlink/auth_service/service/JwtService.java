package com.neighborlink.auth_service.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Claims;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expiration;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration
    ){
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }
    public String generateToken(String userId, String email, String role){
        Date now = new  Date();
        return  Jwts.builder()
                .subject(userId)
                .claim("email",email)
                .claim("role",role)
                .issuedAt(now)
                .expiration(new Date(now.getTime()+expiration))
                .signWith(key)
                .compact();
    }

    public String extractUserId(String token){
        return extractALlClaims(token)
                .get("sub",String.class);
    }

    public String extractEmail(String token) {

        return extractALlClaims(token)
                .get("email", String.class);
    }

    public String extractRole(String token){
        return extractALlClaims(token).get("role",String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            extractALlClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractALlClaims(String token) {
        return  Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
