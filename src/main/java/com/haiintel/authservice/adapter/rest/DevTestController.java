package com.haiintel.authservice.adapter.rest;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.haiintel.authservice.adapter.rest.dto.LoginResponse;
import com.haiintel.authservice.adapter.rest.dto.UserInfoResponse;
import com.haiintel.authservice.domain.model.JwtToken;
import com.haiintel.authservice.domain.model.UserPrincipal;
import com.haiintel.authservice.domain.port.AuditLogger;
import com.haiintel.authservice.domain.service.JwtService;
import com.haiintel.authservice.domain.service.RoleResolver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Development/Test controller for testing authentication flow without real
 * Google OAuth.
 *
 * ⚠️ WARNING: This controller is ONLY enabled in development/local
 * environments.
 * It bypasses real authentication and should NEVER be enabled in production.
 *
 * Enable with: DEV_TEST_ENDPOINTS_ENABLED=true
 */
@RestController
@RequestMapping("/api/v1/dev/test")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Development Testing", description = "Development-only test endpoints (NOT FOR PRODUCTION)")
@ConditionalOnProperty(name = "dev.test.endpoints.enabled", havingValue = "true", matchIfMissing = false)
public class DevTestController {

    private final JwtService jwtService;
    private final RoleResolver roleResolver;
    private final AuditLogger auditLogger;

    @PostMapping("/login/{email}")
    @Operation(summary = "[DEV ONLY] Simulate login for testing", description = "Generate JWT token for a test user without real OAuth. FOR DEVELOPMENT ONLY!")
    public ResponseEntity<LoginResponse> testLogin(
            @PathVariable String email,
            @RequestParam(required = false, defaultValue = "admin@haiintel.com") String group,
            HttpServletRequest httpRequest) {

        log.warn("⚠️ DEV TEST ENDPOINT CALLED: Simulating login for {}", email);

        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // Create test user with specified groups
        List<String> groups = Arrays.asList(group.split(","));
        UserPrincipal user = UserPrincipal.builder()
                .email(email)
                .name(extractNameFromEmail(email))
                .groups(groups)
                .domain("haiintel.com")
                .build();

        // Resolve role from groups
        UserPrincipal enrichedUser = roleResolver.resolveRole(user);
        log.info("Test user role resolved: {} -> {}", enrichedUser.getEmail(), enrichedUser.getRole());

        // Issue JWT token
        JwtToken token = jwtService.issueToken(enrichedUser);
        log.info("Test JWT token issued: jti={}, email={}, role={}",
                token.getJti(), enrichedUser.getEmail(), enrichedUser.getRole());

        // Audit log
        auditLogger.logAuthentication(enrichedUser.getEmail(), ipAddress, userAgent);
        auditLogger.logTokenIssued(token.getJti(), enrichedUser.getEmail(),
                enrichedUser.getRole(), ipAddress);

        LoginResponse response = LoginResponse.builder()
                .accessToken(token.getToken())
                .tokenType("Bearer")
                .expiresIn(3600) // 1 hour in seconds
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user-info/{email}")
    @Operation(summary = "[DEV ONLY] Get test user info", description = "Get user info for a test user. FOR DEVELOPMENT ONLY!")
    public ResponseEntity<UserInfoResponse> getTestUserInfo(
            @PathVariable String email,
            @RequestParam(required = false, defaultValue = "admin@haiintel.com") String group) {

        log.warn("⚠️ DEV TEST ENDPOINT CALLED: Getting user info for {}", email);

        // Create test user with specified groups
        List<String> groups = Arrays.asList(group.split(","));
        UserPrincipal user = UserPrincipal.builder()
                .email(email)
                .name(extractNameFromEmail(email))
                .groups(groups)
                .domain("haiintel.com")
                .build();

        // Resolve role from groups
        UserPrincipal enrichedUser = roleResolver.resolveRole(user);

        UserInfoResponse response = UserInfoResponse.builder()
                .email(enrichedUser.getEmail())
                .name(enrichedUser.getName())
                .role(enrichedUser.getRole().name())
                .groups(enrichedUser.getGroups())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/roles")
    @Operation(summary = "[DEV ONLY] List available roles and groups", description = "List all configured role mappings. FOR DEVELOPMENT ONLY!")
    public ResponseEntity<String> listRoles() {
        return ResponseEntity.ok(
                "Available roles:\n" +
                        "- ADMIN (group: admin@haiintel.com)\n" +
                        "- EMPLOYEE (group: employees@haiintel.com)\n" +
                        "- INTERN (group: intern@haiintel.com)\n\n" +
                        "Test examples:\n" +
                        "- POST /api/v1/dev/test/login/pattabhi@haiintel.com?group=admin@haiintel.com\n" +
                        "- POST /api/v1/dev/test/login/john@haiintel.com?group=employees@haiintel.com\n" +
                        "- POST /api/v1/dev/test/login/jane@haiintel.com?group=intern@haiintel.com\n" +
                        "- POST /api/v1/dev/test/login/multi@haiintel.com?group=admin@haiintel.com,employees@haiintel.com");
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

    private String extractNameFromEmail(String email) {
        String localPart = email.split("@")[0];
        return Arrays.stream(localPart.split("[._]"))
                .map(part -> part.substring(0, 1).toUpperCase() + part.substring(1))
                .reduce((a, b) -> a + " " + b)
                .orElse(localPart);
    }
}
