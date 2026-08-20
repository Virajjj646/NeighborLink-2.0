package com.neighborlink.user_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

    @Component
    public class InternalServiceAuthenticationFilter
            extends OncePerRequestFilter {

        private static final String HEADER_NAME =
                "X-Internal-Service-Key";

        private final String internalServiceKey;

        public InternalServiceAuthenticationFilter(
                @Value("${USER_SERVICE_INTERNAL_KEY}")
                String internalServiceKey
        ) {
            this.internalServiceKey = internalServiceKey;
        }

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {

            if (!request.getRequestURI()
                    .equals("/internal/profile")) {

                filterChain.doFilter(request, response);
                return;
            }

            String providedKey =
                    request.getHeader(HEADER_NAME);

            if (providedKey == null
                    || !providedKey.equals(internalServiceKey)) {

                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            filterChain.doFilter(request, response);
        }
    }

