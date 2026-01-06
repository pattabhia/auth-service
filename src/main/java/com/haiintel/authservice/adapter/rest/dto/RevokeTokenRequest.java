package com.haiintel.authservice.adapter.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

/**
 * Revoke token request DTO.
 */
@Data
public class RevokeTokenRequest {
    
    @NotBlank(message = "JTI is required")
    private String jti;
    
    @NotBlank(message = "Email is required")
    private String email;
    
    @NotNull(message = "Expiration time is required")
    private Instant expiresAt;
    
    private String reason;
}

