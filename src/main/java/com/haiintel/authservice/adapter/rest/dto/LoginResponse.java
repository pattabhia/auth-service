package com.haiintel.authservice.adapter.rest.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Login response DTO.
 */
@Data
@Builder
public class LoginResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;  // seconds
}

