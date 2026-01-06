package com.haiintel.authservice.infrastructure.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RedisTokenRevocationRepository.
 * Uses Testcontainers for Redis.
 * 
 * ✅ P0 FIX: Tests for token revocation functionality
 */
@SpringBootTest
@Testcontainers
class RedisTokenRevocationRepositoryTest {
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379);
    
    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }
    
    @Autowired
    private RedisTokenRevocationRepository repository;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @BeforeEach
    void setUp() {
        // Clear Redis before each test
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }
    
    @Test
    void shouldRevokeToken() {
        String jti = "test-jti-123";
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
        
        repository.revokeToken(jti, expiresAt);
        
        assertTrue(repository.isTokenRevoked(jti));
    }
    
    @Test
    void shouldNotRevokeExpiredToken() {
        String jti = "expired-jti-456";
        Instant expiresAt = Instant.now().minus(1, ChronoUnit.HOURS);
        
        repository.revokeToken(jti, expiresAt);
        
        assertFalse(repository.isTokenRevoked(jti));
    }
    
    @Test
    void shouldRevokeAllUserTokens() {
        String email = "test@haiintel.com";
        Instant now = Instant.now();
        Instant before = now.minus(1, ChronoUnit.MINUTES);
        Instant after = now.plus(1, ChronoUnit.MINUTES);
        
        repository.revokeAllUserTokens(email, now);
        
        assertTrue(repository.areUserTokensRevoked(email, before));
        assertFalse(repository.areUserTokensRevoked(email, after));
    }
    
    @Test
    void shouldReturnFalseForNonRevokedToken() {
        String jti = "non-revoked-jti";
        
        assertFalse(repository.isTokenRevoked(jti));
    }
    
    @Test
    void shouldReturnFalseForNonRevokedUserTokens() {
        String email = "test@haiintel.com";
        Instant issuedAt = Instant.now();
        
        assertFalse(repository.areUserTokensRevoked(email, issuedAt));
    }
}

