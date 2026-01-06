package com.haiintel.authservice.infrastructure.idp;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.directory.Directory;
import com.google.api.services.directory.model.Group;
import com.google.api.services.directory.model.Groups;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.haiintel.authservice.domain.model.UserPrincipal;
import com.haiintel.authservice.domain.port.IdentityProvider;
import com.haiintel.authservice.infrastructure.config.GoogleWorkspaceProperties;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Google Workspace implementation of Identity Provider.
 * 
 * ✅ P1 FIX: Resilience4j circuit breaker (was manual implementation)
 * ✅ P2 FIX: Hexagonal architecture (implements IdentityProvider port)
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "idp.provider", havingValue = "google", matchIfMissing = true)
public class GoogleWorkspaceIdentityProvider implements IdentityProvider {

    private final GoogleWorkspaceProperties properties;
    private Directory directoryService;

    @Override
    public UserPrincipal authenticate(String authorizationCode) {
        try {
            // Exchange authorization code for tokens
            GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    "https://oauth2.googleapis.com/token",
                    properties.getClientId(),
                    properties.getClientSecret(),
                    authorizationCode,
                    properties.getRedirectUri()).execute();

            // Get user info from ID token
            String email = tokenResponse.parseIdToken().getPayload().getEmail();
            String name = (String) tokenResponse.parseIdToken().getPayload().get("name");

            // Verify user domain
            if (!email.endsWith("@" + properties.getDomain())) {
                throw new AuthenticationException("User not in allowed domain: " + email);
            }

            // Get user groups
            List<String> groups = getUserGroups(email);

            log.info("User authenticated via Google Workspace: email={}, groups={}", email, groups);

            return UserPrincipal.builder()
                    .email(email)
                    .name(name)
                    .groups(groups)
                    .domain(properties.getDomain())
                    .build();

        } catch (Exception e) {
            log.error("Google Workspace authentication failed", e);
            throw new AuthenticationException("Authentication failed: " + e.getMessage(), e);
        }
    }

    @Override
    @CircuitBreaker(name = "googleWorkspace", fallbackMethod = "getUserGroupsFallback")
    @Cacheable(value = "userGroups", key = "#email", unless = "#result == null")
    public List<String> getUserGroups(String email) {
        try {
            Directory directory = getDirectoryService();

            Groups groups = directory.groups()
                    .list()
                    .setUserKey(email)
                    .setDomain(properties.getDomain())
                    .execute();

            if (groups.getGroups() == null) {
                log.warn("No groups found for user: {}", email);
                return new ArrayList<>();
            }

            List<String> groupEmails = groups.getGroups().stream()
                    .map(Group::getEmail)
                    .collect(Collectors.toList());

            log.debug("Fetched groups for user {}: {}", email, groupEmails);
            return groupEmails;

        } catch (Exception e) {
            log.error("Failed to fetch groups for user: {}", email, e);
            throw new IdentityProviderException("Failed to fetch user groups", e);
        }
    }

    /**
     * ✅ P1 FIX: Circuit breaker fallback - returns empty list instead of failing
     */
    private List<String> getUserGroupsFallback(String email, Exception e) {
        log.error("Circuit breaker fallback: Failed to fetch groups for user: {}", email, e);
        return new ArrayList<>(); // Fail-safe: empty groups -> INTERN role
    }

    @Override
    public boolean isUserActive(String email) {
        try {
            Directory directory = getDirectoryService();
            var user = directory.users().get(email).execute();
            return !user.getSuspended();
        } catch (Exception e) {
            log.error("Failed to check user status: {}", email, e);
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "google";
    }

    private Directory getDirectoryService() throws Exception {
        if (directoryService == null) {
            GoogleCredentials credentials = ServiceAccountCredentials
                    .fromStream(new FileInputStream(properties.getServiceAccountFile()))
                    .createScoped(properties.getScopes())
                    .createDelegated(properties.getDelegatedAdmin());

            directoryService = new Directory.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("HAI-Indexer Auth Service")
                    .build();
        }
        return directoryService;
    }
}
