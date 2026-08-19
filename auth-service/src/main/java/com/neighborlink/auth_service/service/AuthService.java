package com.neighborlink.auth_service.service;

import com.neighborlink.auth_service.dto.LoginRequest;
import com.neighborlink.auth_service.dto.LoginResponse;
import com.neighborlink.auth_service.dto.RefreshRequest;
import com.neighborlink.auth_service.dto.RegisterRequest;
import com.neighborlink.auth_service.entity.RefreshToken;
import com.neighborlink.auth_service.entity.Role;
import com.neighborlink.auth_service.entity.User;
import com.neighborlink.auth_service.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService  jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, RefreshTokenService refreshTokenService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    public void register(RegisterRequest request){
        if(userRepository.existsByEmail(request.email())) throw new RuntimeException("Email already exists");

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();

        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request){
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Invalid email/password"));
        if(!passwordEncoder.matches(request.password(), user.getPassword())) throw new RuntimeException("Invalid password");
        String accessToken = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
        String refreshToken = refreshTokenService.createRefreshToken(user);
        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                user.getRole().name()
        );
    }
    public LoginResponse refresh(RefreshRequest request) {

        RefreshToken storedToken =
                refreshTokenService.validateRefreshToken(
                        request.refreshToken()
                );

        User user = userRepository.findById(storedToken.getUserID())
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        refreshTokenService.revokeRefreshToken(storedToken);

        String newAccessToken = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        String newRefreshToken =
                refreshTokenService.createRefreshToken(user);
        return new LoginResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                user.getRole().name()
        );
    }

    public void logout(String refreshToken){
        refreshTokenService.logout(refreshToken);
    }
}
