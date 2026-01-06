package com.haiintel.authservice.adapter.rest;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.haiintel.authservice.adapter.rest.dto.LoginRequest;
import com.haiintel.authservice.adapter.rest.dto.LoginResponse;
import com.haiintel.authservice.adapter.rest.dto.UserInfoResponse;
import com.haiintel.authservice.domain.model.JwtToken;
import com.haiintel.authservice.domain.model.UserPrincipal;
import com.haiintel.authservice.domain.service.AuthenticationService;
import com.haiintel.authservice.infrastructure.config.GoogleWorkspaceProperties;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for authentication endpoints.
 *
 * Endpoints:
 * - GET /api/v1/auth/login/google - Initiate Google OAuth flow
 * - GET /api/v1/auth/callback/google - OAuth callback endpoint
 * - POST /api/v1/auth/google/login - Authenticate with Google OAuth code
 * - GET /api/v1/auth/me - Get current user info
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final GoogleWorkspaceProperties googleWorkspaceProperties;

    /**
     * Initiate Google OAuth flow.
     * Redirects user to Google's OAuth consent screen.
     */
    @GetMapping("/login/google")
    @Operation(summary = "Initiate Google OAuth", description = "Redirects to Google OAuth consent screen to start authentication flow")
    public void initiateGoogleLogin(HttpServletResponse response) throws IOException {
        String clientId = googleWorkspaceProperties.getClientId();
        String redirectUri = googleWorkspaceProperties.getRedirectUri();
        String scope = "openid email profile https://www.googleapis.com/auth/admin.directory.group.readonly";

        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8) +
                "&access_type=offline" +
                "&prompt=consent";

        log.info("Initiating Google OAuth flow, redirecting to: {}", authUrl);
        response.sendRedirect(authUrl);
    }

    /**
     * OAuth callback endpoint.
     * Receives authorization code from Google and exchanges it for JWT token.
     */
    @GetMapping("/callback/google")
    @Operation(summary = "Google OAuth Callback", description = "Handles OAuth callback from Google and returns JWT token")
    public ResponseEntity<LoginResponse> handleGoogleCallback(
            @RequestParam("code") String authorizationCode,
            @RequestParam(value = "error", required = false) String error,
            HttpServletRequest httpRequest) {

        if (error != null) {
            log.error("OAuth error: {}", error);
            throw new RuntimeException("OAuth authentication failed: " + error);
        }

        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        log.info("OAuth callback received from IP: {}, exchanging code for token", ipAddress);

        JwtToken token = authenticationService.authenticate(
                authorizationCode,
                ipAddress,
                userAgent);

        LoginResponse response = LoginResponse.builder()
                .accessToken(token.getToken())
                .tokenType("Bearer")
                .expiresIn(3600) // 1 hour in seconds
                .build();

        log.info("OAuth flow completed successfully, JWT issued");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google/login")
    @Operation(summary = "Login with Google OAuth", description = "Exchange Google OAuth authorization code for JWT token")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        log.info("Login request from IP: {}", ipAddress);

        JwtToken token = authenticationService.authenticate(
                request.getAuthorizationCode(),
                ipAddress,
                userAgent);

        LoginResponse response = LoginResponse.builder()
                .accessToken(token.getToken())
                .tokenType("Bearer")
                .expiresIn(3600) // 1 hour in seconds
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user info", description = "Get authenticated user information from JWT token")
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
