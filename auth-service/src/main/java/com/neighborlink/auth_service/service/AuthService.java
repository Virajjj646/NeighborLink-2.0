package com.neighborlink.auth_service.service;

import com.neighborlink.auth_service.dto.LoginRequest;
import com.neighborlink.auth_service.dto.LoginResponse;
import com.neighborlink.auth_service.dto.RefreshRequest;
import com.neighborlink.auth_service.dto.RegisterRequest;
import com.neighborlink.auth_service.entity.RefreshToken;
import com.neighborlink.auth_service.entity.Role;
import com.neighborlink.auth_service.entity.User;
import com.neighborlink.auth_service.exception.AuthException;
import com.neighborlink.auth_service.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService  jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserServiceClient userServiceClient;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, RefreshTokenService refreshTokenService, UserServiceClient userServiceClient) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userServiceClient = userServiceClient;
    }

    public void register(RegisterRequest request){
        if(userRepository.existsByEmail(request.email())) throw new AuthException(HttpStatus.CONFLICT,"Email already exists");

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);

        try {

            userServiceClient.createProfile(
                    savedUser.getId(),
                    savedUser.getName()
            );

        } catch (Exception exception) {

            userRepository.delete(savedUser);

            throw new AuthException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to create user profile"
            );
        }
    }

    public LoginResponse login(LoginRequest request){
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED,"Invalid email/password"));
        if(!passwordEncoder.matches(request.password(), user.getPassword())) throw new AuthException(HttpStatus.UNAUTHORIZED,"Invalid password");
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
                        new AuthException(HttpStatus.NOT_FOUND,"User not found"));

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
