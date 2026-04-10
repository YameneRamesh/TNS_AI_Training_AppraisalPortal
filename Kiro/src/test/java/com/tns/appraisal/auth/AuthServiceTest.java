package com.tns.appraisal.auth;

import com.tns.appraisal.audit.AuditLogService;
import com.tns.appraisal.exception.InvalidCredentialsException;
import com.tns.appraisal.user.User;
import com.tns.appraisal.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

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

    @InjectMocks
    private AuthService authService;

    private User activeUser;
    private User inactiveUser;
    private Role employeeRole;

    @BeforeEach
    void setUp() {
        employeeRole = new Role("EMPLOYEE");
        employeeRole.setId(1);

        activeUser = new User("EMP001", "John Doe", "john.doe@tns.com", "$2a$10$hashedPassword");
        activeUser.setId(1L);
        activeUser.setIsActive(true);
        activeUser.addRole(employeeRole);

        inactiveUser = new User("EMP002", "Jane Smith", "jane.smith@tns.com", "$2a$10$hashedPassword");
        inactiveUser.setId(2L);
        inactiveUser.setIsActive(false);
        inactiveUser.addRole(employeeRole);
    }

    // --- login() ---

    @Test
    void login_WithValidEmailAndPassword_ReturnsUser() {
        when(userRepository.findByEmail("john.doe@tns.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("plainPassword", activeUser.getPasswordHash())).thenReturn(true);
        when(request.getSession(true)).thenReturn(session);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(session.getId()).thenReturn("session-123");

        User result = authService.login("john.doe@tns.com", "plainPassword", request);

        assertNotNull(result);
        assertEquals(activeUser.getId(), result.getId());
        verify(session).setAttribute("AUTHENTICATED_USER_ID", activeUser.getId());
        verify(session).setMaxInactiveInterval(15 * 60);
    }

    @Test
    void login_WithValidEmployeeIdAndPassword_ReturnsUser() {
        when(userRepository.findByEmail("EMP001")).thenReturn(Optional.empty());
        when(userRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("plainPassword", activeUser.getPasswordHash())).thenReturn(true);
        when(request.getSession(true)).thenReturn(session);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(session.getId()).thenReturn("session-123");

        User result = authService.login("EMP001", "plainPassword", request);

        assertNotNull(result);
        assertEquals(activeUser.getEmployeeId(), result.getEmployeeId());
    }

    @Test
    void login_WithUnknownIdentifier_ThrowsInvalidCredentialsException() {
        when(userRepository.findByEmail("unknown@tns.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmployeeId("unknown@tns.com")).thenReturn(Optional.empty());
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () ->
            authService.login("unknown@tns.com", "anyPassword", request));

        assertEquals("Invalid credentials", ex.getMessage());
        verify(request, never()).getSession(true);
    }

    @Test
    void login_WithWrongPassword_ThrowsInvalidCredentialsException() {
        when(userRepository.findByEmail("john.doe@tns.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrongPassword", activeUser.getPasswordHash())).thenReturn(false);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () ->
            authService.login("john.doe@tns.com", "wrongPassword", request));

        assertEquals("Invalid credentials", ex.getMessage());
        verify(request, never()).getSession(true);
    }

    @Test
    void login_WithInactiveUser_ThrowsInvalidCredentialsException() {
        when(userRepository.findByEmail("jane.smith@tns.com")).thenReturn(Optional.of(inactiveUser));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () ->
            authService.login("jane.smith@tns.com", "anyPassword", request));

        assertEquals("User account is inactive", ex.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(request, never()).getSession(true);
    }

    @Test
    void login_Success_LogsAuditEvent() {
        when(userRepository.findByEmail("john.doe@tns.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("plainPassword", activeUser.getPasswordHash())).thenReturn(true);
        when(request.getSession(true)).thenReturn(session);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(session.getId()).thenReturn("session-abc");

        authService.login("john.doe@tns.com", "plainPassword", request);

        verify(auditLogService).logAsync(eq(activeUser.getId()), eq("LOGIN"), eq("User"),
            eq(activeUser.getId()), anyMap(), eq("10.0.0.1"));
    }

    @Test
    void login_WithWrongPassword_LogsFailedAuditEvent() {
        when(userRepository.findByEmail("john.doe@tns.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrongPassword", activeUser.getPasswordHash())).thenReturn(false);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        assertThrows(InvalidCredentialsException.class, () ->
            authService.login("john.doe@tns.com", "wrongPassword", request));

        verify(auditLogService).logAsync(eq(activeUser.getId()), eq("LOGIN_FAILED"), eq("User"),
            eq(activeUser.getId()), anyMap(), eq("10.0.0.1"));
    }

    // --- logout() ---

    @Test
    void logout_WithActiveSession_InvalidatesSession() {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("AUTHENTICATED_USER_ID")).thenReturn(activeUser.getId());
        when(session.getId()).thenReturn("session-123");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        authService.logout(request);

        verify(session).invalidate();
    }

    @Test
    void logout_WithActiveSession_LogsAuditEvent() {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("AUTHENTICATED_USER_ID")).thenReturn(activeUser.getId());
        when(session.getId()).thenReturn("session-123");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        authService.logout(request);

        verify(auditLogService).logAsync(eq(activeUser.getId()), eq("LOGOUT"), eq("User"),
            eq(activeUser.getId()), anyMap(), eq("10.0.0.1"));
    }

    @Test
    void logout_WithNoSession_DoesNothing() {
        when(request.getSession(false)).thenReturn(null);

        authService.logout(request);

        verify(auditLogService, never()).logAsync(anyLong(), anyString(), anyString(),
            anyLong(), anyMap(), any());
    }

    // --- getCurrentUser() ---

    @Test
    void getCurrentUser_WithValidSession_ReturnsUser() {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("AUTHENTICATED_USER_ID")).thenReturn(activeUser.getId());
        when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));

        User result = authService.getCurrentUser(request);

        assertNotNull(result);
        assertEquals(activeUser.getId(), result.getId());
        assertEquals(activeUser.getEmail(), result.getEmail());
    }

    @Test
    void getCurrentUser_WithNoSession_ThrowsInvalidCredentialsException() {
        when(request.getSession(false)).thenReturn(null);

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () ->
            authService.getCurrentUser(request));

        assertEquals("No active session", ex.getMessage());
    }

    @Test
    void getCurrentUser_WithSessionButNoUserId_ThrowsInvalidCredentialsException() {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("AUTHENTICATED_USER_ID")).thenReturn(null);

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () ->
            authService.getCurrentUser(request));

        assertEquals("No authenticated user in session", ex.getMessage());
    }

    @Test
    void getCurrentUser_WithDeletedUser_ThrowsInvalidCredentialsException() {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("AUTHENTICATED_USER_ID")).thenReturn(999L);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () ->
            authService.getCurrentUser(request));

        assertEquals("User not found", ex.getMessage());
    }

    // --- hasRole() ---

    @Test
    void hasRole_UserHasRole_ReturnsTrue() {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("AUTHENTICATED_USER_ID")).thenReturn(activeUser.getId());
        when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));

        boolean result = authService.hasRole(request, "EMPLOYEE");

        assertTrue(result);
    }

    @Test
    void hasRole_UserDoesNotHaveRole_ReturnsFalse() {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("AUTHENTICATED_USER_ID")).thenReturn(activeUser.getId());
        when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));

        boolean result = authService.hasRole(request, "ADMIN");

        assertFalse(result);
    }

    @Test
    void hasRole_WithNoSession_ReturnsFalse() {
        when(request.getSession(false)).thenReturn(null);

        boolean result = authService.hasRole(request, "EMPLOYEE");

        assertFalse(result);
    }

    // --- getCurrentUserRoles() ---

    @Test
    void getCurrentUserRoles_ReturnsAllRoles() {
        Role managerRole = new Role("MANAGER");
        managerRole.setId(2);
        activeUser.addRole(managerRole);

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("AUTHENTICATED_USER_ID")).thenReturn(activeUser.getId());
        when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));

        Set<String> roles = authService.getCurrentUserRoles(request);

        assertNotNull(roles);
        assertEquals(2, roles.size());
        assertTrue(roles.contains("EMPLOYEE"));
        assertTrue(roles.contains("MANAGER"));
    }

    // --- isSessionValid() ---

    @Test
    void isSessionValid_WithValidSession_ReturnsTrue() {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("AUTHENTICATED_USER_ID")).thenReturn(activeUser.getId());

        boolean result = authService.isSessionValid(request);

        assertTrue(result);
    }

    @Test
    void isSessionValid_WithNoSession_ReturnsFalse() {
        when(request.getSession(false)).thenReturn(null);

        boolean result = authService.isSessionValid(request);

        assertFalse(result);
    }

    @Test
    void isSessionValid_WithSessionButNoUserId_ReturnsFalse() {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("AUTHENTICATED_USER_ID")).thenReturn(null);

        boolean result = authService.isSessionValid(request);

        assertFalse(result);
    }

    // --- refreshSession() ---

    @Test
    void refreshSession_WithActiveSession_ResetsTimeout() {
        when(request.getSession(false)).thenReturn(session);

        authService.refreshSession(request);

        verify(session).setMaxInactiveInterval(15 * 60);
    }

    @Test
    void refreshSession_WithNoSession_DoesNothing() {
        when(request.getSession(false)).thenReturn(null);

        // Should not throw
        assertDoesNotThrow(() -> authService.refreshSession(request));
        verify(session, never()).setMaxInactiveInterval(anyInt());
    }
}
