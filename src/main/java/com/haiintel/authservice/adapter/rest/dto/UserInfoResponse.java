package com.haiintel.authservice.adapter.rest.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * User info response DTO.
 */
@Data
@Builder
public class UserInfoResponse {
    private String email;
    private String name;
    private String role;
    private List<String> groups;
}

