package com.tns.appraisal.user.dto;

import com.tns.appraisal.auth.Role;
import com.tns.appraisal.user.User;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Response DTO for user information.
 */
public class UserResponse {

    private Long id;
    private String employeeId;
    private String fullName;
    private String email;
    private String designation;
    private String department;
    private Long managerId;
    private String managerName;
    private Boolean isActive;
    private Set<String> roles;
    private Instant createdAt;
    private Instant updatedAt;

    // Constructors
    public UserResponse() {
    }

    public UserResponse(User user) {
        this.id = user.getId();
        this.employeeId = user.getEmployeeId();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        this.designation = user.getDesignation();
        this.department = user.getDepartment();
        this.managerId = user.getManager() != null ? user.getManager().getId() : null;
        this.managerName = user.getManager() != null ? user.getManager().getFullName() : null;
        this.isActive = user.getIsActive();
        this.roles = user.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toSet());
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
