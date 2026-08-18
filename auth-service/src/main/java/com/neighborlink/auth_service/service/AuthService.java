package com.neighborlink.auth_service.service;

import com.neighborlink.auth_service.dto.LoginRequest;
import com.neighborlink.auth_service.dto.LoginResponse;
import com.neighborlink.auth_service.dto.RegisterRequest;
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

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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
        String token = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
        return new LoginResponse(
                token,
                "Bearer",
                user.getRole().name()
        );
    }
}
