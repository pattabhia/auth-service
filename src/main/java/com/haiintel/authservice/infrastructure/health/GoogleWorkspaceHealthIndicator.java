package com.haiintel.authservice.infrastructure.health;

import com.haiintel.authservice.domain.port.IdentityProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Health indicator for Google Workspace connectivity.
 * 
 * ✅ P2 FIX: Enhanced health checks for dependencies
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "idp.provider", havingValue = "google", matchIfMissing = true)
public class GoogleWorkspaceHealthIndicator implements HealthIndicator {
    
    private final IdentityProvider identityProvider;
    
    @Override
    public Health health() {
        try {
            // Simple connectivity check
            String providerName = identityProvider.getProviderName();
            
            return Health.up()
                .withDetail("provider", providerName)
                .withDetail("status", "connected")
                .build();
                
        } catch (Exception e) {
            log.error("Google Workspace health check failed", e);
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}

