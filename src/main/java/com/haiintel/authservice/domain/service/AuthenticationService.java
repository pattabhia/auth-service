package com.haiintel.authservice.domain.service;

import com.haiintel.authservice.domain.model.JwtToken;
import com.haiintel.authservice.domain.model.UserPrincipal;
import com.haiintel.authservice.domain.port.AuditLogger;
import com.haiintel.authservice.domain.port.IdentityProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Domain service for authentication operations.
 * Orchestrates identity provider and JWT token issuance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {
    
    private final IdentityProvider identityProvider;
    private final JwtService jwtService;
    private final RoleResolver roleResolver;
    private final AuditLogger auditLogger;
    
    /**
     * Authenticate user with OAuth 2.0 authorization code and issue JWT token.
     * 
     * @param authorizationCode OAuth 2.0 authorization code
     * @param ipAddress Client IP address
     * @param userAgent Client user agent
     * @return JWT token
     */
    public JwtToken authenticate(String authorizationCode, String ipAddress, String userAgent) {
        log.debug("Authenticating user with authorization code");
        
        try {
            // Step 1: Authenticate with identity provider
            UserPrincipal user = identityProvider.authenticate(authorizationCode);
            log.info("User authenticated: {}", user.getEmail());
            
            // Step 2: Resolve user role from groups
            UserPrincipal enrichedUser = roleResolver.resolveRole(user);
            log.info("User role resolved: {} -> {}", enrichedUser.getEmail(), enrichedUser.getRole());
            
            // Step 3: Issue JWT token
            JwtToken token = jwtService.issueToken(enrichedUser);
            log.info("JWT token issued: jti={}, email={}, role={}", 
                token.getJti(), enrichedUser.getEmail(), enrichedUser.getRole());
            
            // Step 4: Audit log
            auditLogger.logAuthentication(enrichedUser.getEmail(), ipAddress, userAgent);
            auditLogger.logTokenIssued(token.getJti(), enrichedUser.getEmail(), 
                enrichedUser.getRole(), ipAddress);
            
            return token;
            
        } catch (Exception e) {
            log.error("Authentication failed", e);
            auditLogger.logAuthenticationFailure("unknown", ipAddress, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Validate JWT token and return user principal.
     * 
     * @param token JWT token string
     * @return User principal
     */
    public UserPrincipal validateToken(String token) {
        UserPrincipal user = jwtService.validateToken(token);
        auditLogger.logTokenValidated(jwtService.extractJti(token), user.getEmail());
        return user;
    }
}

