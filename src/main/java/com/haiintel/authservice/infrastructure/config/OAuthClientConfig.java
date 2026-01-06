package com.haiintel.authservice.infrastructure.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;

/**
 * Configuration loader for OAuth client credentials from secrets file.
 * Reads oauth-client.json and populates GoogleWorkspaceProperties.
 */
@Configuration
@Slf4j
public class OAuthClientConfig {

    @Value("${GOOGLE_OAUTH_CLIENT_FILE:secrets/oauth-client.json}")
    private String oauthClientFile;

    private final GoogleWorkspaceProperties googleWorkspaceProperties;
    private final ObjectMapper objectMapper;

    public OAuthClientConfig(GoogleWorkspaceProperties googleWorkspaceProperties) {
        this.googleWorkspaceProperties = googleWorkspaceProperties;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void loadOAuthClientCredentials() {
        try {
            File file = new File(oauthClientFile);
            if (!file.exists()) {
                log.warn("OAuth client file not found: {}. Using environment variables or defaults.", oauthClientFile);
                return;
            }

            log.info("Loading OAuth client credentials from: {}", oauthClientFile);
            JsonNode root = objectMapper.readTree(file);
            JsonNode web = root.get("web");

            if (web == null) {
                log.error("Invalid oauth-client.json format: missing 'web' section");
                return;
            }

            // Only override if not already set via environment variables
            if (googleWorkspaceProperties.getClientId() == null || 
                googleWorkspaceProperties.getClientId().isEmpty()) {
                String clientId = web.get("client_id").asText();
                googleWorkspaceProperties.setClientId(clientId);
                log.info("Loaded client_id from secrets file");
            }

            if (googleWorkspaceProperties.getClientSecret() == null || 
                googleWorkspaceProperties.getClientSecret().isEmpty()) {
                String clientSecret = web.get("client_secret").asText();
                googleWorkspaceProperties.setClientSecret(clientSecret);
                log.info("Loaded client_secret from secrets file");
            }

            if (googleWorkspaceProperties.getRedirectUri() == null || 
                googleWorkspaceProperties.getRedirectUri().isEmpty()) {
                JsonNode redirectUris = web.get("redirect_uris");
                if (redirectUris != null && redirectUris.isArray() && redirectUris.size() > 0) {
                    String redirectUri = redirectUris.get(0).asText();
                    googleWorkspaceProperties.setRedirectUri(redirectUri);
                    log.info("Loaded redirect_uri from secrets file: {}", redirectUri);
                }
            }

            log.info("OAuth client credentials loaded successfully");
            log.debug("Client ID: {}", maskClientId(googleWorkspaceProperties.getClientId()));
            log.debug("Redirect URI: {}", googleWorkspaceProperties.getRedirectUri());

        } catch (IOException e) {
            log.error("Failed to load OAuth client credentials from {}: {}", oauthClientFile, e.getMessage());
            log.warn("Falling back to environment variables or defaults");
        }
    }

    private String maskClientId(String clientId) {
        if (clientId == null || clientId.length() < 20) {
            return "***";
        }
        return clientId.substring(0, 10) + "..." + clientId.substring(clientId.length() - 10);
    }
}

