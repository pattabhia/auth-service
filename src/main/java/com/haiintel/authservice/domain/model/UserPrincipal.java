package com.haiintel.authservice.domain.model;

import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * Domain model representing an authenticated user.
 * Immutable value object.
 */
@Data
@Builder(toBuilder = true)
public class UserPrincipal {
    String email;
    String name;
    Role role;
    List<String> groups;
    String domain;

    /**
     * Returns the subject claim for JWT (email).
     */
    public String getSubject() {
        return email;
    }
}
