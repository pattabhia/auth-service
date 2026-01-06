package com.haiintel.authservice.domain.service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.haiintel.authservice.domain.model.JwtToken;
import com.haiintel.authservice.domain.model.Role;
import com.haiintel.authservice.domain.model.UserPrincipal;
import com.haiintel.authservice.domain.port.TokenRevocationRepository;
import com.haiintel.authservice.infrastructure.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Domain service for JWT token operations.
 * 
 * ✅ P0 FIX: JWT expiration = 1 hour (was 8 hours)
 * ✅ P1 FIX: kid generation = timestamp-based (was date-based)
 * ✅ P0 FIX: Token revocation check
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    private final JwtProperties jwtProperties;
    private final TokenRevocationRepository tokenRevocationRepository;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    /**
     * Issue JWT token for authenticated user.
     * 
     * @param user Authenticated user principal
     * @return JWT token
     */
    public JwtToken issueToken(UserPrincipal user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.getExpirationHours(), ChronoUnit.HOURS);
        String jti = UUID.randomUUID().toString();

        // ✅ P1 FIX: Timestamp-based kid (supports multiple rotations per day)
        String kid = generateKeyId();

        String token = Jwts.builder()
                .header()
                .keyId(kid)
                .and()
                .subject(user.getEmail())
                .claim("name", user.getName())
                .claim("role", user.getRole().name())
                .claim("groups", user.getGroups())
                .issuer(jwtProperties.getIssuer())
                .audience().add(jwtProperties.getAudience()).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .id(jti)
                .signWith(getPrivateKey())
                .compact();

        log.debug("Issued JWT token: jti={}, sub={}, exp={}", jti, user.getEmail(), expiresAt);

        return JwtToken.builder()
                .token(token)
                .jti(jti)
                .subject(user.getEmail())
                .role(user.getRole())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .keyId(kid)
                .build();
    }

    /**
     * Validate JWT token and return user principal.
     * 
     * ✅ P0 FIX: Check token revocation
     * 
     * @param token JWT token string
     * @return User principal
     * @throws io.jsonwebtoken.JwtException if token is invalid
     */
    public UserPrincipal validateToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getPublicKey())
                .requireIssuer(jwtProperties.getIssuer())
                .requireAudience(jwtProperties.getAudience())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String jti = claims.getId();
        String email = claims.getSubject();
        Instant issuedAt = claims.getIssuedAt().toInstant();

        // ✅ P0 FIX: Check if token is revoked
        if (tokenRevocationRepository.isTokenRevoked(jti)) {
            log.warn("Token revoked: jti={}", jti);
            throw new TokenRevokedException("Token has been revoked");
        }

        if (tokenRevocationRepository.areUserTokensRevoked(email, issuedAt)) {
            log.warn("User tokens revoked: email={}, issuedAt={}", email, issuedAt);
            throw new TokenRevokedException("All user tokens have been revoked");
        }

        return UserPrincipal.builder()
                .email(email)
                .name(claims.get("name", String.class))
                .role(Role.valueOf(claims.get("role", String.class)))
                .groups(claims.get("groups", List.class))
                .build();
    }

    /**
     * Extract JTI from token without full validation.
     */
    public String extractJti(String token) {
        return Jwts.parser()
                .unsecured()
                .build()
                .parseUnsecuredClaims(token)
                .getPayload()
                .getId();
    }

    /**
     * Generate key ID for JWKS.
     * ✅ P1 FIX: Timestamp-based (was date-based, only 1 rotation per day)
     */
    private String generateKeyId() {
        return String.valueOf(Instant.now().getEpochSecond());
    }

    /**
     * Get public key for JWKS endpoint.
     */
    public PublicKey getPublicKey() {
        if (publicKey == null) {
            try {
                String keyContent = Files.readString(Paths.get(jwtProperties.getPublicKeyFile()))
                        .replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", "")
                        .replaceAll("\\s", "");

                byte[] keyBytes = Base64.getDecoder().decode(keyContent);
                X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                publicKey = keyFactory.generatePublic(spec);

                log.info("Loaded public key from: {}", jwtProperties.getPublicKeyFile());
            } catch (Exception e) {
                throw new RuntimeException("Failed to load public key", e);
            }
        }
        return publicKey;
    }

    /**
     * Get private key for signing.
     */
    private PrivateKey getPrivateKey() {
        if (privateKey == null) {
            try {
                String keyContent = Files.readString(Paths.get(jwtProperties.getPrivateKeyFile()))
                        .replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                        .replaceAll("\\s", "");

                byte[] keyBytes = Base64.getDecoder().decode(keyContent);
                PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                privateKey = keyFactory.generatePrivate(spec);

                log.info("Loaded private key from: {}", jwtProperties.getPrivateKeyFile());
            } catch (Exception e) {
                throw new RuntimeException("Failed to load private key", e);
            }
        }
        return privateKey;
    }

    /**
     * Custom exception for revoked tokens.
     */
    public static class TokenRevokedException extends RuntimeException {
        public TokenRevokedException(String message) {
            super(message);
        }
    }
}
