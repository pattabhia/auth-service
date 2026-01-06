package com.haiintel.authservice.infrastructure.config;

import com.haiintel.authservice.domain.model.Role;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Authorization configuration properties.
 * 
 * ✅ P2 FIX: Configuration-based role mapping (was hardcoded)
 */
@Data
@Component
@ConfigurationProperties(prefix = "authorization")
public class AuthorizationProperties {
    
    private List<RoleMapping> roleMappings = new ArrayList<>();
    private Role defaultRole = Role.INTERN;
    
    @Data
    public static class RoleMapping {
        private String group;
        private Role role;
        private int priority;
    }
}

