package com.haiintel.authservice.adapter.rest;

import com.haiintel.authservice.adapter.rest.dto.RevokeTokenRequest;
import com.haiintel.authservice.adapter.rest.dto.RevokeUserTokensRequest;
import com.haiintel.authservice.domain.model.UserPrincipal;
import com.haiintel.authservice.domain.port.AuditLogger;
import com.haiintel.authservice.domain.port.TokenRevocationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * REST controller for administrative operations.
 * 
 * ✅ P0 FIX: Token revocation endpoints
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "Administrative endpoints (ADMIN role required)")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {
    
    private final TokenRevocationRepository tokenRevocationRepository;
    private final AuditLogger auditLogger;
    
    @PostMapping("/revoke-token")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Revoke a specific token", 
               description = "Revoke a JWT token by its JTI (ADMIN only)")
    public ResponseEntity<Map<String, String>> revokeToken(
            @Valid @RequestBody RevokeTokenRequest request,
            @AuthenticationPrincipal UserPrincipal admin) {
        
        log.info("Admin {} revoking token: jti={}", admin.getEmail(), request.getJti());
        
        tokenRevocationRepository.revokeToken(
            request.getJti(), 
            request.getExpiresAt()
        );
        
        auditLogger.logTokenRevoked(
            request.getJti(), 
            request.getEmail(), 
            admin.getEmail(), 
            request.getReason()
        );
        
        return ResponseEntity.ok(Map.of(
            "message", "Token revoked successfully",
            "jti", request.getJti()
        ));
    }
    
    @PostMapping("/revoke-user-tokens")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Revoke all tokens for a user", 
               description = "Revoke all JWT tokens for a specific user (ADMIN only)")
    public ResponseEntity<Map<String, String>> revokeUserTokens(
            @Valid @RequestBody RevokeUserTokensRequest request,
            @AuthenticationPrincipal UserPrincipal admin) {
        
        log.info("Admin {} revoking all tokens for user: {}", admin.getEmail(), request.getEmail());
        
        Instant now = Instant.now();
        tokenRevocationRepository.revokeAllUserTokens(request.getEmail(), now);
        
        auditLogger.logUserTokensRevoked(
            request.getEmail(), 
            admin.getEmail(), 
            request.getReason()
        );
        
        return ResponseEntity.ok(Map.of(
            "message", "All user tokens revoked successfully",
            "email", request.getEmail(),
            "revokedBefore", now.toString()
        ));
    }
}

