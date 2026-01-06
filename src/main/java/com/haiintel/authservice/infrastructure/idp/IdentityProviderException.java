package com.haiintel.authservice.infrastructure.idp;

/**
 * Exception thrown when identity provider operations fail.
 */
public class IdentityProviderException extends RuntimeException {
    
    public IdentityProviderException(String message) {
        super(message);
    }
    
    public IdentityProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}

