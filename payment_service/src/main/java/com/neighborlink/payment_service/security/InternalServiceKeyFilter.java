package com.neighborlink.payment_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InternalServiceKeyFilter
        extends OncePerRequestFilter {

    @Value("${PAYMENT_INTERNAL_SERVICE_KEY}")
    private String internalServiceKey;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        if (!request.getRequestURI()
                .startsWith("/internal")) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        String providedKey =
                request.getHeader(
                        "X-Internal-Service-Key"
                );

        if (providedKey == null
                || !providedKey.equals(internalServiceKey)) {

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            return;
        }

        filterChain.doFilter(
                request,
                response
        );
    }
}