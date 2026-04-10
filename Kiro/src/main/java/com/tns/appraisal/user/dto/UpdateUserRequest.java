package com.tns.appraisal.user.dto;

import jakarta.validation.constraints.Email;

/**
 * Request DTO for updating an existing user.
 * All fields are optional - only provided fields will be updated.
 */
public class UpdateUserRequest {

    private String fullName;

    @Email(message = "Email must be valid")
    private String email;

    private String password;

    private String designation;

    private String department;

    private Long managerId;

    // Constructors
    public UpdateUserRequest() {
    }

    // Getters and Setters
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
}
