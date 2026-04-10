package com.tns.appraisal.user;

import com.tns.appraisal.auth.AuthService;
import com.tns.appraisal.common.dto.ApiResponse;
import com.tns.appraisal.common.dto.PageResponse;
import com.tns.appraisal.user.dto.AssignRolesRequest;
import com.tns.appraisal.user.dto.CreateUserRequest;
import com.tns.appraisal.user.dto.UpdateUserRequest;
import com.tns.appraisal.user.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST controller for user management operations.
 * All endpoints require ADMIN role.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    /**
     * Create a new user.
     * 
     * POST /api/users
     * 
     * @param request create user request
     * @param httpRequest HTTP request for getting current user
     * @return created user
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request,
            HttpServletRequest httpRequest) {
        
        logger.info("Create user request received for employeeId: {}", request.getEmployeeId());

        User currentUser = authService.getCurrentUser(httpRequest);

        User user = userService.createUser(
            request.getEmployeeId(),
            request.getFullName(),
            request.getEmail(),
            request.getPassword(),
            request.getDesignation(),
            request.getDepartment(),
            request.getManagerId(),
            request.getRoles(),
            currentUser.getId()
        );

        UserResponse response = new UserResponse(user);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("User created successfully", response));
    }

    /**
     * Get user by ID.
     * 
     * GET /api/users/{id}
     * 
     * @param id user ID
     * @return user details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        logger.debug("Get user by ID request received: {}", id);

        User user = userService.getUserById(id);
        UserResponse response = new UserResponse(user);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * List all users with pagination and search/filter capabilities.
     * 
     * GET /api/users
     * 
     * @param searchTerm search term for name, email, or employee ID (optional)
     * @param department filter by department (optional)
     * @param role filter by role name (optional)
     * @param isActive filter by active status (optional)
     * @param page page number (default: 0)
     * @param size page size (default: 20)
     * @param sort sort field (default: fullName)
     * @param direction sort direction (default: ASC)
     * @return paginated list of users
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> listUsers(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fullName") String sort,
            @RequestParam(defaultValue = "ASC") String direction) {
        
        logger.debug("List users request - searchTerm: {}, department: {}, role: {}, isActive: {}, page: {}, size: {}",
            searchTerm, department, role, isActive, page, size);

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        Page<User> userPage = userService.searchUsers(searchTerm, department, role, isActive, pageable);
        
        Page<UserResponse> responsePage = userPage.map(UserResponse::new);
        PageResponse<UserResponse> pageResponse = new PageResponse<>(responsePage);

        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    /**
     * Update user information.
     * 
     * PUT /api/users/{id}
     * 
     * @param id user ID
     * @param request update user request
     * @param httpRequest HTTP request for getting current user
     * @return updated user
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            HttpServletRequest httpRequest) {
        
        logger.info("Update user request received for ID: {}", id);

        User currentUser = authService.getCurrentUser(httpRequest);

        User user = userService.updateUser(
            id,
            request.getFullName(),
            request.getEmail(),
            request.getPassword(),
            request.getDesignation(),
            request.getDepartment(),
            request.getManagerId(),
            currentUser.getId()
        );

        UserResponse response = new UserResponse(user);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", response));
    }

    /**
     * Deactivate user (soft delete).
     * 
     * DELETE /api/users/{id}
     * 
     * @param id user ID
     * @param httpRequest HTTP request for getting current user
     * @return success message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        
        logger.info("Deactivate user request received for ID: {}", id);

        User currentUser = authService.getCurrentUser(httpRequest);
        userService.deactivateUser(id, currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success("User deactivated successfully", null));
    }

    /**
     * Assign roles to a user (replaces existing roles).
     * 
     * POST /api/users/{id}/roles
     * 
     * @param id user ID
     * @param request assign roles request
     * @param httpRequest HTTP request for getting current user
     * @return updated user with new roles
     */
    @PostMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> assignRoles(
            @PathVariable Long id,
            @Valid @RequestBody AssignRolesRequest request,
            HttpServletRequest httpRequest) {
        
        logger.info("Assign roles request received for user ID: {}", id);

        User currentUser = authService.getCurrentUser(httpRequest);
        User user = userService.assignRoles(id, request.getRoles(), currentUser.getId());

        UserResponse response = new UserResponse(user);
        return ResponseEntity.ok(ApiResponse.success("Roles assigned successfully", response));
    }

    /**
     * Add a single role to a user.
     * 
     * POST /api/users/{id}/roles/{roleName}
     * 
     * @param id user ID
     * @param roleName role name to add
     * @param httpRequest HTTP request for getting current user
     * @return updated user
     */
    @PostMapping("/{id}/roles/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> addRole(
            @PathVariable Long id,
            @PathVariable String roleName,
            HttpServletRequest httpRequest) {
        
        logger.info("Add role request received - user ID: {}, role: {}", id, roleName);

        User currentUser = authService.getCurrentUser(httpRequest);
        User user = userService.addRole(id, roleName, currentUser.getId());

        UserResponse response = new UserResponse(user);
        return ResponseEntity.ok(ApiResponse.success("Role added successfully", response));
    }

    /**
     * Remove a single role from a user.
     * 
     * DELETE /api/users/{id}/roles/{roleName}
     * 
     * @param id user ID
     * @param roleName role name to remove
     * @param httpRequest HTTP request for getting current user
     * @return updated user
     */
    @DeleteMapping("/{id}/roles/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> removeRole(
            @PathVariable Long id,
            @PathVariable String roleName,
            HttpServletRequest httpRequest) {
        
        logger.info("Remove role request received - user ID: {}, role: {}", id, roleName);

        User currentUser = authService.getCurrentUser(httpRequest);
        User user = userService.removeRole(id, roleName, currentUser.getId());

        UserResponse response = new UserResponse(user);
        return ResponseEntity.ok(ApiResponse.success("Role removed successfully", response));
    }

    /**
     * Get direct reports for a user (manager).
     * 
     * GET /api/users/{id}/direct-reports
     * 
     * @param id manager's user ID
     * @return list of direct reports
     */
    @GetMapping("/{id}/direct-reports")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<UserResponse>>> getDirectReports(@PathVariable Long id) {
        logger.debug("Get direct reports request received for manager ID: {}", id);

        List<User> directReports = userService.getDirectReports(id);
        List<UserResponse> response = directReports.stream()
            .map(UserResponse::new)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Reactivate a previously deactivated user.
     *
     * PATCH /api/users/{id}/reactivate
     *
     * @param id user ID
     * @param httpRequest HTTP request for getting current user
     * @return success message
     */
    @PatchMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reactivateUser(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        logger.info("Reactivate user request received for ID: {}", id);

        User currentUser = authService.getCurrentUser(httpRequest);
        userService.reactivateUser(id, currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success("User reactivated successfully", null));
    }

    /**
     * Get all roles assigned to a user.
     * 
     * GET /api/users/{id}/roles
     * 
     * @param id user ID
     * @return set of role names
     */
    @GetMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Set<String>>> getUserRoles(@PathVariable Long id) {
        logger.debug("Get roles request received for user ID: {}", id);

        Set<String> roles = userService.getUserRoles(id);
        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    /**
     * Check if a user has a specific role.
     * 
     * GET /api/users/{id}/roles/{roleName}/check
     * 
     * @param id user ID
     * @param roleName role name to check
     * @return true if user has the role, false otherwise
     */
    @GetMapping("/{id}/roles/{roleName}/check")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> checkUserHasRole(
            @PathVariable Long id,
            @PathVariable String roleName) {
        logger.debug("Check role request received - user ID: {}, role: {}", id, roleName);

        boolean hasRole = userService.hasRole(id, roleName);
        return ResponseEntity.ok(ApiResponse.success(hasRole));
    }
}
