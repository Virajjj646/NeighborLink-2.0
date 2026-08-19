package com.neighborlink.auth_service.config;

import com.neighborlink.auth_service.entity.Role;
import com.neighborlink.auth_service.entity.User;
import com.neighborlink.auth_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${INITIAL_ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${INITIAL_ADMIN_PASSWORD:}")
    private String adminPassword;

    @Value("${INITIAL_ADMIN_NAME:System Admin}")
    private String adminName;

    public AdminBootstrap(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }

        if (adminEmail == null || adminEmail.isBlank()) {
            throw new IllegalStateException(
                    "INITIAL_ADMIN_EMAIL must be configured when no ADMIN exists"
            );
        }

        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "INITIAL_ADMIN_PASSWORD must be configured when no ADMIN exists"
            );
        }

        if (userRepository.existsByEmail(adminEmail)) {
            throw new IllegalStateException(
                    "INITIAL_ADMIN_EMAIL already belongs to an existing non-admin user"
            );
        }

        User admin = User.builder()
                .name(adminName)
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .build();

        userRepository.save(admin);

        System.out.println(
                "Initial ADMIN account created: " + adminEmail
        );
    }
}