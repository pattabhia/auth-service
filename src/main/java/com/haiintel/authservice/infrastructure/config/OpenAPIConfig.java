package com.haiintel.authservice.infrastructure.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;

/**
 * OpenAPI/Swagger configuration for API documentation.
 *
 * Provides:
 * - Interactive API documentation at /swagger-ui.html
 * - Google OAuth 2.0 authentication support
 * - JWT Bearer token authentication support (fallback)
 * - API versioning and metadata
 */
@Configuration
@RequiredArgsConstructor
public class OpenAPIConfig {

        private final GoogleWorkspaceProperties googleWorkspaceProperties;

        @Bean
        public OpenAPI authServiceOpenAPI() {
                // Define security schemes
                final String oauth2SchemeName = "googleOAuth2";
                final String bearerSchemeName = "Manual JWT Token";

                return new OpenAPI()
                                .info(new Info()
                                                .title("HAI-Indexer Authentication Service API")
                                                .version("2.1.1")
                                                .description("""
                                                                Enterprise authentication and authorization service for HAI-Indexer.

                                                                ## Features
                                                                - Google Workspace OAuth 2.0 authentication
                                                                - JWT token issuance (RS256 with JWKS)
                                                                - Token revocation (Redis-based deny list)
                                                                - Role-based access control (ADMIN, EMPLOYEE, INTERN)
                                                                - Audit logging (SOC 2 / ISO 27001 compliant)
                                                                - Circuit breaker for Google Workspace API
                                                                - Rate limiting via Istio

                                                                ## Authentication Flow

                                                                **Choose ONE of the following authentication methods:**

                                                                ### Option 1: Google OAuth (Recommended) ⭐
                                                                1. Click the "Authorize" button above
                                                                2. Select **"googleOAuth2"** (OAuth 2.0)
                                                                3. Click "Authorize" to log in with Google
                                                                4. Grant permissions
                                                                5. You'll be authenticated automatically!

                                                                **OR**

                                                                ### Option 2: Manual JWT Token (Dev/Testing Only)
                                                                1. Use `/api/v1/dev/test/login/{email}` endpoint to get a JWT token (DEV ONLY)
                                                                2. Click "Authorize" and select **"Manual JWT Token"**
                                                                3. Paste the JWT token (without 'Bearer' prefix)
                                                                4. Test protected endpoints like `/api/v1/auth/me`

                                                                **Note:** Use Google OAuth for realistic testing. Use Manual JWT Token only for quick dev testing.
                                                                """))
                                .servers(List.of(
                                                new Server()
                                                                .url("http://localhost:8000")
                                                                .description("Local Development Server"),
                                                new Server()
                                                                .url("https://auth.haiintel.com")
                                                                .description("Production Server")))
                                // Add global security requirements (OAuth2 OR Bearer)
                                .addSecurityItem(new SecurityRequirement().addList(oauth2SchemeName))
                                .addSecurityItem(new SecurityRequirement().addList(bearerSchemeName))
                                // Define security schemes
                                .components(new Components()
                                                // Google OAuth 2.0 Authorization Code Flow
                                                .addSecuritySchemes(oauth2SchemeName,
                                                                new SecurityScheme()
                                                                                .type(SecurityScheme.Type.OAUTH2)
                                                                                .description("Google OAuth 2.0 Authorization Code Flow")
                                                                                .flows(new OAuthFlows()
                                                                                                .authorizationCode(
                                                                                                                new OAuthFlow()
                                                                                                                                .authorizationUrl(
                                                                                                                                                "http://localhost:8000/api/v1/auth/login/google")
                                                                                                                                .tokenUrl("http://localhost:8000/api/v1/auth/callback/google")
                                                                                                                                .scopes(new Scopes()
                                                                                                                                                .addString("openid",
                                                                                                                                                                "OpenID Connect")
                                                                                                                                                .addString("email",
                                                                                                                                                                "User email address")
                                                                                                                                                .addString("profile",
                                                                                                                                                                "User profile information")))))
                                                // Manual JWT Token (for dev/testing only)
                                                .addSecuritySchemes(bearerSchemeName,
                                                                new SecurityScheme()
                                                                                .name(bearerSchemeName)
                                                                                .type(SecurityScheme.Type.HTTP)
                                                                                .scheme("bearer")
                                                                                .bearerFormat("JWT")
                                                                                .description("""
                                                                                                **MANUAL PROCESS - For Dev/Testing Only**

                                                                                                Use this ONLY if you already have a JWT token.
                                                                                                For normal authentication, use "googleOAuth2" instead.

                                                                                                Enter your JWT token in the text input below (without 'Bearer' prefix).

                                                                                                Example: "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."

                                                                                                To get a test token:
                                                                                                1. Call POST /api/v1/dev/test/login/pattabhi@haiintel.com?group=admin@haiintel.com
                                                                                                2. Copy the 'accessToken' from the response
                                                                                                3. Paste it here
                                                                                                """)));
        }
}
