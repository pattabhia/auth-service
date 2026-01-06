package com.haiintel.authservice.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation.
 * 
 * Provides:
 * - Interactive API documentation at /swagger-ui.html
 * - JWT Bearer token authentication support
 * - API versioning and metadata
 */
@Configuration
public class OpenAPIConfig {
    
    @Bean
    public OpenAPI authServiceOpenAPI() {
        // Define security scheme for JWT Bearer authentication
        final String securitySchemeName = "bearerAuth";
        
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
                    1. Use `/dev/test/login/{email}` endpoint to get a JWT token (DEV ONLY)
                    2. Click the "Authorize" button and enter: `Bearer <your-jwt-token>`
                    3. Test protected endpoints like `/auth/me`
                    
                    ## Production Authentication
                    In production, use `/auth/google/login` with a real Google OAuth authorization code.
                    """)
                .contact(new Contact()
                    .name("HAI-Intel Team")
                    .email("support@haiintel.com")
                    .url("https://haiintel.com"))
                .license(new License()
                    .name("Proprietary")
                    .url("https://haiintel.com/license")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Local Development Server"),
                new Server()
                    .url("https://auth.haiintel.com")
                    .description("Production Server")
            ))
            // Add global security requirement
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            // Define security schemes
            .components(new Components()
                .addSecuritySchemes(securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("""
                            JWT Authorization header using the Bearer scheme.
                            
                            Enter your JWT token in the text input below.
                            
                            Example: "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
                            
                            To get a test token:
                            1. Call POST /dev/test/login/steve@haiintel.com?group=admin@haiintel.com
                            2. Copy the 'accessToken' from the response
                            3. Paste it here (without 'Bearer' prefix)
                            """)));
    }
}

