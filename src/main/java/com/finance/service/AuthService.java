package com.finance.service;

import com.finance.dto.request.LoginRequest;
import com.finance.dto.response.AuthResponse;
import com.finance.security.JwtUtil;
import com.finance.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Handles user authentication and JWT token issuance.
 *
 * Design: delegates credential verification entirely to Spring Security's
 * AuthenticationManager (which calls DaoAuthenticationProvider → BCrypt compare).
 * This service only orchestrates the flow and builds the response.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil               jwtUtil;

    /**
     * Authenticates the user and returns a signed JWT.
     *
     * Spring's AuthenticationManager throws:
     *   - BadCredentialsException  if the password doesn't match
     *   - DisabledException        if the account is INACTIVE
     * Both are caught by GlobalExceptionHandler and mapped to 401.
     */
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String token = jwtUtil.generateToken(principal, principal.getRole());

        log.info("User logged in: {} with role: {}", principal.getUsername(), principal.getRole());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(principal.getId())
                .email(principal.getUsername())
                .fullName(principal.getUser().getFullName())
                .role(principal.getUser().getRole())
                .build();
    }
}
