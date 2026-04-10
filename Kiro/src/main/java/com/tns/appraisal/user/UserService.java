package com.tns.appraisal.user;

import com.tns.appraisal.auth.PasswordHashingService;
import com.tns.appraisal.auth.Role;
import com.tns.appraisal.auth.RoleRepository;
import com.tns.appraisal.audit.AuditLogService;
import com.tns.appraisal.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for user management operations including CRUD, role assignment, and search/filter capabilities.
 * Supports admin functions for creating, updating, deactivating, and deleting user accounts.
 */
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordHashingService passwordHashingService;
    private final AuditLogService auditLogService;

    public UserService(UserRepository userRepository,
                      RoleRepository roleRepository,
                      PasswordHashingService passwordHashingService,
                      AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordHashingService = passwordHashingService;
        this.auditLogService = auditLogService;
    }

    /**
     * Create a new user with password hashing and role assignment.
     *
     * @param employeeId unique employee identifier
     * @param fullName user's full name
     * @param email user's email address
     * @param plainPassword plain text password (will be hashed)
     * @param designation user's job title
     * @param department user's department
     * @param managerId ID of the user's manager (optional)
     * @param roleNames set of role names to assign (e.g., "EMPLOYEE", "MANAGER")
     * @param createdByUserId ID of the admin creating this user
     * @return created user with roles
     * @throws BusinessException if validation fails or user already exists
     */
    @Transactional
    public User createUser(String employeeId, String fullName, String email, String plainPassword,
                          String designation, String department, Long managerId,
                          Set<String> roleNames, Long createdByUserId) {
        logger.debug("Creating user with employeeId: {}", employeeId);

        // Validate required fields
        validateRequiredFields(employeeId, fullName, email, plainPassword);

        // Validate password strength
        String passwordError = passwordHashingService.getPasswordStrengthError(plainPassword);
        if (passwordError != null) {
            throw new BusinessException(passwordError);
        }

        // Check for duplicate employee ID
        if (userRepository.existsByEmployeeId(employeeId)) {
            throw new BusinessException("User with employee ID " + employeeId + " already exists");
        }

        // Check for duplicate email
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("User with email " + email + " already exists");
        }

        // Hash password
        String passwordHash = passwordHashingService.hashPassword(plainPassword);

        // Create user entity
        User user = new User(employeeId, fullName, email, passwordHash);
        user.setDesignation(designation);
        user.setDepartment(department);
        user.setIsActive(true);

        // Set manager if provided
        if (managerId != null) {
            User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new BusinessException("Manager with ID " + managerId + " not found"));
            user.setManager(manager);
        }

        // Assign roles
        if (roleNames != null && !roleNames.isEmpty()) {
            Set<Role> roles = resolveRoles(roleNames);
            user.setRoles(roles);
        }

        // Save user
        User savedUser = userRepository.save(user);
        logger.info("User created successfully: {} (ID: {})", savedUser.getEmployeeId(), savedUser.getId());

        // Audit log
        auditLogService.logAsync(createdByUserId, "USER_CREATED", "User", savedUser.getId(),
            Map.of("employeeId", employeeId, "email", email, "roles", roleNames != null ? roleNames : Collections.emptySet()),
            null);

        return savedUser;
    }

    /**
     * Get user by ID.
     *
     * @param userId user ID
     * @return user with roles
     * @throws BusinessException if user not found
     */
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        logger.debug("Fetching user by ID: {}", userId);
        return userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException("User with ID " + userId + " not found"));
    }

    /**
     * Get user by email address.
     *
     * @param email email address
     * @return user with roles
     * @throws BusinessException if user not found
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        logger.debug("Fetching user by email: {}", email);
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException("User with email " + email + " not found"));
    }

    /**
     * Get user by employee ID.
     *
     * @param employeeId employee ID
     * @return user with roles
     * @throws BusinessException if user not found
     */
    @Transactional(readOnly = true)
    public User getUserByEmployeeId(String employeeId) {
        logger.debug("Fetching user by employeeId: {}", employeeId);
        return userRepository.findByEmployeeId(employeeId)
            .orElseThrow(() -> new BusinessException("User with employee ID " + employeeId + " not found"));
    }

    /**
     * Get all users with pagination.
     *
     * @param pageable pagination parameters
     * @return page of users
     */
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        logger.debug("Fetching all users with pagination");
        return userRepository.findAll(pageable);
    }

    /**
     * Get all active users.
     *
     * @return list of active users
     */
    @Transactional(readOnly = true)
    public List<User> getAllActiveUsers() {
        logger.debug("Fetching all active users");
        return userRepository.findAll().stream()
            .filter(User::getIsActive)
            .collect(Collectors.toList());
    }

    /**
     * Update user information.
     * Password is only updated if a new password is provided.
     *
     * @param userId user ID to update
     * @param fullName updated full name (optional)
     * @param email updated email (optional)
     * @param plainPassword new password (optional, will be hashed)
     * @param designation updated designation (optional)
     * @param department updated department (optional)
     * @param managerId updated manager ID (optional)
     * @param updatedByUserId ID of the admin updating this user
     * @return updated user
     * @throws BusinessException if validation fails or user not found
     */
    @Transactional
    public User updateUser(Long userId, String fullName, String email, String plainPassword,
                          String designation, String department, Long managerId,
                          Long updatedByUserId) {
        logger.debug("Updating user with ID: {}", userId);

        User user = getUserById(userId);
        Map<String, Object> changes = new HashMap<>();

        // Update full name
        if (fullName != null && !fullName.trim().isEmpty()) {
            user.setFullName(fullName);
            changes.put("fullName", fullName);
        }

        // Update email (check for duplicates)
        if (email != null && !email.trim().isEmpty() && !email.equals(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new BusinessException("Email " + email + " is already in use");
            }
            user.setEmail(email);
            changes.put("email", email);
        }

        // Update password if provided
        if (plainPassword != null && !plainPassword.trim().isEmpty()) {
            String passwordError = passwordHashingService.getPasswordStrengthError(plainPassword);
            if (passwordError != null) {
                throw new BusinessException(passwordError);
            }
            String passwordHash = passwordHashingService.hashPassword(plainPassword);
            user.setPasswordHash(passwordHash);
            changes.put("passwordUpdated", true);
        }

        // Update designation
        if (designation != null) {
            user.setDesignation(designation);
            changes.put("designation", designation);
        }

        // Update department
        if (department != null) {
            user.setDepartment(department);
            changes.put("department", department);
        }

        // Update manager
        if (managerId != null) {
            if (managerId.equals(userId)) {
                throw new BusinessException("User cannot be their own manager");
            }
            User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new BusinessException("Manager with ID " + managerId + " not found"));
            user.setManager(manager);
            changes.put("managerId", managerId);
        }

        User updatedUser = userRepository.save(user);
        logger.info("User updated successfully: {} (ID: {})", updatedUser.getEmployeeId(), updatedUser.getId());

        // Audit log
        auditLogService.logAsync(updatedByUserId, "USER_UPDATED", "User", userId, changes, null);

        return updatedUser;
    }

    /**
     * Deactivate a user (soft delete).
     * Sets is_active to false, preventing login but preserving data.
     *
     * @param userId user ID to deactivate
     * @param deactivatedByUserId ID of the admin deactivating this user
     * @throws BusinessException if user not found
     */
    @Transactional
    public void deactivateUser(Long userId, Long deactivatedByUserId) {
        logger.debug("Deactivating user with ID: {}", userId);

        User user = getUserById(userId);
        user.setIsActive(false);
        userRepository.save(user);

        logger.info("User deactivated successfully: {} (ID: {})", user.getEmployeeId(), user.getId());

        // Audit log
        auditLogService.logAsync(deactivatedByUserId, "USER_DEACTIVATED", "User", userId,
            Map.of("employeeId", user.getEmployeeId()), null);
    }

    /**
     * Reactivate a previously deactivated user.
     *
     * @param userId user ID to reactivate
     * @param reactivatedByUserId ID of the admin reactivating this user
     * @throws BusinessException if user not found
     */
    @Transactional
    public void reactivateUser(Long userId, Long reactivatedByUserId) {
        logger.debug("Reactivating user with ID: {}", userId);

        User user = getUserById(userId);
        user.setIsActive(true);
        userRepository.save(user);

        logger.info("User reactivated successfully: {} (ID: {})", user.getEmployeeId(), user.getId());

        // Audit log
        auditLogService.logAsync(reactivatedByUserId, "USER_REACTIVATED", "User", userId,
            Map.of("employeeId", user.getEmployeeId()), null);
    }

    /**
     * Delete a user permanently (hard delete).
     * Use with caution - this removes all user data.
     *
     * @param userId user ID to delete
     * @param deletedByUserId ID of the admin deleting this user
     * @throws BusinessException if user not found
     */
    @Transactional
    public void deleteUser(Long userId, Long deletedByUserId) {
        logger.debug("Deleting user with ID: {}", userId);

        User user = getUserById(userId);
        String employeeId = user.getEmployeeId();

        userRepository.delete(user);
        logger.warn("User deleted permanently: {} (ID: {})", employeeId, userId);

        // Audit log
        auditLogService.logAsync(deletedByUserId, "USER_DELETED", "User", userId,
            Map.of("employeeId", employeeId), null);
    }

    /**
     * Assign roles to a user.
     * Replaces existing roles with the provided set.
     *
     * @param userId user ID
     * @param roleNames set of role names to assign
     * @param assignedByUserId ID of the admin assigning roles
     * @return updated user with new roles
     * @throws BusinessException if user or roles not found, or if roleNames is empty
     */
    @Transactional
    public User assignRoles(Long userId, Set<String> roleNames, Long assignedByUserId) {
        logger.debug("Assigning roles to user ID: {}", userId);

        // Validate that at least one role is being assigned
        if (roleNames == null || roleNames.isEmpty()) {
            throw new BusinessException("At least one role must be assigned to a user");
        }

        User user = getUserById(userId);
        Set<String> previousRoles = user.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toSet());
        
        Set<Role> roles = resolveRoles(roleNames);

        user.setRoles(roles);
        User updatedUser = userRepository.save(user);

        logger.info("Roles assigned to user {}: {} (previous: {})", 
            user.getEmployeeId(), roleNames, previousRoles);

        // Audit log
        auditLogService.logAsync(assignedByUserId, "ROLES_ASSIGNED", "User", userId,
            Map.of("roles", roleNames, "previousRoles", previousRoles), null);

        return updatedUser;
    }

    /**
     * Add a single role to a user.
     * Idempotent - if user already has the role, no error is thrown.
     *
     * @param userId user ID
     * @param roleName role name to add
     * @param assignedByUserId ID of the admin adding the role
     * @return updated user
     * @throws BusinessException if user or role not found
     */
    @Transactional
    public User addRole(Long userId, String roleName, Long assignedByUserId) {
        logger.debug("Adding role {} to user ID: {}", roleName, userId);

        User user = getUserById(userId);
        Role role = roleRepository.findByName(roleName)
            .orElseThrow(() -> new BusinessException("Role " + roleName + " not found"));

        // Check if user already has this role
        boolean alreadyHasRole = user.getRoles().stream()
            .anyMatch(r -> r.getName().equals(roleName));
        
        if (alreadyHasRole) {
            logger.info("User {} already has role {}, no changes made", user.getEmployeeId(), roleName);
            return user;
        }

        user.addRole(role);
        User updatedUser = userRepository.save(user);

        logger.info("Role {} added to user {}", roleName, user.getEmployeeId());

        // Audit log
        auditLogService.logAsync(assignedByUserId, "ROLE_ADDED", "User", userId,
            Map.of("role", roleName, "allRoles", 
                updatedUser.getRoles().stream().map(Role::getName).collect(Collectors.toSet())), null);

        return updatedUser;
    }

    /**
     * Remove a single role from a user.
     * Prevents removing the last role from a user.
     *
     * @param userId user ID
     * @param roleName role name to remove
     * @param revokedByUserId ID of the admin removing the role
     * @return updated user
     * @throws BusinessException if user or role not found, or if attempting to remove the last role
     */
    @Transactional
    public User removeRole(Long userId, String roleName, Long revokedByUserId) {
        logger.debug("Removing role {} from user ID: {}", roleName, userId);

        User user = getUserById(userId);
        
        // Validate that user will have at least one role after removal
        if (user.getRoles().size() <= 1) {
            throw new BusinessException("Cannot remove the last role from a user. At least one role must be assigned.");
        }
        
        Role role = roleRepository.findByName(roleName)
            .orElseThrow(() -> new BusinessException("Role " + roleName + " not found"));

        // Check if user actually has this role
        boolean hasRole = user.getRoles().stream()
            .anyMatch(r -> r.getName().equals(roleName));
        
        if (!hasRole) {
            throw new BusinessException("User does not have role " + roleName);
        }

        user.removeRole(role);
        User updatedUser = userRepository.save(user);

        logger.info("Role {} removed from user {}", roleName, user.getEmployeeId());

        // Audit log
        auditLogService.logAsync(revokedByUserId, "ROLE_REMOVED", "User", userId,
            Map.of("role", roleName, "remainingRoles", 
                updatedUser.getRoles().stream().map(Role::getName).collect(Collectors.toSet())), null);

        return updatedUser;
    }

    /**
     * Search and filter users by various criteria.
     *
     * @param searchTerm search term for name or email (optional)
     * @param department filter by department (optional)
     * @param roleName filter by role name (optional)
     * @param isActive filter by active status (optional)
     * @param pageable pagination parameters
     * @return page of users matching criteria
     */
    @Transactional(readOnly = true)
    public Page<User> searchUsers(String searchTerm, String department, String roleName,
                                  Boolean isActive, Pageable pageable) {
        logger.debug("Searching users with criteria - searchTerm: {}, department: {}, role: {}, isActive: {}",
            searchTerm, department, roleName, isActive);

        // Get all users and apply filters
        List<User> allUsers = userRepository.findAll();
        List<User> filteredUsers = allUsers.stream()
            .filter(user -> matchesSearchTerm(user, searchTerm))
            .filter(user -> matchesDepartment(user, department))
            .filter(user -> matchesRole(user, roleName))
            .filter(user -> matchesActiveStatus(user, isActive))
            .collect(Collectors.toList());

        // Apply pagination manually
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredUsers.size());
        List<User> pageContent = filteredUsers.subList(start, end);

        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, filteredUsers.size());
    }

    /**
     * Get all users with a specific role.
     *
     * @param roleName role name to filter by
     * @return list of users with the specified role
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByRole(String roleName) {
        logger.debug("Fetching users with role: {}", roleName);
        return userRepository.findAll().stream()
            .filter(user -> user.getRoles().stream()
                .anyMatch(role -> role.getName().equals(roleName)))
            .collect(Collectors.toList());
    }

    /**
     * Get all users in a specific department.
     *
     * @param department department name
     * @return list of users in the department
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByDepartment(String department) {
        logger.debug("Fetching users in department: {}", department);
        return userRepository.findAll().stream()
            .filter(user -> department.equals(user.getDepartment()))
            .collect(Collectors.toList());
    }

    /**
     * Get all direct reports for a manager.
     *
     * @param managerId manager's user ID
     * @return list of users reporting to the manager
     */
    @Transactional(readOnly = true)
    public List<User> getDirectReports(Long managerId) {
        logger.debug("Fetching direct reports for manager ID: {}", managerId);
        return userRepository.findAll().stream()
            .filter(user -> user.getManager() != null && user.getManager().getId().equals(managerId))
            .collect(Collectors.toList());
    }

    /**
     * Get all roles assigned to a user.
     *
     * @param userId user ID
     * @return set of role names
     * @throws BusinessException if user not found
     */
    @Transactional(readOnly = true)
    public Set<String> getUserRoles(Long userId) {
        logger.debug("Fetching roles for user ID: {}", userId);
        User user = getUserById(userId);
        return user.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toSet());
    }

    /**
     * Check if a user has a specific role.
     *
     * @param userId user ID
     * @param roleName role name to check
     * @return true if user has the role, false otherwise
     * @throws BusinessException if user not found
     */
    @Transactional(readOnly = true)
    public boolean hasRole(Long userId, String roleName) {
        logger.debug("Checking if user ID {} has role {}", userId, roleName);
        User user = getUserById(userId);
        return user.getRoles().stream()
            .anyMatch(role -> role.getName().equals(roleName));
    }

    /**
     * Validate that a user has at least one role assigned.
     * This is a business rule validation method.
     *
     * @param userId user ID
     * @throws BusinessException if user has no roles
     */
    @Transactional(readOnly = true)
    public void validateUserHasRoles(Long userId) {
        User user = getUserById(userId);
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            throw new BusinessException("User must have at least one role assigned");
        }
    }

    // Private helper methods

    private void validateRequiredFields(String employeeId, String fullName, String email, String password) {
        if (employeeId == null || employeeId.trim().isEmpty()) {
            throw new BusinessException("Employee ID is required");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new BusinessException("Full name is required");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessException("Email is required");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new BusinessException("Password is required");
        }
    }

    private Set<Role> resolveRoles(Set<String> roleNames) {
        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new BusinessException("Role " + roleName + " not found"));
            roles.add(role);
        }
        return roles;
    }

    private boolean matchesSearchTerm(User user, String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return true;
        }
        String term = searchTerm.toLowerCase();
        return user.getFullName().toLowerCase().contains(term) ||
               user.getEmail().toLowerCase().contains(term) ||
               user.getEmployeeId().toLowerCase().contains(term);
    }

    private boolean matchesDepartment(User user, String department) {
        if (department == null || department.trim().isEmpty()) {
            return true;
        }
        return department.equals(user.getDepartment());
    }

    private boolean matchesRole(User user, String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            return true;
        }
        return user.getRoles().stream()
            .anyMatch(role -> role.getName().equals(roleName));
    }

    private boolean matchesActiveStatus(User user, Boolean isActive) {
        if (isActive == null) {
            return true;
        }
        return isActive.equals(user.getIsActive());
    }
}
