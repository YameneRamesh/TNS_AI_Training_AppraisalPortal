package com.tns.appraisal.auth.dto;

import java.util.Set;

/**
 * DTO for user profile response.
 * Contains basic user information displayed on the home screen.
 */
public class UserProfileResponse {

    private Long id;
    private String employeeId;
    private String fullName;
    private String email;
    private String designation;
    private String department;
    private String managerName;
    private Set<String> roles;

    public UserProfileResponse() {
    }

    public UserProfileResponse(Long id, String employeeId, String fullName, String email,
                              String designation, String department, String managerName, Set<String> roles) {
        this.id = id;
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.email = email;
        this.designation = designation;
        this.department = department;
        this.managerName = managerName;
        this.roles = roles;
    }

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

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}
