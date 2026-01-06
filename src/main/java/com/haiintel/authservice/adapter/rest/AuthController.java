package com.haiintel.authservice.adapter.rest;

import com.haiintel.authservice.adapter.rest.dto.LoginRequest;
import com.haiintel.authservice.adapter.rest.dto.LoginResponse;
import com.haiintel.authservice.adapter.rest.dto.UserInfoResponse;
import com.haiintel.authservice.domain.model.JwtToken;
import com.haiintel.authservice.domain.model.UserPrincipal;
import com.haiintel.authservice.domain.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints.
 * 
 * Endpoints:
 * - POST /auth/google/login - Authenticate with Google OAuth code
 * - GET /auth/me - Get current user info
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {
    
    private final AuthenticationService authenticationService;
    
    @PostMapping("/google/login")
    @Operation(summary = "Login with Google OAuth", 
               description = "Exchange Google OAuth authorization code for JWT token")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        log.info("Login request from IP: {}", ipAddress);
        
        JwtToken token = authenticationService.authenticate(
            request.getAuthorizationCode(), 
            ipAddress, 
            userAgent
        );
        
        LoginResponse response = LoginResponse.builder()
            .accessToken(token.getToken())
            .tokenType("Bearer")
            .expiresIn(3600)  // 1 hour in seconds
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/me")
    @Operation(summary = "Get current user info", 
               description = "Get authenticated user information from JWT token")
    public ResponseEntity<UserInfoResponse> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal user) {
        
        log.debug("Get user info: {}", user.getEmail());
        
        UserInfoResponse response = UserInfoResponse.builder()
            .email(user.getEmail())
            .name(user.getName())
            .role(user.getRole().name())
            .groups(user.getGroups())
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}

