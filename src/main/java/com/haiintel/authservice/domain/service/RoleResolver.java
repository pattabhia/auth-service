package com.haiintel.authservice.domain.service;

import com.haiintel.authservice.domain.model.Role;
import com.haiintel.authservice.domain.model.UserPrincipal;
import com.haiintel.authservice.infrastructure.config.AuthorizationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Domain service for resolving user roles from group memberships.
 * 
 * ✅ P2 FIX: Configuration-based role mapping (was hardcoded)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleResolver {
    
    private final AuthorizationProperties authorizationProperties;
    
    /**
     * Resolve user role from group memberships.
     * Returns the highest privilege role (lowest priority number).
     * Falls back to default role (INTERN) if no groups match.
     * 
     * @param user User principal with groups
     * @return User principal with resolved role
     */
    public UserPrincipal resolveRole(UserPrincipal user) {
        List<String> userGroups = user.getGroups();
        
        if (userGroups == null || userGroups.isEmpty()) {
            log.warn("User {} has no groups, assigning default role: {}", 
                user.getEmail(), authorizationProperties.getDefaultRole());
            return user.toBuilder()
                .role(authorizationProperties.getDefaultRole())
                .build();
        }
        
        // Find highest privilege role from group mappings
        Role resolvedRole = authorizationProperties.getRoleMappings().stream()
            .filter(mapping -> userGroups.contains(mapping.getGroup()))
            .map(AuthorizationProperties.RoleMapping::getRole)
            .reduce(Role::max)  // Get highest privilege (lowest priority)
            .orElse(authorizationProperties.getDefaultRole());
        
        log.debug("Resolved role for user {}: {} (groups: {})", 
            user.getEmail(), resolvedRole, userGroups);
        
        return user.toBuilder()
            .role(resolvedRole)
            .build();
    }
}

