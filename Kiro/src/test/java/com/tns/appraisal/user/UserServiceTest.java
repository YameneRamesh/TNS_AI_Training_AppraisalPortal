package com.tns.appraisal.user;

import com.tns.appraisal.auth.PasswordHashingService;
import com.tns.appraisal.auth.Role;
import com.tns.appraisal.auth.RoleRepository;
import com.tns.appraisal.audit.AuditLogService;
import com.tns.appraisal.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordHashingService passwordHashingService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role employeeRole;
    private Role managerRole;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User("EMP001", "John Doe", "john.doe@tns.com", "hashedPassword123");
        testUser.setId(1L);
        testUser.setDesignation("Software Engineer");
        testUser.setDepartment("Engineering");
        testUser.setIsActive(true);

        // Setup roles
        employeeRole = new Role("EMPLOYEE");
        employeeRole.setId(1);
        
        managerRole = new Role("MANAGER");
        managerRole.setId(2);

        testUser.addRole(employeeRole);
    }

    @Test
    void testCreateUser_Success() {
        // Given
        String employeeId = "EMP002";
        String fullName = "Jane Smith";
        String email = "jane.smith@tns.com";
        String plainPassword = "TestPass123";
        String designation = "Senior Engineer";
        String department = "Engineering";
        Set<String> roleNames = Set.of("EMPLOYEE");
        Long createdByUserId = 1L;

        when(passwordHashingService.getPasswordStrengthError(plainPassword)).thenReturn(null);
        when(userRepository.existsByEmployeeId(employeeId)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordHashingService.hashPassword(plainPassword)).thenReturn("hashedPassword");
        when(roleRepository.findByName("EMPLOYEE")).thenReturn(Optional.of(employeeRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });

        // When
        User createdUser = userService.createUser(employeeId, fullName, email, plainPassword,
            designation, department, null, roleNames, createdByUserId);

        // Then
        assertNotNull(createdUser);
        assertEquals(employeeId, createdUser.getEmployeeId());
        assertEquals(fullName, createdUser.getFullName());
        assertEquals(email, createdUser.getEmail());
        assertEquals(designation, createdUser.getDesignation());
        assertEquals(department, createdUser.getDepartment());
        assertTrue(createdUser.getIsActive());
        assertEquals(1, createdUser.getRoles().size());

        verify(userRepository).save(any(User.class));
        verify(auditLogService).logAsync(eq(createdByUserId), eq("USER_CREATED"), eq("User"), 
            eq(2L), anyMap(), isNull());
    }

    @Test
    void testCreateUser_WithManager_Success() {
        // Given
        String employeeId = "EMP002";
        String fullName = "Jane Smith";
        String email = "jane.smith@tns.com";
        String plainPassword = "TestPass123";
        Long managerId = 1L;
        Set<String> roleNames = Set.of("EMPLOYEE");
        Long createdByUserId = 1L;

        when(passwordHashingService.getPasswordStrengthError(plainPassword)).thenReturn(null);
        when(userRepository.existsByEmployeeId(employeeId)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordHashingService.hashPassword(plainPassword)).thenReturn("hashedPassword");
        when(userRepository.findById(managerId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("EMPLOYEE")).thenReturn(Optional.of(employeeRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });

        // When
        User createdUser = userService.createUser(employeeId, fullName, email, plainPassword,
            null, null, managerId, roleNames, createdByUserId);

        // Then
        assertNotNull(createdUser);
        assertNotNull(createdUser.getManager());
        assertEquals(testUser.getId(), createdUser.getManager().getId());
    }

    @Test
    void testCreateUser_DuplicateEmployeeId_ThrowsException() {
        // Given
        String employeeId = "EMP001";
        when(userRepository.existsByEmployeeId(employeeId)).thenReturn(true);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.createUser(employeeId, "John Doe", "john@tns.com", "TestPass123",
                null, null, null, null, 1L);
        });

        assertEquals("User with employee ID EMP001 already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testCreateUser_DuplicateEmail_ThrowsException() {
        // Given
        String email = "john.doe@tns.com";
        when(userRepository.existsByEmployeeId(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.createUser("EMP002", "Jane Smith", email, "TestPass123",
                null, null, null, null, 1L);
        });

        assertEquals("User with email john.doe@tns.com already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testCreateUser_WeakPassword_ThrowsException() {
        // Given
        String weakPassword = "weak";
        when(passwordHashingService.getPasswordStrengthError(weakPassword))
            .thenReturn("Password must be at least 8 characters long");

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.createUser("EMP002", "Jane Smith", "jane@tns.com", weakPassword,
                null, null, null, null, 1L);
        });

        assertEquals("Password must be at least 8 characters long", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testCreateUser_MissingRequiredFields_ThrowsException() {
        // When & Then - null employeeId
        assertThrows(BusinessException.class, () -> {
            userService.createUser(null, "John Doe", "john@tns.com", "TestPass123",
                null, null, null, null, 1L);
        });

        // When & Then - null fullName
        assertThrows(BusinessException.class, () -> {
            userService.createUser("EMP001", null, "john@tns.com", "TestPass123",
                null, null, null, null, 1L);
        });

        // When & Then - null email
        assertThrows(BusinessException.class, () -> {
            userService.createUser("EMP001", "John Doe", null, "TestPass123",
                null, null, null, null, 1L);
        });

        // When & Then - null password
        assertThrows(BusinessException.class, () -> {
            userService.createUser("EMP001", "John Doe", "john@tns.com", null,
                null, null, null, null, 1L);
        });
    }

    @Test
    void testGetUserById_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        User foundUser = userService.getUserById(1L);

        // Then
        assertNotNull(foundUser);
        assertEquals(testUser.getId(), foundUser.getId());
        assertEquals(testUser.getEmployeeId(), foundUser.getEmployeeId());
    }

    @Test
    void testGetUserById_NotFound_ThrowsException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.getUserById(999L);
        });

        assertEquals("User with ID 999 not found", exception.getMessage());
    }

    @Test
    void testGetUserByEmail_Success() {
        // Given
        when(userRepository.findByEmail("john.doe@tns.com")).thenReturn(Optional.of(testUser));

        // When
        User foundUser = userService.getUserByEmail("john.doe@tns.com");

        // Then
        assertNotNull(foundUser);
        assertEquals(testUser.getEmail(), foundUser.getEmail());
    }

    @Test
    void testGetUserByEmployeeId_Success() {
        // Given
        when(userRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(testUser));

        // When
        User foundUser = userService.getUserByEmployeeId("EMP001");

        // Then
        assertNotNull(foundUser);
        assertEquals(testUser.getEmployeeId(), foundUser.getEmployeeId());
    }

    @Test
    void testGetAllUsers_ReturnsPaginatedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        org.springframework.data.domain.Page<User> page =
            new org.springframework.data.domain.PageImpl<>(List.of(testUser), pageable, 1);
        when(userRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<User> result = userService.getAllUsers(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testUser.getId(), result.getContent().get(0).getId());
        verify(userRepository).findAll(pageable);
    }

    @Test
    void testGetAllActiveUsers_Success() {
        // Given
        User activeUser1 = new User("EMP001", "John Doe", "john@tns.com", "hash");
        activeUser1.setIsActive(true);
        
        User activeUser2 = new User("EMP002", "Jane Smith", "jane@tns.com", "hash");
        activeUser2.setIsActive(true);
        
        User inactiveUser = new User("EMP003", "Bob Johnson", "bob@tns.com", "hash");
        inactiveUser.setIsActive(false);

        when(userRepository.findAll()).thenReturn(Arrays.asList(activeUser1, activeUser2, inactiveUser));

        // When
        List<User> activeUsers = userService.getAllActiveUsers();

        // Then
        assertEquals(2, activeUsers.size());
        assertTrue(activeUsers.stream().allMatch(User::getIsActive));
    }

    @Test
    void testUpdateUser_Success() {
        // Given
        Long userId = 1L;
        String newFullName = "John Updated Doe";
        String newEmail = "john.updated@tns.com";
        String newDesignation = "Lead Engineer";
        Long updatedByUserId = 2L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail(newEmail)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User updatedUser = userService.updateUser(userId, newFullName, newEmail, null,
            newDesignation, null, null, updatedByUserId);

        // Then
        assertNotNull(updatedUser);
        verify(userRepository).save(testUser);
        verify(auditLogService).logAsync(eq(updatedByUserId), eq("USER_UPDATED"), eq("User"), 
            eq(userId), anyMap(), isNull());
    }

    @Test
    void testUpdateUser_WithPassword_Success() {
        // Given
        Long userId = 1L;
        String newPassword = "NewPass123";
        Long updatedByUserId = 2L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordHashingService.getPasswordStrengthError(newPassword)).thenReturn(null);
        when(passwordHashingService.hashPassword(newPassword)).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User updatedUser = userService.updateUser(userId, null, null, newPassword,
            null, null, null, updatedByUserId);

        // Then
        assertNotNull(updatedUser);
        verify(passwordHashingService).hashPassword(newPassword);
        verify(userRepository).save(testUser);
    }

    @Test
    void testUpdateUser_DuplicateEmail_ThrowsException() {
        // Given
        Long userId = 1L;
        String newEmail = "existing@tns.com";

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail(newEmail)).thenReturn(true);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.updateUser(userId, null, newEmail, null, null, null, null, 1L);
        });

        assertEquals("Email existing@tns.com is already in use", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testDeactivateUser_Success() {
        // Given
        Long userId = 1L;
        Long deactivatedByUserId = 2L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.deactivateUser(userId, deactivatedByUserId);

        // Then
        verify(userRepository).save(testUser);
        verify(auditLogService).logAsync(eq(deactivatedByUserId), eq("USER_DEACTIVATED"), 
            eq("User"), eq(userId), anyMap(), isNull());
    }

    @Test
    void testReactivateUser_Success() {
        // Given
        Long userId = 1L;
        Long reactivatedByUserId = 2L;
        testUser.setIsActive(false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.reactivateUser(userId, reactivatedByUserId);

        // Then
        verify(userRepository).save(testUser);
        verify(auditLogService).logAsync(eq(reactivatedByUserId), eq("USER_REACTIVATED"), 
            eq("User"), eq(userId), anyMap(), isNull());
    }

    @Test
    void testDeleteUser_Success() {
        // Given
        Long userId = 1L;
        Long deletedByUserId = 2L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When
        userService.deleteUser(userId, deletedByUserId);

        // Then
        verify(userRepository).delete(testUser);
        verify(auditLogService).logAsync(eq(deletedByUserId), eq("USER_DELETED"), 
            eq("User"), eq(userId), anyMap(), isNull());
    }

    @Test
    void testAssignRoles_Success() {
        // Given
        Long userId = 1L;
        Set<String> roleNames = Set.of("EMPLOYEE", "MANAGER");
        Long assignedByUserId = 2L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("EMPLOYEE")).thenReturn(Optional.of(employeeRole));
        when(roleRepository.findByName("MANAGER")).thenReturn(Optional.of(managerRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User updatedUser = userService.assignRoles(userId, roleNames, assignedByUserId);

        // Then
        assertNotNull(updatedUser);
        verify(userRepository).save(testUser);
        verify(auditLogService).logAsync(eq(assignedByUserId), eq("ROLES_ASSIGNED"), 
            eq("User"), eq(userId), anyMap(), isNull());
    }

    @Test
    void testAssignRoles_RoleNotFound_ThrowsException() {
        // Given
        Long userId = 1L;
        Set<String> roleNames = Set.of("INVALID_ROLE");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("INVALID_ROLE")).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.assignRoles(userId, roleNames, 1L);
        });

        assertEquals("Role INVALID_ROLE not found", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testAssignRoles_EmptyRoleSet_ThrowsException() {
        // Given
        Long userId = 1L;
        Set<String> emptyRoleNames = new HashSet<>();
        Long assignedByUserId = 2L;

        // No need to stub userRepository since validation happens before fetching user

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.assignRoles(userId, emptyRoleNames, assignedByUserId);
        });

        assertEquals("At least one role must be assigned to a user", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testAssignRoles_NullRoleSet_ThrowsException() {
        // Given
        Long userId = 1L;
        Long assignedByUserId = 2L;

        // No need to stub userRepository since validation happens before fetching user

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.assignRoles(userId, null, assignedByUserId);
        });

        assertEquals("At least one role must be assigned to a user", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testAddRole_Success() {
        // Given
        Long userId = 1L;
        String roleName = "MANAGER";
        Long assignedByUserId = 2L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName(roleName)).thenReturn(Optional.of(managerRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User updatedUser = userService.addRole(userId, roleName, assignedByUserId);

        // Then
        assertNotNull(updatedUser);
        verify(userRepository).save(testUser);
        verify(auditLogService).logAsync(eq(assignedByUserId), eq("ROLE_ADDED"), 
            eq("User"), eq(userId), anyMap(), isNull());
    }

    @Test
    void testAddRole_AlreadyHasRole_NoChanges() {
        // Given
        Long userId = 1L;
        String roleName = "EMPLOYEE"; // User already has this role
        Long assignedByUserId = 2L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName(roleName)).thenReturn(Optional.of(employeeRole));

        // When
        User updatedUser = userService.addRole(userId, roleName, assignedByUserId);

        // Then
        assertNotNull(updatedUser);
        verify(userRepository, never()).save(any(User.class)); // No save should occur
        verify(auditLogService, never()).logAsync(anyLong(), anyString(), anyString(), 
            anyLong(), anyMap(), any());
    }

    @Test
    void testRemoveRole_Success() {
        // Given
        Long userId = 1L;
        String roleName = "EMPLOYEE";
        Long revokedByUserId = 2L;
        
        // Add another role so user has more than one
        testUser.addRole(managerRole);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName(roleName)).thenReturn(Optional.of(employeeRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User updatedUser = userService.removeRole(userId, roleName, revokedByUserId);

        // Then
        assertNotNull(updatedUser);
        verify(userRepository).save(testUser);
        verify(auditLogService).logAsync(eq(revokedByUserId), eq("ROLE_REMOVED"), 
            eq("User"), eq(userId), anyMap(), isNull());
    }

    @Test
    void testRemoveRole_LastRole_ThrowsException() {
        // Given
        Long userId = 1L;
        String roleName = "EMPLOYEE"; // User only has this one role
        Long revokedByUserId = 2L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        // No need to stub roleRepository since validation happens before fetching role

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.removeRole(userId, roleName, revokedByUserId);
        });

        assertEquals("Cannot remove the last role from a user. At least one role must be assigned.", 
            exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRemoveRole_UserDoesNotHaveRole_ThrowsException() {
        // Given
        Long userId = 1L;
        String roleName = "HR"; // User doesn't have this role
        Long revokedByUserId = 2L;
        
        // Add another role so we pass the "at least one role" check
        testUser.addRole(managerRole);
        
        Role hrRole = new Role("HR");
        hrRole.setId(3);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("HR")).thenReturn(Optional.of(hrRole));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.removeRole(userId, roleName, revokedByUserId);
        });

        assertEquals("User does not have role HR", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testGetUsersByRole_Success() {
        // Given
        User user1 = new User("EMP001", "John Doe", "john@tns.com", "hash");
        user1.addRole(employeeRole);
        
        User user2 = new User("EMP002", "Jane Smith", "jane@tns.com", "hash");
        user2.addRole(employeeRole);
        user2.addRole(managerRole);
        
        User user3 = new User("EMP003", "Bob Johnson", "bob@tns.com", "hash");
        user3.addRole(managerRole);

        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2, user3));

        // When
        List<User> employeeUsers = userService.getUsersByRole("EMPLOYEE");

        // Then
        assertEquals(2, employeeUsers.size());
        assertTrue(employeeUsers.stream()
            .allMatch(u -> u.getRoles().stream()
                .anyMatch(r -> r.getName().equals("EMPLOYEE"))));
    }

    @Test
    void testGetUsersByDepartment_Success() {
        // Given
        User user1 = new User("EMP001", "John Doe", "john@tns.com", "hash");
        user1.setDepartment("Engineering");
        
        User user2 = new User("EMP002", "Jane Smith", "jane@tns.com", "hash");
        user2.setDepartment("Engineering");
        
        User user3 = new User("EMP003", "Bob Johnson", "bob@tns.com", "hash");
        user3.setDepartment("HR");

        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2, user3));

        // When
        List<User> engineeringUsers = userService.getUsersByDepartment("Engineering");

        // Then
        assertEquals(2, engineeringUsers.size());
        assertTrue(engineeringUsers.stream()
            .allMatch(u -> "Engineering".equals(u.getDepartment())));
    }

    @Test
    void testGetDirectReports_Success() {
        // Given
        Long managerId = 1L;
        
        User report1 = new User("EMP002", "Jane Smith", "jane@tns.com", "hash");
        report1.setManager(testUser);
        
        User report2 = new User("EMP003", "Bob Johnson", "bob@tns.com", "hash");
        report2.setManager(testUser);
        
        User otherUser = new User("EMP004", "Alice Brown", "alice@tns.com", "hash");

        when(userRepository.findAll()).thenReturn(Arrays.asList(report1, report2, otherUser));

        // When
        List<User> directReports = userService.getDirectReports(managerId);

        // Then
        assertEquals(2, directReports.size());
        assertTrue(directReports.stream()
            .allMatch(u -> u.getManager() != null && u.getManager().getId().equals(managerId)));
    }

    @Test
    void testGetUserRoles_Success() {
        // Given
        Long userId = 1L;
        testUser.addRole(managerRole);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When
        Set<String> roles = userService.getUserRoles(userId);

        // Then
        assertNotNull(roles);
        assertEquals(2, roles.size());
        assertTrue(roles.contains("EMPLOYEE"));
        assertTrue(roles.contains("MANAGER"));
    }

    @Test
    void testHasRole_UserHasRole_ReturnsTrue() {
        // Given
        Long userId = 1L;
        String roleName = "EMPLOYEE";

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When
        boolean hasRole = userService.hasRole(userId, roleName);

        // Then
        assertTrue(hasRole);
    }

    @Test
    void testHasRole_UserDoesNotHaveRole_ReturnsFalse() {
        // Given
        Long userId = 1L;
        String roleName = "MANAGER";

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When
        boolean hasRole = userService.hasRole(userId, roleName);

        // Then
        assertFalse(hasRole);
    }

    @Test
    void testValidateUserHasRoles_UserHasRoles_NoException() {
        // Given
        Long userId = 1L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> userService.validateUserHasRoles(userId));
    }

    @Test
    void testValidateUserHasRoles_UserHasNoRoles_ThrowsException() {
        // Given
        Long userId = 1L;
        User userWithoutRoles = new User("EMP002", "Jane Smith", "jane@tns.com", "hash");
        userWithoutRoles.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(userWithoutRoles));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.validateUserHasRoles(userId);
        });

        assertEquals("User must have at least one role assigned", exception.getMessage());
    }
}
