package com.haiintel.authservice.infrastructure.logging;

import com.haiintel.authservice.domain.model.Role;
import com.haiintel.authservice.domain.port.AuditLogger;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Logback-based implementation of audit logger.
 * Logs to separate audit log file with structured JSON format.
 * 
 * ✅ P0 FIX: Structured audit logging for compliance (SOC 2, ISO 27001)
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "audit.enabled", havingValue = "true", matchIfMissing = true)
public class LogbackAuditLogger implements AuditLogger {
    
    private static final org.slf4j.Logger AUDIT_LOG = 
        org.slf4j.LoggerFactory.getLogger("com.haiintel.authservice.infrastructure.logging.AuditLogger");
    
    @Override
    public void logAuthentication(String email, String ipAddress, String userAgent) {
        Map<String, Object> event = createBaseEvent("AUTHENTICATION_SUCCESS");
        event.put("email", email);
        event.put("ipAddress", ipAddress);
        event.put("userAgent", userAgent);
        
        logAuditEvent(event);
    }
    
    @Override
    public void logAuthenticationFailure(String email, String ipAddress, String reason) {
        Map<String, Object> event = createBaseEvent("AUTHENTICATION_FAILURE");
        event.put("email", email);
        event.put("ipAddress", ipAddress);
        event.put("reason", reason);
        
        logAuditEvent(event);
    }
    
    @Override
    public void logTokenIssued(String jti, String email, Role role, String ipAddress) {
        Map<String, Object> event = createBaseEvent("TOKEN_ISSUED");
        event.put("jti", jti);
        event.put("email", email);
        event.put("role", role.name());
        event.put("ipAddress", ipAddress);
        
        logAuditEvent(event);
    }
    
    @Override
    public void logTokenValidated(String jti, String email) {
        Map<String, Object> event = createBaseEvent("TOKEN_VALIDATED");
        event.put("jti", jti);
        event.put("email", email);
        
        logAuditEvent(event);
    }
    
    @Override
    public void logAuthorizationFailure(String email, String resource, String requiredRole) {
        Map<String, Object> event = createBaseEvent("AUTHORIZATION_FAILURE");
        event.put("email", email);
        event.put("resource", resource);
        event.put("requiredRole", requiredRole);
        
        logAuditEvent(event);
    }
    
    @Override
    public void logTokenRevoked(String jti, String email, String revokedBy, String reason) {
        Map<String, Object> event = createBaseEvent("TOKEN_REVOKED");
        event.put("jti", jti);
        event.put("email", email);
        event.put("revokedBy", revokedBy);
        event.put("reason", reason);
        
        logAuditEvent(event);
    }
    
    @Override
    public void logUserTokensRevoked(String email, String revokedBy, String reason) {
        Map<String, Object> event = createBaseEvent("USER_TOKENS_REVOKED");
        event.put("email", email);
        event.put("revokedBy", revokedBy);
        event.put("reason", reason);
        
        logAuditEvent(event);
    }
    
    @Override
    public void logAdminAction(String adminEmail, String action, Map<String, Object> details) {
        Map<String, Object> event = createBaseEvent("ADMIN_ACTION");
        event.put("adminEmail", adminEmail);
        event.put("action", action);
        event.put("details", details);
        
        logAuditEvent(event);
    }
    
    @Override
    public void logSecurityEvent(String eventType, String email, Map<String, Object> details) {
        Map<String, Object> event = createBaseEvent("SECURITY_EVENT");
        event.put("eventType", eventType);
        event.put("email", email);
        event.put("details", details);
        
        logAuditEvent(event);
    }
    
    private Map<String, Object> createBaseEvent(String eventType) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("timestamp", Instant.now().toString());
        event.put("requestId", MDC.get("requestId"));
        return event;
    }
    
    private void logAuditEvent(Map<String, Object> event) {
        // Add event to MDC for structured logging
        event.forEach((key, value) -> {
            if (value != null) {
                MDC.put(key, value.toString());
            }
        });
        
        try {
            AUDIT_LOG.info("Audit event: {}", event.get("eventType"));
        } finally {
            // Clean up MDC
            event.keySet().forEach(MDC::remove);
        }
    }
}

