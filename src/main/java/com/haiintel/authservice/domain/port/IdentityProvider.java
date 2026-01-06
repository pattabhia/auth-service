package com.haiintel.authservice.domain.port;

import com.haiintel.authservice.domain.model.UserPrincipal;

import java.util.List;

/**
 * Port interface for Identity Provider integration (Hexagonal Architecture).
 * 
 * Implementations:
 * - GoogleWorkspaceIdentityProvider (current)
 * - AzureADIdentityProvider (future)
 * - OktaIdentityProvider (future)
 * 
 * ✅ P2 FIX: Hexagonal architecture for IdP abstraction
 */
public interface IdentityProvider {
    
    /**
     * Authenticate user with OAuth 2.0 authorization code.
     * 
     * @param authorizationCode OAuth 2.0 authorization code
     * @return Authenticated user principal
     * @throws AuthenticationException if authentication fails
     */
    UserPrincipal authenticate(String authorizationCode);
    
    /**
     * Get user groups from the identity provider.
     * 
     * @param email User email
     * @return List of group emails the user belongs to
     * @throws IdentityProviderException if fetching groups fails
     */
    List<String> getUserGroups(String email);
    
    /**
     * Verify user exists and is active in the identity provider.
     * 
     * @param email User email
     * @return true if user exists and is active
     */
    boolean isUserActive(String email);
    
    /**
     * Get the identity provider name (e.g., "google", "azure", "okta").
     */
    String getProviderName();
}

