package com.neighborlink.payment_service.security;

import com.neighborlink.payment_service.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String header =
                request.getHeader("Authorization");

        if (header == null
                || !header.startsWith("Bearer ")) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        String token =
                header.substring(7);

        if (!jwtService.isTokenValid(token)) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        Claims claims =
                jwtService.extractAllClaims(token);

        String userId =
                claims.getSubject();

        String role =
                claims.get("role", String.class);

        System.out.println("===== JWT DEBUG =====");
        System.out.println("User ID: " + userId);
        System.out.println("Role from JWT: " + role);
        System.out.println("Token valid: " + jwtService.isTokenValid(token));
        System.out.println("=====================");

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_" + role
                                )
                        )
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(
                        authentication
                );

        filterChain.doFilter(
                request,
                response
        );
    }
}