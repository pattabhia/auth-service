package com.haiintel.authservice.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT configuration properties.
 * 
 * ✅ P0 FIX: expiration-hours = 1 (was 8)
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String algorithm = "RS256";
    private String privateKeyFile;
    private String publicKeyFile;
    private String issuer;
    private String audience;
    private int expirationHours = 1;  // ✅ P0 FIX: 1 hour (was 8)
}

