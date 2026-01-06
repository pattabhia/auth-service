package com.haiintel.authservice.domain.service;

import com.haiintel.authservice.domain.model.Role;
import com.haiintel.authservice.domain.model.UserPrincipal;
import com.haiintel.authservice.infrastructure.config.AuthorizationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RoleResolver.
 * 
 * ✅ P2 FIX: Tests for configuration-based role mapping
 */
class RoleResolverTest {
    
    private RoleResolver roleResolver;
    private AuthorizationProperties properties;
    
    @BeforeEach
    void setUp() {
        properties = new AuthorizationProperties();
        properties.setDefaultRole(Role.INTERN);
        
        // Configure role mappings
        AuthorizationProperties.RoleMapping adminMapping = new AuthorizationProperties.RoleMapping();
        adminMapping.setGroup("admin@haiintel.com");
        adminMapping.setRole(Role.ADMIN);
        adminMapping.setPriority(1);
        
        AuthorizationProperties.RoleMapping employeeMapping = new AuthorizationProperties.RoleMapping();
        employeeMapping.setGroup("employees@haiintel.com");
        employeeMapping.setRole(Role.EMPLOYEE);
        employeeMapping.setPriority(2);
        
        AuthorizationProperties.RoleMapping internMapping = new AuthorizationProperties.RoleMapping();
        internMapping.setGroup("intern@haiintel.com");
        internMapping.setRole(Role.INTERN);
        internMapping.setPriority(3);
        
        properties.setRoleMappings(List.of(adminMapping, employeeMapping, internMapping));
        
        roleResolver = new RoleResolver(properties);
    }
    
    @Test
    void shouldResolveAdminRole() {
        UserPrincipal user = UserPrincipal.builder()
            .email("john@haiintel.com")
            .name("John Doe")
            .groups(List.of("admin@haiintel.com", "employees@haiintel.com"))
            .build();
        
        UserPrincipal result = roleResolver.resolveRole(user);
        
        assertEquals(Role.ADMIN, result.getRole());
    }
    
    @Test
    void shouldResolveEmployeeRole() {
        UserPrincipal user = UserPrincipal.builder()
            .email("jane@haiintel.com")
            .name("Jane Doe")
            .groups(List.of("employees@haiintel.com"))
            .build();
        
        UserPrincipal result = roleResolver.resolveRole(user);
        
        assertEquals(Role.EMPLOYEE, result.getRole());
    }
    
    @Test
    void shouldResolveInternRole() {
        UserPrincipal user = UserPrincipal.builder()
            .email("intern@haiintel.com")
            .name("Intern User")
            .groups(List.of("intern@haiintel.com"))
            .build();
        
        UserPrincipal result = roleResolver.resolveRole(user);
        
        assertEquals(Role.INTERN, result.getRole());
    }
    
    @Test
    void shouldUseDefaultRoleWhenNoGroupsMatch() {
        UserPrincipal user = UserPrincipal.builder()
            .email("unknown@haiintel.com")
            .name("Unknown User")
            .groups(List.of("unknown-group@haiintel.com"))
            .build();
        
        UserPrincipal result = roleResolver.resolveRole(user);
        
        assertEquals(Role.INTERN, result.getRole());
    }
    
    @Test
    void shouldUseDefaultRoleWhenNoGroups() {
        UserPrincipal user = UserPrincipal.builder()
            .email("nogroups@haiintel.com")
            .name("No Groups User")
            .groups(List.of())
            .build();
        
        UserPrincipal result = roleResolver.resolveRole(user);
        
        assertEquals(Role.INTERN, result.getRole());
    }
    
    @Test
    void shouldSelectHighestPrivilegeRole() {
        // User in both EMPLOYEE and INTERN groups should get EMPLOYEE role
        UserPrincipal user = UserPrincipal.builder()
            .email("multi@haiintel.com")
            .name("Multi Group User")
            .groups(List.of("employees@haiintel.com", "intern@haiintel.com"))
            .build();
        
        UserPrincipal result = roleResolver.resolveRole(user);
        
        assertEquals(Role.EMPLOYEE, result.getRole());
    }
}

