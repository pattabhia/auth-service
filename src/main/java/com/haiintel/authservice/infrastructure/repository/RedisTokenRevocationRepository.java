package com.haiintel.authservice.infrastructure.repository;

import com.haiintel.authservice.domain.port.TokenRevocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;

/**
 * Redis-based implementation of token revocation repository.
 * 
 * ✅ P0 FIX: Token revocation with Redis deny list
 * 
 * Redis keys:
 * - revoked:token:{jti} -> "1" (TTL = token expiration)
 * - revoked:user:{email} -> timestamp (TTL = max token lifetime)
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "token-revocation.enabled", havingValue = "true", matchIfMissing = true)
public class RedisTokenRevocationRepository implements TokenRevocationRepository {
    
    private final RedisTemplate<String, String> redisTemplate;
    private static final String TOKEN_PREFIX = "revoked:token:";
    private static final String USER_PREFIX = "revoked:user:";
    
    @Override
    public void revokeToken(String jti, Instant expiresAt) {
        String key = TOKEN_PREFIX + jti;
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        
        if (ttl.isNegative() || ttl.isZero()) {
            log.warn("Token already expired, not adding to revocation list: jti={}", jti);
            return;
        }
        
        redisTemplate.opsForValue().set(key, "1", ttl);
        log.info("Token revoked: jti={}, ttl={}s", jti, ttl.getSeconds());
    }
    
    @Override
    public void revokeAllUserTokens(String email, Instant issuedBefore) {
        String key = USER_PREFIX + email;
        String timestamp = String.valueOf(issuedBefore.getEpochSecond());
        
        // Store revocation timestamp with TTL = max token lifetime (1 hour)
        redisTemplate.opsForValue().set(key, timestamp, Duration.ofHours(1));
        log.info("All user tokens revoked: email={}, issuedBefore={}", email, issuedBefore);
    }
    
    @Override
    public boolean isTokenRevoked(String jti) {
        String key = TOKEN_PREFIX + jti;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }
    
    @Override
    public boolean areUserTokensRevoked(String email, Instant issuedAt) {
        String key = USER_PREFIX + email;
        String value = redisTemplate.opsForValue().get(key);
        
        if (value == null) {
            return false;
        }
        
        try {
            long revokedTimestamp = Long.parseLong(value);
            boolean revoked = issuedAt.getEpochSecond() < revokedTimestamp;
            
            if (revoked) {
                log.debug("User token revoked: email={}, issuedAt={}, revokedBefore={}", 
                    email, issuedAt, Instant.ofEpochSecond(revokedTimestamp));
            }
            
            return revoked;
        } catch (NumberFormatException e) {
            log.error("Invalid revocation timestamp for user: email={}, value={}", email, value);
            return false;
        }
    }
}

