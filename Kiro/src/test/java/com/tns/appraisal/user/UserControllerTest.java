package com.tns.appraisal.user;

import com.tns.appraisal.auth.AuthService;
import com.tns.appraisal.auth.Role;
import com.tns.appraisal.user.dto.AssignRolesRequest;
import com.tns.appraisal.user.dto.CreateUserRequest;
import com.tns.appraisal.user.dto.UpdateUserRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserController.
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AuthService authService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private UserController userController;

    private User adminUser;
    private User testUser;
    private Role employeeRole;

    @BeforeEach
    void setUp() {
        // Setup admin user
        adminUser = new User("ADMIN001", "Admin User", "admin@tns.com", "hashedPassword");
        adminUser.setId(1L);
        Role adminRole = new Role("ADMIN");
        adminRole.setId(4);
        adminUser.addRole(adminRole);

        // Setup test user
        testUser = new User("EMP001", "John Doe", "john.doe@tns.com", "hashedPassword123");
        testUser.setId(2L);
        testUser.setDesignation("Software Engineer");
        testUser.setDepartment("Engineering");
        testUser.setIsActive(true);

        employeeRole = new Role("EMPLOYEE");
        employeeRole.setId(1);
        testUser.addRole(employeeRole);
    }

    @Test
    void testCreateUser_Success() {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        request.setEmployeeId("EMP002");
        request.setFullName("Jane Smith");
        request.setEmail("jane.smith@tns.com");
        request.setPassword("TestPass123");
        request.setDesignation("Senior Engineer");
        request.setDepartment("Engineering");
        request.setRoles(Set.of("EMPLOYEE"));

        when(authService.getCurrentUser(httpRequest)).thenReturn(adminUser);
        when(userService.createUser(anyString(), anyString(), anyString(), anyString(),
            anyString(), anyString(), any(), any(), anyLong())).thenReturn(testUser);

        // When
        var response = userController.createUser(request, httpRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("User created successfully", response.getBody().getMessage());
        assertNotNull(response.getBody().getData());
        assertEquals(testUser.getId(), response.getBody().getData().getId());

        verify(userService).createUser(
            request.getEmployeeId(),
            request.getFullName(),
            request.getEmail(),
            request.getPassword(),
            request.getDesignation(),
            request.getDepartment(),
            request.getManagerId(),
            request.getRoles(),
            adminUser.getId()
        );
    }

    @Test
    void testGetUserById_Success() {
        // Given
        Long userId = 2L;
        when(userService.getUserById(userId)).thenReturn(testUser);

        // When
        var response = userController.getUserById(userId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertNotNull(response.getBody().getData());
        assertEquals(testUser.getId(), response.getBody().getData().getId());
        assertEquals(testUser.getEmployeeId(), response.getBody().getData().getEmployeeId());

        verify(userService).getUserById(userId);
    }

    @Test
    void testListUsers_Success() {
        // Given
        List<User> users = Arrays.asList(testUser);
        Page<User> userPage = new PageImpl<>(users, PageRequest.of(0, 20), 1);
        when(userService.searchUsers(any(), any(), any(), any(), any())).thenReturn(userPage);

        // When
        var response = userController.listUsers(null, null, null, null, 0, 20, "fullName", "ASC");

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertNotNull(response.getBody().getData());
        assertEquals(1, response.getBody().getData().getContent().size());
        assertEquals(testUser.getId(), response.getBody().getData().getContent().get(0).getId());

        verify(userService).searchUsers(any(), any(), any(), any(), any());
    }

    @Test
    void testUpdateUser_Success() {
        // Given
        Long userId = 2L;
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("John Updated");
        request.setDesignation("Lead Engineer");

        when(authService.getCurrentUser(httpRequest)).thenReturn(adminUser);
        when(userService.updateUser(anyLong(), anyString(), any(), any(), anyString(),
            any(), any(), anyLong())).thenReturn(testUser);

        // When
        var response = userController.updateUser(userId, request, httpRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("User updated successfully", response.getBody().getMessage());

        verify(userService).updateUser(
            userId,
            request.getFullName(),
            request.getEmail(),
            request.getPassword(),
            request.getDesignation(),
            request.getDepartment(),
            request.getManagerId(),
            adminUser.getId()
        );
    }

    @Test
    void testDeactivateUser_Success() {
        // Given
        Long userId = 2L;
        when(authService.getCurrentUser(httpRequest)).thenReturn(adminUser);
        doNothing().when(userService).deactivateUser(userId, adminUser.getId());

        // When
        var response = userController.deactivateUser(userId, httpRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("User deactivated successfully", response.getBody().getMessage());

        verify(userService).deactivateUser(userId, adminUser.getId());
    }

    @Test
    void testAssignRoles_Success() {
        // Given
        Long userId = 2L;
        AssignRolesRequest request = new AssignRolesRequest(Set.of("EMPLOYEE", "MANAGER"));

        when(authService.getCurrentUser(httpRequest)).thenReturn(adminUser);
        when(userService.assignRoles(anyLong(), any(), anyLong())).thenReturn(testUser);

        // When
        var response = userController.assignRoles(userId, request, httpRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Roles assigned successfully", response.getBody().getMessage());

        verify(userService).assignRoles(userId, request.getRoles(), adminUser.getId());
    }

    @Test
    void testAddRole_Success() {
        // Given
        Long userId = 2L;
        String roleName = "MANAGER";

        when(authService.getCurrentUser(httpRequest)).thenReturn(adminUser);
        when(userService.addRole(anyLong(), anyString(), anyLong())).thenReturn(testUser);

        // When
        var response = userController.addRole(userId, roleName, httpRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Role added successfully", response.getBody().getMessage());

        verify(userService).addRole(userId, roleName, adminUser.getId());
    }

    @Test
    void testRemoveRole_Success() {
        // Given
        Long userId = 2L;
        String roleName = "EMPLOYEE";

        when(authService.getCurrentUser(httpRequest)).thenReturn(adminUser);
        when(userService.removeRole(anyLong(), anyString(), anyLong())).thenReturn(testUser);

        // When
        var response = userController.removeRole(userId, roleName, httpRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Role removed successfully", response.getBody().getMessage());

        verify(userService).removeRole(userId, roleName, adminUser.getId());
    }

    @Test
    void testGetDirectReports_Success() {
        // Given
        Long managerId = 1L;
        List<User> directReports = Arrays.asList(testUser);

        when(userService.getDirectReports(managerId)).thenReturn(directReports);

        // When
        var response = userController.getDirectReports(managerId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertNotNull(response.getBody().getData());
        assertEquals(1, response.getBody().getData().size());
        assertEquals(testUser.getId(), response.getBody().getData().get(0).getId());

        verify(userService).getDirectReports(managerId);
    }
}
