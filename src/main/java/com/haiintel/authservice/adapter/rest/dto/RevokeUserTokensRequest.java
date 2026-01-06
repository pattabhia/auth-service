package com.haiintel.authservice.adapter.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Revoke user tokens request DTO.
 */
@Data
public class RevokeUserTokensRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    private String reason;
}

