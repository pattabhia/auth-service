package com.haiintel.authservice.domain.model;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

/**
 * Domain model representing a JWT token.
 * Immutable value object.
 */
@Data
@Builder
public class JwtToken {
    String token;
    String jti; // JWT ID (unique identifier)
    String subject; // User email
    Role role;
    Instant issuedAt;
    Instant expiresAt;
    String keyId; // kid (key identifier for JWKS)
}
