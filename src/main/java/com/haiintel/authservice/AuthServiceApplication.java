package com.haiintel.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * HAI-Indexer Authentication Service
 * 
 * Enterprise-grade authentication and authorization service with:
 * - Google Workspace integration (OAuth 2.0 client)
 * - JWT token issuance (RS256 with JWKS)
 * - Token revocation (Redis-based deny list)
 * - Audit logging (SOC 2 compliant)
 * - Circuit breaker (Resilience4j)
 * - Rate limiting (Istio-based)
 * 
 * @version 2.1.1
 * @author HAI-Intel Team
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
@EnableAsync
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}

