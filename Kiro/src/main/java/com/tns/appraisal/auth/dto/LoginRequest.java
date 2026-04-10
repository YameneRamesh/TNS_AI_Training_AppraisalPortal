package com.tns.appraisal.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for login request.
 */
public class LoginRequest {

    @NotBlank(message = "Login identifier is required")
    private String loginIdentifier;

    @NotBlank(message = "Password is required")
    private String password;

    public LoginRequest() {
    }

    public LoginRequest(String loginIdentifier, String password) {
        this.loginIdentifier = loginIdentifier;
        this.password = password;
    }

    public String getLoginIdentifier() {
        return loginIdentifier;
    }

    public void setLoginIdentifier(String loginIdentifier) {
        this.loginIdentifier = loginIdentifier;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
