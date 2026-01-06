package com.haiintel.authservice.domain.port;

import java.time.Instant;

/**
 * Port interface for token revocation storage (Hexagonal Architecture).
 * 
 * Implementations:
 * - RedisTokenRevocationRepository (current)
 * - CosmosDBTokenRevocationRepository (future - Azure)
 * 
 * ✅ P0 FIX: Token revocation support
 * ✅ P2 FIX: Hexagonal architecture for storage abstraction
 */
public interface TokenRevocationRepository {
    
    /**
     * Revoke a specific token by its JTI.
     * 
     * @param jti JWT ID (unique token identifier)
     * @param expiresAt Token expiration time (for TTL)
     */
    void revokeToken(String jti, Instant expiresAt);
    
    /**
     * Revoke all tokens for a specific user.
     * 
     * @param email User email
     * @param issuedBefore Revoke all tokens issued before this time
     */
    void revokeAllUserTokens(String email, Instant issuedBefore);
    
    /**
     * Check if a token is revoked.
     * 
     * @param jti JWT ID
     * @return true if token is revoked
     */
    boolean isTokenRevoked(String jti);
    
    /**
     * Check if all user tokens issued before a certain time are revoked.
     * 
     * @param email User email
     * @param issuedAt Token issued time
     * @return true if user tokens are revoked
     */
    boolean areUserTokensRevoked(String email, Instant issuedAt);
}

