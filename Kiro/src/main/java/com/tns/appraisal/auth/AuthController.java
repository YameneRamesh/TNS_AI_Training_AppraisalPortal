package com.tns.appraisal.auth;

import com.tns.appraisal.auth.dto.LoginRequest;
import com.tns.appraisal.auth.dto.UserProfileResponse;
import com.tns.appraisal.common.dto.ApiResponse;
import com.tns.appraisal.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

/**
 * REST controller for authentication operations.
 * Provides endpoints for login, logout, and retrieving current user profile.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Authenticate user and create session.
     * 
     * POST /api/auth/login
     * 
     * @param loginRequest login credentials (email/employeeId and password)
     * @param request HTTP request for session management
     * @return user profile with roles on success
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserProfileResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {
        
        logger.info("Login request received for identifier: {}", loginRequest.getLoginIdentifier());

        User user = authService.login(
            loginRequest.getLoginIdentifier(),
            loginRequest.getPassword(),
            request
        );

        UserProfileResponse profile = mapToUserProfile(user);
        
        return ResponseEntity.ok(ApiResponse.success("Login successful", profile));
    }

    /**
     * Logout current user and invalidate session.
     * 
     * POST /api/auth/logout
     * 
     * @param request HTTP request containing the session
     * @return success message
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        logger.info("Logout request received");

        authService.logout(request);

        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    /**
     * Get current authenticated user profile.
     * 
     * GET /api/auth/me
     * 
     * @param request HTTP request containing the session
     * @return current user profile with roles
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(HttpServletRequest request) {
        logger.debug("Get current user request received");

        User user = authService.getCurrentUser(request);
        UserProfileResponse profile = mapToUserProfile(user);

        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    /**
     * Map User entity to UserProfileResponse DTO.
     * 
     * @param user User entity
     * @return UserProfileResponse DTO
     */
    private UserProfileResponse mapToUserProfile(User user) {
        String managerName = user.getManager() != null ? user.getManager().getFullName() : null;
        
        return new UserProfileResponse(
            user.getId(),
            user.getEmployeeId(),
            user.getFullName(),
            user.getEmail(),
            user.getDesignation(),
            user.getDepartment(),
            managerName,
            user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet())
        );
    }
}
