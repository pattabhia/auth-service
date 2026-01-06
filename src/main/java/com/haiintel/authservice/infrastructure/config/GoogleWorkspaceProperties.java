package com.haiintel.authservice.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Google Workspace configuration properties.
 */
@Data
@Component
@ConfigurationProperties(prefix = "google.workspace")
public class GoogleWorkspaceProperties {
    private String serviceAccountFile;
    private String delegatedAdmin;
    private String domain;
    private List<String> scopes = new ArrayList<>();
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    
    private CacheConfig cache = new CacheConfig();
    
    @Data
    public static class CacheConfig {
        private boolean enabled = true;
        private int ttlMinutes = 5;
        private int maxSize = 1000;
    }
}

