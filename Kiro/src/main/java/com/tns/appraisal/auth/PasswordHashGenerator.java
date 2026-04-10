package com.tns.appraisal.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class for generating BCrypt password hashes.
 * This is useful for creating initial user accounts or generating hashes for database seed scripts.
 * 
 * Usage:
 * Run this class as a standalone Java application to generate BCrypt hashes for passwords.
 * The generated hash can then be used in SQL INSERT statements or user creation code.
 */
public class PasswordHashGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Example passwords to hash
        String[] passwords = {
            "Admin@123",
            "Hr@123",
            "Manager@123",
            "Employee@123"
        };
        
        System.out.println("BCrypt Password Hashes:");
        System.out.println("=".repeat(80));
        
        for (String password : passwords) {
            String hash = encoder.encode(password);
            System.out.println("Password: " + password);
            System.out.println("Hash:     " + hash);
            System.out.println("-".repeat(80));
        }
        
        System.out.println("\nNote: Each time you run this, BCrypt will generate a different hash");
        System.out.println("for the same password (due to random salt). All hashes are valid.");
    }
}
