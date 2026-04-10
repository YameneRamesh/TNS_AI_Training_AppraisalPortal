package com.tns.appraisal.auth;

import com.tns.appraisal.audit.AuditLogService;
import com.tns.appraisal.exception.InvalidCredentialsException;
import com.tns.appraisal.user.User;
import com.tns.appraisal.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for authentication operations including login, logout, and session management.
 * Implements session-based authentication with 15-minute timeout.
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final String SESSION_USER_KEY = "AUTHENTICATED_USER_ID";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public AuthService(UserRepository userRepository, 
                      PasswordEncoder passwordEncoder,
                      AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    /**
     * Authenticate user with email/employeeId and password.
     * Creates a new session on successful authentication.
     *
     * @param loginIdentifier email or employee ID
     * @param password plain text password
     * @param request HTTP request for session management
     * @return authenticated user with roles
     * @throws InvalidCredentialsException if credentials are invalid or user is inactive
     */
    @Transactional(readOnly = true)
    public User login(String loginIdentifier, String password, HttpServletRequest request) {
        logger.debug("Login attempt for identifier: {}", loginIdentifier);

        // Find user by email or employee ID
        Optional<User> userOpt = userRepository.findByEmail(loginIdentifier);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmployeeId(loginIdentifier);
        }

        // Validate user exists
        if (userOpt.isEmpty()) {
            logger.warn("Login failed: user not found for identifier: {}", loginIdentifier);
            auditLogAsync(null, "LOGIN_FAILED", "User", null, 
                Map.of("reason", "User not found", "identifier", loginIdentifier), 
                request.getRemoteAddr());
            throw new InvalidCredentialsException("Invalid credentials");
        }

        User user = userOpt.get();

        // Validate user is active
        if (!user.getIsActive()) {
            logger.warn("Login failed: user is inactive: {}", user.getEmployeeId());
            auditLogAsync(user.getId(), "LOGIN_FAILED", "User", user.getId(), 
                Map.of("reason", "User inactive"), 
                request.getRemoteAddr());
            throw new InvalidCredentialsException("User account is inactive");
        }

        // Validate password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            logger.warn("Login failed: invalid password for user: {}", user.getEmployeeId());
            auditLogAsync(user.getId(), "LOGIN_FAILED", "User", user.getId(), 
                Map.of("reason", "Invalid password"), 
                request.getRemoteAddr());
            throw new InvalidCredentialsException("Invalid credentials");
        }

        // Create new session and store user ID
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_USER_KEY, user.getId());
        session.setMaxInactiveInterval(15 * 60); // 15 minutes in seconds

        logger.info("Login successful for user: {} (session: {})", user.getEmployeeId(), session.getId());
        
        // Log successful login
        auditLogAsync(user.getId(), "LOGIN", "User", user.getId(), 
            Map.of("sessionId", session.getId()), 
            request.getRemoteAddr());

        return user;
    }

    /**
     * Logout the current user by invalidating the session.
     *
     * @param request HTTP request containing the session
     */
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Long userId = (Long) session.getAttribute(SESSION_USER_KEY);
            String sessionId = session.getId();
            
            // Invalidate session
            session.invalidate();
            
            logger.info("Logout successful for userId: {} (session: {})", userId, sessionId);
            
            // Log logout
            if (userId != null) {
                auditLogAsync(userId, "LOGOUT", "User", userId, 
                    Map.of("sessionId", sessionId), 
                    request.getRemoteAddr());
            }
        }
    }

    /**
     * Get the currently authenticated user from the session.
     *
     * @param request HTTP request containing the session
     * @return authenticated user with roles
     * @throws InvalidCredentialsException if no valid session exists
     */
    @Transactional(readOnly = true)
    public User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new InvalidCredentialsException("No active session");
        }

        Long userId = (Long) session.getAttribute(SESSION_USER_KEY);
        if (userId == null) {
            throw new InvalidCredentialsException("No authenticated user in session");
        }

        return userRepository.findById(userId)
            .orElseThrow(() -> new InvalidCredentialsException("User not found"));
    }

    /**
     * Check if the current user has a specific role.
     *
     * @param request HTTP request containing the session
     * @param roleName role name to check (e.g., "EMPLOYEE", "MANAGER", "HR", "ADMIN")
     * @return true if user has the role, false otherwise
     */
    public boolean hasRole(HttpServletRequest request, String roleName) {
        try {
            User user = getCurrentUser(request);
            return user.getRoles().stream()
                .anyMatch(role -> role.getName().equals(roleName));
        } catch (InvalidCredentialsException e) {
            return false;
        }
    }

    /**
     * Get all role names for the current user.
     *
     * @param request HTTP request containing the session
     * @return set of role names
     */
    public Set<String> getCurrentUserRoles(HttpServletRequest request) {
        User user = getCurrentUser(request);
        return user.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toSet());
    }

    /**
     * Validate that the session is active and not expired.
     *
     * @param request HTTP request containing the session
     * @return true if session is valid, false otherwise
     */
    public boolean isSessionValid(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }

        Long userId = (Long) session.getAttribute(SESSION_USER_KEY);
        return userId != null;
    }

    /**
     * Refresh the session timeout (reset the 15-minute timer).
     *
     * @param request HTTP request containing the session
     */
    public void refreshSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            // Accessing the session automatically refreshes the timeout
            session.setMaxInactiveInterval(15 * 60);
            logger.debug("Session refreshed for session: {}", session.getId());
        }
    }

    /**
     * Helper method to log audit events asynchronously.
     */
    private void auditLogAsync(Long userId, String action, String entityType, Long entityId, 
                               Map<String, Object> details, String ipAddress) {
        auditLogService.logAsync(userId, action, entityType, entityId, details, ipAddress);
    }
}
