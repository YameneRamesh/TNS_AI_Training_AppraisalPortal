package com.tns.appraisal.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service for password hashing operations using BCrypt.
 * Provides methods to hash plain text passwords and verify password strength.
 */
@Service
public class PasswordHashingService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordHashingService.class);
    
    private final PasswordEncoder passwordEncoder;

    public PasswordHashingService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Hash a plain text password using BCrypt.
     *
     * @param plainPassword the plain text password to hash
     * @return BCrypt hashed password
     * @throws IllegalArgumentException if password is null or empty
     */
    public String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        logger.debug("Hashing password");
        String hashedPassword = passwordEncoder.encode(plainPassword);
        logger.debug("Password hashed successfully");
        
        return hashedPassword;
    }

    /**
     * Verify if a plain text password matches a hashed password.
     *
     * @param plainPassword the plain text password to verify
     * @param hashedPassword the BCrypt hashed password to compare against
     * @return true if passwords match, false otherwise
     */
    public boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }

        return passwordEncoder.matches(plainPassword, hashedPassword);
    }

    /**
     * Validate password strength requirements.
     * Current requirements:
     * - Minimum 8 characters
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one digit
     *
     * @param password the password to validate
     * @return true if password meets requirements, false otherwise
     */
    public boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowercase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);

        return hasUppercase && hasLowercase && hasDigit;
    }

    /**
     * Get password strength validation error message.
     *
     * @param password the password to validate
     * @return error message if password is weak, null if password is strong
     */
    public String getPasswordStrengthError(String password) {
        if (password == null || password.isEmpty()) {
            return "Password is required";
        }

        if (password.length() < 8) {
            return "Password must be at least 8 characters long";
        }

        if (!password.chars().anyMatch(Character::isUpperCase)) {
            return "Password must contain at least one uppercase letter";
        }

        if (!password.chars().anyMatch(Character::isLowerCase)) {
            return "Password must contain at least one lowercase letter";
        }

        if (!password.chars().anyMatch(Character::isDigit)) {
            return "Password must contain at least one digit";
        }

        return null; // Password is strong
    }
}
