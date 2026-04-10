package com.tns.appraisal.auth;

import com.tns.appraisal.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify BCrypt password hashing is properly configured
 * and working end-to-end in the Spring Boot application context.
 */
@SpringBootTest(classes = {SecurityConfig.class, PasswordHashingService.class})
class BCryptIntegrationTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordHashingService passwordHashingService;

    @Test
    @DisplayName("BCryptPasswordEncoder bean should be properly configured")
    void testBCryptPasswordEncoderBean() {
        assertNotNull(passwordEncoder, "PasswordEncoder bean should be available");
        assertEquals("org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder",
            passwordEncoder.getClass().getName(),
            "PasswordEncoder should be BCryptPasswordEncoder");
    }

    @Test
    @DisplayName("PasswordHashingService should use BCrypt for hashing")
    void testPasswordHashingServiceUsesBCrypt() {
        String plainPassword = "TestPassword123";
        String hashedPassword = passwordHashingService.hashPassword(plainPassword);

        assertNotNull(hashedPassword, "Hashed password should not be null");
        assertTrue(hashedPassword.startsWith("$2a$") || hashedPassword.startsWith("$2b$"),
            "BCrypt hash should start with $2a$ or $2b$");
        assertEquals(60, hashedPassword.length(),
            "BCrypt hash should be 60 characters long");
    }

    @Test
    @DisplayName("Password verification should work with BCrypt")
    void testPasswordVerificationWithBCrypt() {
        String plainPassword = "SecurePass456";
        String hashedPassword = passwordHashingService.hashPassword(plainPassword);

        assertTrue(passwordHashingService.verifyPassword(plainPassword, hashedPassword),
            "Correct password should verify successfully");
        assertFalse(passwordHashingService.verifyPassword("WrongPassword", hashedPassword),
            "Incorrect password should fail verification");
    }

    @Test
    @DisplayName("BCrypt should generate different hashes for same password")
    void testBCryptGeneratesDifferentSalts() {
        String plainPassword = "SamePassword789";
        String hash1 = passwordHashingService.hashPassword(plainPassword);
        String hash2 = passwordHashingService.hashPassword(plainPassword);

        assertNotEquals(hash1, hash2,
            "BCrypt should generate different hashes due to random salt");
        assertTrue(passwordHashingService.verifyPassword(plainPassword, hash1),
            "First hash should verify correctly");
        assertTrue(passwordHashingService.verifyPassword(plainPassword, hash2),
            "Second hash should verify correctly");
    }

    @Test
    @DisplayName("PasswordEncoder and PasswordHashingService should be compatible")
    void testPasswordEncoderAndServiceCompatibility() {
        String plainPassword = "CompatibilityTest123";

        // Hash with PasswordHashingService
        String hashFromService = passwordHashingService.hashPassword(plainPassword);

        // Verify with PasswordEncoder directly
        assertTrue(passwordEncoder.matches(plainPassword, hashFromService),
            "PasswordEncoder should verify hash created by PasswordHashingService");

        // Hash with PasswordEncoder directly
        String hashFromEncoder = passwordEncoder.encode(plainPassword);

        // Verify with PasswordHashingService
        assertTrue(passwordHashingService.verifyPassword(plainPassword, hashFromEncoder),
            "PasswordHashingService should verify hash created by PasswordEncoder");
    }
}
