package com.neighborlink.rental_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class InternalServiceKeyFilter
        extends OncePerRequestFilter {

    @Value("${RENTAL_INTERNAL_SERVICE_KEY}")
    private String internalServiceKey;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        if (!request.getRequestURI().startsWith("/internal/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedKey =
                request.getHeader("X-Internal-Service-Key");

        if (providedKey == null
                || !providedKey.equals(internalServiceKey)) {

            response.setStatus(
                    HttpServletResponse.SC_FORBIDDEN
            );

            response.getWriter().write(
                    "Invalid internal service key"
            );

            return;
        }

        filterChain.doFilter(request, response);
    }
}