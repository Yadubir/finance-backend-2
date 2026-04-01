package com.finance.controller;

import com.finance.dto.request.LoginRequest;
import com.finance.dto.response.AuthResponse;
import com.finance.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller — public endpoints (no JWT required).
 *
 * Kept intentionally thin: validation is handled by @Valid + Bean Validation,
 * business logic lives in AuthService, errors are handled by GlobalExceptionHandler.
 * Controllers are pure HTTP adapters.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login and token management")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(
        summary = "Login",
        description = "Authenticate with email and password. Returns a JWT Bearer token valid for 24 hours."
    )
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
