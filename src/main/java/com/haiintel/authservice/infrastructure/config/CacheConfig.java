package com.haiintel.authservice.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.haiintel.authservice.infrastructure.config.GoogleWorkspaceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for Google Workspace API responses.
 * 
 * ✅ P1 FIX: Cache TTL = 5 minutes (was 1 hour in JWKS)
 */
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class CacheConfig {
    
    private final GoogleWorkspaceProperties googleWorkspaceProperties;
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("userGroups");
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }
    
    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
            .expireAfterWrite(
                googleWorkspaceProperties.getCache().getTtlMinutes(), 
                TimeUnit.MINUTES
            )
            .maximumSize(googleWorkspaceProperties.getCache().getMaxSize())
            .recordStats();
    }
}

