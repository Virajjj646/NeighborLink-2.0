package com.neighborlink.auth_service.service;

import com.neighborlink.auth_service.entity.RefreshToken;
import com.neighborlink.auth_service.entity.User;
import com.neighborlink.auth_service.exception.AuthException;
import com.neighborlink.auth_service.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenExpiration;

    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${JWT_REFRESH_EXPIRATION:604800000}") long refreshTokenExpiration
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String createRefreshToken(User user) {

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String refreshToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .userID(user.getId())
                .tokenHash(hashToken(refreshToken))
                .expiryAt(
                        LocalDateTime.now()
                                .plusNanos(refreshTokenExpiration * 1_000_000)
                )
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        return refreshToken;
    }

    public RefreshToken validateRefreshToken(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        RefreshToken storedToken = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(()->
                        new AuthException(HttpStatus.UNAUTHORIZED,"Invalid refresh token"));
        if(storedToken.isRevoked()){
            throw new AuthException(HttpStatus.UNAUTHORIZED,"Refresh token has been revoked");
        }
        if(storedToken.getExpiryAt().isBefore(LocalDateTime.now())){
            throw new AuthException(HttpStatus.UNAUTHORIZED,"Refresh token has expired");
        }
        return storedToken;
    }

    public void revokeRefreshToken(RefreshToken refreshToken) {
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    public void logout(String refreshToken) {

        String tokenHash = hashToken(refreshToken);

        RefreshToken storedToken = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() ->
                        new AuthException(HttpStatus.UNAUTHORIZED,"Invalid refresh token"));

        if (storedToken.isRevoked()) {
            throw new AuthException(HttpStatus.UNAUTHORIZED,"Refresh token has already been revoked");
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);
    }

    private String hashToken(String token) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getEncoder()
                    .encodeToString(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is not available", e);
        }
    }
}