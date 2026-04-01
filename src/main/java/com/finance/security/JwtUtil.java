package com.finance.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT utility for creating and validating tokens.
 *
 * Tradeoff: We use HS256 (symmetric HMAC) for simplicity.
 *
 * Token payload (claims):
 *   - sub  : user email (used to reload UserDetails on each request)
 *   - role : user's role (stored in token to avoid DB lookup on every request)
 *   - iat  : issued-at
 *   - exp  : expiry
 */
@Component
@Slf4j
public class JwtUtil {

    private final SecretKey  secretKey;
    private final long       expirationMs;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        this.secretKey    = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Generate a signed JWT token for an authenticated user.
     */
    public String generateToken(UserDetails userDetails, String role) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extract the username (email) from a token — used to load UserDetails.
     */
    public String extractUsername(String token) {
        return parseClaims(token).getPayload().getSubject();
    }

    /**
     * Validate a token: checks signature, expiry, and that username matches.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return parseClaims(token).getPayload().getExpiration().before(new Date());
    }

    /**
     * Parse and verify token signature. Throws JwtException on failure.
     */
    private Jws<Claims> parseClaims(String token) {
        return (Jws<Claims>) Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
    }

    /**
     * Safely validate without throwing — used for logging/debugging.
     */
    public boolean validateTokenSilently(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parse(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired");
        } catch (MalformedJwtException e) {
            log.warn("JWT token is malformed");
        } catch (JwtException e) {
            log.warn("JWT validation error: {}", e.getMessage());
        }
        return false;
    }
}
