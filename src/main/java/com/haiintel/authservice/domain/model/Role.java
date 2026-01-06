package com.haiintel.authservice.domain.model;

/**
 * User roles in the HAI-Indexer system.
 * 
 * Hierarchy (highest to lowest privilege):
 * 1. ADMIN - Full system access
 * 2. EMPLOYEE - Standard employee access
 * 3. INTERN - Limited access (fail-safe default)
 */
public enum Role {
    ADMIN(1),
    EMPLOYEE(2),
    INTERN(3);
    
    private final int priority;
    
    Role(int priority) {
        this.priority = priority;
    }
    
    public int getPriority() {
        return priority;
    }
    
    /**
     * Returns the role with higher privilege (lower priority number).
     */
    public Role max(Role other) {
        return this.priority < other.priority ? this : other;
    }
}

