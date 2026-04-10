package com.tns.appraisal.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PasswordHashingService.
 */
class PasswordHashingServiceTest {

    private PasswordHashingService passwordHashingService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        passwordHashingService = new PasswordHashingService(passwordEncoder);
    }

    @Test
    void testHashPassword_Success() {
        // Given
        String plainPassword = "TestPassword123";

        // When
        String hashedPassword = passwordHashingService.hashPassword(plainPassword);

        // Then
        assertNotNull(hashedPassword);
        assertNotEquals(plainPassword, hashedPassword);
        assertTrue(hashedPassword.startsWith("$2a$") || hashedPassword.startsWith("$2b$"));
        assertTrue(hashedPassword.length() >= 60); // BCrypt hashes are 60 characters
    }

    @Test
    void testHashPassword_NullPassword_ThrowsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            passwordHashingService.hashPassword(null);
        });
    }

    @Test
    void testHashPassword_EmptyPassword_ThrowsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            passwordHashingService.hashPassword("");
        });
    }

    @Test
    void testHashPassword_WhitespacePassword_ThrowsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            passwordHashingService.hashPassword("   ");
        });
    }

    @Test
    void testHashPassword_DifferentHashesForSamePassword() {
        // Given
        String plainPassword = "TestPassword123";

        // When
        String hash1 = passwordHashingService.hashPassword(plainPassword);
        String hash2 = passwordHashingService.hashPassword(plainPassword);

        // Then
        assertNotEquals(hash1, hash2); // BCrypt uses salt, so hashes differ
        assertTrue(passwordHashingService.verifyPassword(plainPassword, hash1));
        assertTrue(passwordHashingService.verifyPassword(plainPassword, hash2));
    }

    @Test
    void testVerifyPassword_CorrectPassword_ReturnsTrue() {
        // Given
        String plainPassword = "TestPassword123";
        String hashedPassword = passwordHashingService.hashPassword(plainPassword);

        // When
        boolean result = passwordHashingService.verifyPassword(plainPassword, hashedPassword);

        // Then
        assertTrue(result);
    }

    @Test
    void testVerifyPassword_IncorrectPassword_ReturnsFalse() {
        // Given
        String plainPassword = "TestPassword123";
        String wrongPassword = "WrongPassword456";
        String hashedPassword = passwordHashingService.hashPassword(plainPassword);

        // When
        boolean result = passwordHashingService.verifyPassword(wrongPassword, hashedPassword);

        // Then
        assertFalse(result);
    }

    @Test
    void testVerifyPassword_NullPlainPassword_ReturnsFalse() {
        // Given
        String hashedPassword = passwordHashingService.hashPassword("TestPassword123");

        // When
        boolean result = passwordHashingService.verifyPassword(null, hashedPassword);

        // Then
        assertFalse(result);
    }

    @Test
    void testVerifyPassword_NullHashedPassword_ReturnsFalse() {
        // When
        boolean result = passwordHashingService.verifyPassword("TestPassword123", null);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsPasswordStrong_ValidPassword_ReturnsTrue() {
        // Given
        String strongPassword = "TestPass123";

        // When
        boolean result = passwordHashingService.isPasswordStrong(strongPassword);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsPasswordStrong_TooShort_ReturnsFalse() {
        // Given
        String shortPassword = "Test12";

        // When
        boolean result = passwordHashingService.isPasswordStrong(shortPassword);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsPasswordStrong_NoUppercase_ReturnsFalse() {
        // Given
        String noUppercase = "testpass123";

        // When
        boolean result = passwordHashingService.isPasswordStrong(noUppercase);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsPasswordStrong_NoLowercase_ReturnsFalse() {
        // Given
        String noLowercase = "TESTPASS123";

        // When
        boolean result = passwordHashingService.isPasswordStrong(noLowercase);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsPasswordStrong_NoDigit_ReturnsFalse() {
        // Given
        String noDigit = "TestPassword";

        // When
        boolean result = passwordHashingService.isPasswordStrong(noDigit);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsPasswordStrong_NullPassword_ReturnsFalse() {
        // When
        boolean result = passwordHashingService.isPasswordStrong(null);

        // Then
        assertFalse(result);
    }

    @Test
    void testGetPasswordStrengthError_NullPassword() {
        // When
        String error = passwordHashingService.getPasswordStrengthError(null);

        // Then
        assertEquals("Password is required", error);
    }

    @Test
    void testGetPasswordStrengthError_EmptyPassword() {
        // When
        String error = passwordHashingService.getPasswordStrengthError("");

        // Then
        assertEquals("Password is required", error);
    }

    @Test
    void testGetPasswordStrengthError_TooShort() {
        // When
        String error = passwordHashingService.getPasswordStrengthError("Test12");

        // Then
        assertEquals("Password must be at least 8 characters long", error);
    }

    @Test
    void testGetPasswordStrengthError_NoUppercase() {
        // When
        String error = passwordHashingService.getPasswordStrengthError("testpass123");

        // Then
        assertEquals("Password must contain at least one uppercase letter", error);
    }

    @Test
    void testGetPasswordStrengthError_NoLowercase() {
        // When
        String error = passwordHashingService.getPasswordStrengthError("TESTPASS123");

        // Then
        assertEquals("Password must contain at least one lowercase letter", error);
    }

    @Test
    void testGetPasswordStrengthError_NoDigit() {
        // When
        String error = passwordHashingService.getPasswordStrengthError("TestPassword");

        // Then
        assertEquals("Password must contain at least one digit", error);
    }

    @Test
    void testGetPasswordStrengthError_StrongPassword() {
        // When
        String error = passwordHashingService.getPasswordStrengthError("TestPass123");

        // Then
        assertNull(error);
    }

    @Test
    void testHashPassword_SpecialCharacters() {
        // Given
        String passwordWithSpecialChars = "Test@Pass#123!";

        // When
        String hashedPassword = passwordHashingService.hashPassword(passwordWithSpecialChars);

        // Then
        assertNotNull(hashedPassword);
        assertTrue(passwordHashingService.verifyPassword(passwordWithSpecialChars, hashedPassword));
    }

    @Test
    void testHashPassword_LongPassword() {
        // Given
        String longPassword = "ThisIsAVeryLongPasswordWithManyCharacters123456789";

        // When
        String hashedPassword = passwordHashingService.hashPassword(longPassword);

        // Then
        assertNotNull(hashedPassword);
        assertTrue(passwordHashingService.verifyPassword(longPassword, hashedPassword));
    }
}
