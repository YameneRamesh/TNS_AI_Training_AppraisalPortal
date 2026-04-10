package com.tns.appraisal.user.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * Request DTO for assigning roles to a user.
 */
public class AssignRolesRequest {

    @NotNull(message = "Roles are required")
    private Set<String> roles;

    // Constructors
    public AssignRolesRequest() {
    }

    public AssignRolesRequest(Set<String> roles) {
        this.roles = roles;
    }

    // Getters and Setters
    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}
