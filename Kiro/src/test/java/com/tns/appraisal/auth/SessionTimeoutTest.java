package com.tns.appraisal.auth;

import com.tns.appraisal.audit.AuditLogService;
import com.tns.appraisal.user.User;
import com.tns.appraisal.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for session timeout handling in AuthService.
 */
@ExtendWith(MockitoExtension.class)
class SessionTimeoutTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, auditLogService);
    }

    @Test
    void testSessionTimeoutIsSetTo15Minutes() {
        // Arrange
        User user = createTestUser();
        when(userRepository.findByEmail("test@tns.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", user.getPasswordHash())).thenReturn(true);
        when(request.getSession(true)).thenReturn(session);
        when(session.getId()).thenReturn("test-session-id");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        authService.login("test@tns.com", "password", request);

        // Assert - verify session timeout is set to 15 minutes (900 seconds)
        verify(session).setMaxInactiveInterval(900);
    }

    @Test
    void testIsSessionValidReturnsTrueForActiveSession() {
        // Arrange
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("AUTHENTICATED_USER_ID")).thenReturn(1L);

        // Act
        boolean isValid = authService.isSessionValid(request);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testIsSessionValidReturnsFalseForNoSession() {
        // Arrange
        when(request.getSession(false)).thenReturn(null);

        // Act
        boolean isValid = authService.isSessionValid(request);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testIsSessionValidReturnsFalseForSessionWithoutUser() {
        // Arrange
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("AUTHENTICATED_USER_ID")).thenReturn(null);

        // Act
        boolean isValid = authService.isSessionValid(request);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testRefreshSessionResetsTimeout() {
        // Arrange
        when(request.getSession(false)).thenReturn(session);

        // Act
        authService.refreshSession(request);

        // Assert - verify timeout is reset to 15 minutes
        verify(session).setMaxInactiveInterval(900);
    }

    @Test
    void testRefreshSessionDoesNothingWhenNoSession() {
        // Arrange
        when(request.getSession(false)).thenReturn(null);

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> authService.refreshSession(request));
    }

    @Test
    void testLogoutInvalidatesSession() {
        // Arrange
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("AUTHENTICATED_USER_ID")).thenReturn(1L);
        when(session.getId()).thenReturn("session-123");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        authService.logout(request);

        // Assert
        verify(session).invalidate();
        verify(auditLogService).logAsync(eq(1L), eq("LOGOUT"), eq("User"), eq(1L), 
            any(), eq("127.0.0.1"));
    }

    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setEmployeeId("EMP001");
        user.setFullName("Test User");
        user.setEmail("test@tns.com");
        user.setPasswordHash("$2a$10$hashedpassword");
        user.setIsActive(true);
        return user;
    }
}
