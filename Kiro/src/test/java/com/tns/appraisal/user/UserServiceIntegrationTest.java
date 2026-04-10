package com.tns.appraisal.user;

import com.tns.appraisal.auth.Role;
import com.tns.appraisal.auth.RoleRepository;
import com.tns.appraisal.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for UserService with actual database.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Role employeeRole;
    private Role managerRole;

    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        userRepository.deleteAll();

        // Ensure roles exist
        employeeRole = roleRepository.findByName("EMPLOYEE")
            .orElseGet(() -> roleRepository.save(new Role("EMPLOYEE")));
        
        managerRole = roleRepository.findByName("MANAGER")
            .orElseGet(() -> roleRepository.save(new Role("MANAGER")));
    }

    @Test
    void testCreateAndRetrieveUser() {
        // Given
        String employeeId = "TEST001";
        String fullName = "Test User";
        String email = "test.user@tns.com";
        String password = "TestPass123";
        Set<String> roleNames = Set.of("EMPLOYEE");

        // When
        User createdUser = userService.createUser(employeeId, fullName, email, password,
            "Software Engineer", "Engineering", null, roleNames, 1L);

        // Then
        assertNotNull(createdUser);
        assertNotNull(createdUser.getId());
        assertEquals(employeeId, createdUser.getEmployeeId());
        assertEquals(fullName, createdUser.getFullName());
        assertEquals(email, createdUser.getEmail());
        assertTrue(createdUser.getIsActive());

        // Verify retrieval
        User retrievedUser = userService.getUserById(createdUser.getId());
        assertEquals(createdUser.getId(), retrievedUser.getId());
        assertEquals(createdUser.getEmployeeId(), retrievedUser.getEmployeeId());
    }

    @Test
    void testCreateUserWithManager() {
        // Given - Create manager first
        User manager = userService.createUser("MGR001", "Manager User", "manager@tns.com",
            "ManagerPass123", "Manager", "Engineering", null, Set.of("MANAGER"), 1L);

        // When - Create employee with manager
        User employee = userService.createUser("EMP001", "Employee User", "employee@tns.com",
            "EmployeePass123", "Engineer", "Engineering", manager.getId(), Set.of("EMPLOYEE"), 1L);

        // Then
        assertNotNull(employee.getManager());
        assertEquals(manager.getId(), employee.getManager().getId());
        assertEquals(manager.getEmployeeId(), employee.getManager().getEmployeeId());
    }

    @Test
    void testUpdateUser() {
        // Given
        User user = userService.createUser("TEST002", "Original Name", "original@tns.com",
            "TestPass123", "Engineer", "Engineering", null, Set.of("EMPLOYEE"), 1L);

        // When
        String newName = "Updated Name";
        String newEmail = "updated@tns.com";
        User updatedUser = userService.updateUser(user.getId(), newName, newEmail, null,
            "Senior Engineer", "R&D", null, 1L);

        // Then
        assertEquals(newName, updatedUser.getFullName());
        assertEquals(newEmail, updatedUser.getEmail());
        assertEquals("Senior Engineer", updatedUser.getDesignation());
        assertEquals("R&D", updatedUser.getDepartment());
    }

    @Test
    void testDeactivateAndReactivateUser() {
        // Given
        User user = userService.createUser("TEST003", "Test User", "test3@tns.com",
            "TestPass123", null, null, null, Set.of("EMPLOYEE"), 1L);
        assertTrue(user.getIsActive());

        // When - Deactivate
        userService.deactivateUser(user.getId(), 1L);

        // Then
        User deactivatedUser = userService.getUserById(user.getId());
        assertFalse(deactivatedUser.getIsActive());

        // When - Reactivate
        userService.reactivateUser(user.getId(), 1L);

        // Then
        User reactivatedUser = userService.getUserById(user.getId());
        assertTrue(reactivatedUser.getIsActive());
    }

    @Test
    void testAssignAndRemoveRoles() {
        // Given
        User user = userService.createUser("TEST004", "Test User", "test4@tns.com",
            "TestPass123", null, null, null, Set.of("EMPLOYEE"), 1L);
        assertEquals(1, user.getRoles().size());

        // When - Assign multiple roles
        User updatedUser = userService.assignRoles(user.getId(), Set.of("EMPLOYEE", "MANAGER"), 1L);

        // Then
        assertEquals(2, updatedUser.getRoles().size());
        assertTrue(updatedUser.getRoles().stream()
            .anyMatch(r -> r.getName().equals("EMPLOYEE")));
        assertTrue(updatedUser.getRoles().stream()
            .anyMatch(r -> r.getName().equals("MANAGER")));

        // When - Remove a role
        User afterRemoval = userService.removeRole(user.getId(), "MANAGER", 1L);

        // Then
        assertEquals(1, afterRemoval.getRoles().size());
        assertTrue(afterRemoval.getRoles().stream()
            .anyMatch(r -> r.getName().equals("EMPLOYEE")));
        assertFalse(afterRemoval.getRoles().stream()
            .anyMatch(r -> r.getName().equals("MANAGER")));
    }

    @Test
    void testGetUsersByRole() {
        // Given
        userService.createUser("EMP001", "Employee 1", "emp1@tns.com",
            "TestPass123", null, null, null, Set.of("EMPLOYEE"), 1L);
        userService.createUser("EMP002", "Employee 2", "emp2@tns.com",
            "TestPass123", null, null, null, Set.of("EMPLOYEE"), 1L);
        userService.createUser("MGR001", "Manager 1", "mgr1@tns.com",
            "TestPass123", null, null, null, Set.of("MANAGER"), 1L);

        // When
        List<User> employees = userService.getUsersByRole("EMPLOYEE");
        List<User> managers = userService.getUsersByRole("MANAGER");

        // Then
        assertEquals(2, employees.size());
        assertEquals(1, managers.size());
    }

    @Test
    void testGetUsersByDepartment() {
        // Given
        userService.createUser("ENG001", "Engineer 1", "eng1@tns.com",
            "TestPass123", null, "Engineering", null, Set.of("EMPLOYEE"), 1L);
        userService.createUser("ENG002", "Engineer 2", "eng2@tns.com",
            "TestPass123", null, "Engineering", null, Set.of("EMPLOYEE"), 1L);
        userService.createUser("HR001", "HR Person", "hr1@tns.com",
            "TestPass123", null, "HR", null, Set.of("HR"), 1L);

        // When
        List<User> engineeringUsers = userService.getUsersByDepartment("Engineering");
        List<User> hrUsers = userService.getUsersByDepartment("HR");

        // Then
        assertEquals(2, engineeringUsers.size());
        assertEquals(1, hrUsers.size());
    }

    @Test
    void testGetDirectReports() {
        // Given
        User manager = userService.createUser("MGR001", "Manager", "manager@tns.com",
            "TestPass123", null, null, null, Set.of("MANAGER"), 1L);
        
        userService.createUser("EMP001", "Employee 1", "emp1@tns.com",
            "TestPass123", null, null, manager.getId(), Set.of("EMPLOYEE"), 1L);
        userService.createUser("EMP002", "Employee 2", "emp2@tns.com",
            "TestPass123", null, null, manager.getId(), Set.of("EMPLOYEE"), 1L);
        userService.createUser("EMP003", "Employee 3", "emp3@tns.com",
            "TestPass123", null, null, null, Set.of("EMPLOYEE"), 1L);

        // When
        List<User> directReports = userService.getDirectReports(manager.getId());

        // Then
        assertEquals(2, directReports.size());
        assertTrue(directReports.stream()
            .allMatch(u -> u.getManager() != null && u.getManager().getId().equals(manager.getId())));
    }

    @Test
    void testDuplicateEmployeeId_ThrowsException() {
        // Given
        userService.createUser("DUP001", "User 1", "user1@tns.com",
            "TestPass123", null, null, null, Set.of("EMPLOYEE"), 1L);

        // When & Then
        assertThrows(BusinessException.class, () -> {
            userService.createUser("DUP001", "User 2", "user2@tns.com",
                "TestPass123", null, null, null, Set.of("EMPLOYEE"), 1L);
        });
    }

    @Test
    void testDuplicateEmail_ThrowsException() {
        // Given
        userService.createUser("USER001", "User 1", "duplicate@tns.com",
            "TestPass123", null, null, null, Set.of("EMPLOYEE"), 1L);

        // When & Then
        assertThrows(BusinessException.class, () -> {
            userService.createUser("USER002", "User 2", "duplicate@tns.com",
                "TestPass123", null, null, null, Set.of("EMPLOYEE"), 1L);
        });
    }

    @Test
    void testGetAllActiveUsers() {
        // Given
        User user1 = userService.createUser("ACT001", "Active 1", "active1@tns.com",
            "TestPass123", null, null, null, Set.of("EMPLOYEE"), 1L);
        User user2 = userService.createUser("ACT002", "Active 2", "active2@tns.com",
            "TestPass123", null, null, null, Set.of("EMPLOYEE"), 1L);
        User user3 = userService.createUser("ACT003", "To Deactivate", "inactive@tns.com",
            "TestPass123", null, null, null, Set.of("EMPLOYEE"), 1L);
        
        userService.deactivateUser(user3.getId(), 1L);

        // When
        List<User> activeUsers = userService.getAllActiveUsers();

        // Then
        assertEquals(2, activeUsers.size());
        assertTrue(activeUsers.stream().allMatch(User::getIsActive));
        assertTrue(activeUsers.stream().anyMatch(u -> u.getId().equals(user1.getId())));
        assertTrue(activeUsers.stream().anyMatch(u -> u.getId().equals(user2.getId())));
        assertFalse(activeUsers.stream().anyMatch(u -> u.getId().equals(user3.getId())));
    }
}
