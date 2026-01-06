package com.haiintel.authservice.adapter.rest.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Login request DTO.
 */
@Data
public class LoginRequest {
    
    @NotBlank(message = "Authorization code is required")
    private String authorizationCode;
}

