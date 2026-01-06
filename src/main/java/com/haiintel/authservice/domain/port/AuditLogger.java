package com.haiintel.authservice.domain.port;

import com.haiintel.authservice.domain.model.Role;

import java.util.Map;

/**
 * Port interface for audit logging (Hexagonal Architecture).
 * 
 * Implementations:
 * - LogbackAuditLogger (current - file-based)
 * - AzureMonitorAuditLogger (future - Azure Monitor)
 * 
 * ✅ P0 FIX: Structured audit logging for compliance (SOC 2, ISO 27001)
 * ✅ P2 FIX: Hexagonal architecture for logging abstraction
 */
public interface AuditLogger {
    
    /**
     * Log successful authentication.
     */
    void logAuthentication(String email, String ipAddress, String userAgent);
    
    /**
     * Log failed authentication attempt.
     */
    void logAuthenticationFailure(String email, String ipAddress, String reason);
    
    /**
     * Log JWT token issuance.
     */
    void logTokenIssued(String jti, String email, Role role, String ipAddress);
    
    /**
     * Log JWT token validation.
     */
    void logTokenValidated(String jti, String email);
    
    /**
     * Log authorization failure (valid token, insufficient permissions).
     */
    void logAuthorizationFailure(String email, String resource, String requiredRole);
    
    /**
     * Log token revocation.
     */
    void logTokenRevoked(String jti, String email, String revokedBy, String reason);
    
    /**
     * Log user tokens revocation (all tokens for a user).
     */
    void logUserTokensRevoked(String email, String revokedBy, String reason);
    
    /**
     * Log administrative action.
     */
    void logAdminAction(String adminEmail, String action, Map<String, Object> details);
    
    /**
     * Log security event (e.g., rate limit exceeded, circuit breaker opened).
     */
    void logSecurityEvent(String eventType, String email, Map<String, Object> details);
}

