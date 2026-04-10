package com.tns.appraisal.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tns.appraisal.auth.dto.LoginRequest;
import com.tns.appraisal.user.User;
import com.tns.appraisal.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Property-based test for Role-Based Access Enforcement (Property 3).
 * 
 * Property: For any API endpoint and any authenticated user, the response SHALL be 403 Forbidden 
 * if the user's role(s) are not in the authorized set for that endpoint, and SHALL be a successful 
 * response if the user's role(s) are authorized.
 * 
 * Validates: Requirements 2.2, 2.4
 * 
 * This test uses JUnit's @RepeatedTest to run 100 iterations with randomly generated
 * test scenarios, verifying that role-based access control is enforced correctly across
 * all protected endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RoleBasedAccessEnforcementPropertyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final Random random = new Random();
    
    private Map<String, Role> roleCache;

    /**
     * Protected endpoints with their required roles
     */
    private static final List<ProtectedEndpoint> PROTECTED_ENDPOINTS = Arrays.asList(
        // User Management - ADMIN only
        new ProtectedEndpoint("GET", "/api/users", Set.of("ADMIN")),
        new ProtectedEndpoint("GET", "/api/users/1", Set.of("ADMIN")),
        new ProtectedEndpoint("POST", "/api/users", Set.of("ADMIN")),
        new ProtectedEndpoint("PUT", "/api/users/1", Set.of("ADMIN")),
        new ProtectedEndpoint("DELETE", "/api/users/1", Set.of("ADMIN")),
        new ProtectedEndpoint("POST", "/api/users/1/roles", Set.of("ADMIN")),
        new ProtectedEndpoint("POST", "/api/users/1/roles/EMPLOYEE", Set.of("ADMIN")),
        new ProtectedEndpoint("DELETE", "/api/users/1/roles/EMPLOYEE", Set.of("ADMIN")),
        new ProtectedEndpoint("GET", "/api/users/1/direct-reports", Set.of("ADMIN")),
        new ProtectedEndpoint("GET", "/api/users/1/roles", Set.of("ADMIN")),
        new ProtectedEndpoint("GET", "/api/users/1/roles/EMPLOYEE/check", Set.of("ADMIN")),
        
        // Reporting Hierarchy - HR or ADMIN
        new ProtectedEndpoint("POST", "/api/reporting-hierarchy/assign", Set.of("HR", "ADMIN")),
        new ProtectedEndpoint("POST", "/api/reporting-hierarchy/change", Set.of("HR", "ADMIN")),
        new ProtectedEndpoint("POST", "/api/reporting-hierarchy/end", Set.of("HR", "ADMIN")),
        new ProtectedEndpoint("GET", "/api/reporting-hierarchy/employee/1/history", Set.of("HR", "ADMIN")),
        new ProtectedEndpoint("GET", "/api/reporting-hierarchy/employee/1/manager-on-date", Set.of("HR", "ADMIN")),
        new ProtectedEndpoint("GET", "/api/reporting-hierarchy/manager/1/direct-reports-on-date", Set.of("HR", "ADMIN")),
        new ProtectedEndpoint("GET", "/api/reporting-hierarchy/active", Set.of("HR", "ADMIN")),
        
        // Reporting Hierarchy - HR, ADMIN, or MANAGER
        new ProtectedEndpoint("GET", "/api/reporting-hierarchy/employee/1/current-manager", Set.of("HR", "ADMIN", "MANAGER")),
        new ProtectedEndpoint("GET", "/api/reporting-hierarchy/manager/1/direct-reports", Set.of("HR", "ADMIN", "MANAGER"))
    );

    private static final String[] ALL_ROLES = {"EMPLOYEE", "MANAGER", "HR", "ADMIN"};

    @BeforeEach
    void setUp() {
        // Clean up any test users from previous runs
        userRepository.deleteAll();
        
        // Cache roles for quick access
        roleCache = new HashMap<>();
        for (String roleName : ALL_ROLES) {
            roleRepository.findByName(roleName).ifPresent(role -> roleCache.put(roleName, role));
        }
    }

    @AfterEach
    void tearDown() {
        // Clean up test users after each test
        userRepository.deleteAll();
    }

    /**
     * Property 3: Role-Based Access Enforcement - Unauthorized Access Returns 403
     * 
     * Tests that when a user with an unauthorized role attempts to access a protected endpoint,
     * the system returns 403 Forbidden. Runs 100 iterations with random endpoint and role combinations.
     */
    @RepeatedTest(100)
    void unauthorizedRoleReceives403Forbidden() throws Exception {
        // Arrange: Select a random protected endpoint
        ProtectedEndpoint endpoint = PROTECTED_ENDPOINTS.get(random.nextInt(PROTECTED_ENDPOINTS.size()));
        
        // Select a role that is NOT authorized for this endpoint
        String unauthorizedRole = selectUnauthorizedRole(endpoint.requiredRoles);
        
        // Skip if no unauthorized role exists (all roles are authorized)
        if (unauthorizedRole == null) {
            return;
        }
        
        // Create user with unauthorized role
        User user = createTestUserWithRole(unauthorizedRole);
        MockHttpSession session = authenticateUser(user);
        
        // Act & Assert: Request with unauthorized role should return 403
        performRequestWithSession(endpoint, session)
            .andExpect(status().isForbidden());
        
        // Cleanup
        userRepository.delete(user);
    }

    /**
     * Property 3: Role-Based Access Enforcement - Authorized Access Succeeds
     * 
     * Tests that when a user with an authorized role attempts to access a protected endpoint,
     * the system allows access (returns 2xx or 4xx for validation errors, but NOT 403).
     * Runs 100 iterations with random endpoint and role combinations.
     */
    @RepeatedTest(100)
    void authorizedRoleDoesNotReceive403() throws Exception {
        // Arrange: Select a random protected endpoint
        ProtectedEndpoint endpoint = PROTECTED_ENDPOINTS.get(random.nextInt(PROTECTED_ENDPOINTS.size()));
        
        // Select a role that IS authorized for this endpoint
        String authorizedRole = selectAuthorizedRole(endpoint.requiredRoles);
        
        // Create user with authorized role
        User user = createTestUserWithRole(authorizedRole);
        MockHttpSession session = authenticateUser(user);
        
        // Act: Request with authorized role
        MvcResult result = performRequestWithSession(endpoint, session)
            .andReturn();
        
        // Assert: Should NOT return 403 (may return 404, 400, or 200 depending on endpoint logic)
        int status = result.getResponse().getStatus();
        assertThat(status).isNotEqualTo(403);
        
        // Cleanup
        userRepository.delete(user);
    }

    /**
     * Property 3: Role-Based Access Enforcement - ADMIN Role Access
     * 
     * Tests that ADMIN role can access all ADMIN-only endpoints.
     * Runs 100 iterations with random ADMIN-only endpoints.
     */
    @RepeatedTest(100)
    void adminRoleCanAccessAdminEndpoints() throws Exception {
        // Arrange: Select a random ADMIN-only endpoint
        List<ProtectedEndpoint> adminEndpoints = PROTECTED_ENDPOINTS.stream()
            .filter(ep -> ep.requiredRoles.equals(Set.of("ADMIN")))
            .toList();
        
        if (adminEndpoints.isEmpty()) {
            return;
        }
        
        ProtectedEndpoint endpoint = adminEndpoints.get(random.nextInt(adminEndpoints.size()));
        
        // Create user with ADMIN role
        User adminUser = createTestUserWithRole("ADMIN");
        MockHttpSession session = authenticateUser(adminUser);
        
        // Act: Request with ADMIN role
        MvcResult result = performRequestWithSession(endpoint, session)
            .andReturn();
        
        // Assert: Should NOT return 403
        int status = result.getResponse().getStatus();
        assertThat(status).isNotEqualTo(403);
        
        // Cleanup
        userRepository.delete(adminUser);
    }

    /**
     * Property 3: Role-Based Access Enforcement - EMPLOYEE Role Denied Admin Access
     * 
     * Tests that EMPLOYEE role cannot access ADMIN-only endpoints.
     * Runs 100 iterations with random ADMIN-only endpoints.
     */
    @RepeatedTest(100)
    void employeeRoleCannotAccessAdminEndpoints() throws Exception {
        // Arrange: Select a random ADMIN-only endpoint
        List<ProtectedEndpoint> adminEndpoints = PROTECTED_ENDPOINTS.stream()
            .filter(ep -> ep.requiredRoles.equals(Set.of("ADMIN")))
            .toList();
        
        if (adminEndpoints.isEmpty()) {
            return;
        }
        
        ProtectedEndpoint endpoint = adminEndpoints.get(random.nextInt(adminEndpoints.size()));
        
        // Create user with EMPLOYEE role only
        User employeeUser = createTestUserWithRole("EMPLOYEE");
        MockHttpSession session = authenticateUser(employeeUser);
        
        // Act & Assert: Request with EMPLOYEE role should return 403
        performRequestWithSession(endpoint, session)
            .andExpect(status().isForbidden());
        
        // Cleanup
        userRepository.delete(employeeUser);
    }

    /**
     * Property 3: Role-Based Access Enforcement - HR Role Access
     * 
     * Tests that HR role can access HR-authorized endpoints but not ADMIN-only endpoints.
     * Runs 100 iterations with random endpoints.
     */
    @RepeatedTest(100)
    void hrRoleAccessControlIsEnforced() throws Exception {
        // Create user with HR role
        User hrUser = createTestUserWithRole("HR");
        MockHttpSession session = authenticateUser(hrUser);
        
        // Test HR-authorized endpoint (should succeed)
        List<ProtectedEndpoint> hrEndpoints = PROTECTED_ENDPOINTS.stream()
            .filter(ep -> ep.requiredRoles.contains("HR"))
            .toList();
        
        if (!hrEndpoints.isEmpty()) {
            ProtectedEndpoint hrEndpoint = hrEndpoints.get(random.nextInt(hrEndpoints.size()));
            MvcResult result = performRequestWithSession(hrEndpoint, session)
                .andReturn();
            assertThat(result.getResponse().getStatus()).isNotEqualTo(403);
        }
        
        // Test ADMIN-only endpoint (should fail with 403)
        List<ProtectedEndpoint> adminOnlyEndpoints = PROTECTED_ENDPOINTS.stream()
            .filter(ep -> ep.requiredRoles.equals(Set.of("ADMIN")))
            .toList();
        
        if (!adminOnlyEndpoints.isEmpty()) {
            ProtectedEndpoint adminEndpoint = adminOnlyEndpoints.get(random.nextInt(adminOnlyEndpoints.size()));
            performRequestWithSession(adminEndpoint, session)
                .andExpect(status().isForbidden());
        }
        
        // Cleanup
        userRepository.delete(hrUser);
    }

    /**
     * Property 3: Role-Based Access Enforcement - Multiple Roles
     * 
     * Tests that a user with multiple roles can access endpoints authorized for any of their roles.
     * Runs 100 iterations with random role combinations.
     */
    @RepeatedTest(100)
    void userWithMultipleRolesCanAccessAuthorizedEndpoints() throws Exception {
        // Arrange: Create user with multiple roles (e.g., EMPLOYEE and MANAGER)
        User user = createTestUserWithRoles(Arrays.asList("EMPLOYEE", "MANAGER"));
        MockHttpSession session = authenticateUser(user);
        
        // Select an endpoint that requires MANAGER role
        List<ProtectedEndpoint> managerEndpoints = PROTECTED_ENDPOINTS.stream()
            .filter(ep -> ep.requiredRoles.contains("MANAGER"))
            .toList();
        
        if (!managerEndpoints.isEmpty()) {
            ProtectedEndpoint endpoint = managerEndpoints.get(random.nextInt(managerEndpoints.size()));
            
            // Act: Request with MANAGER role (user has EMPLOYEE + MANAGER)
            MvcResult result = performRequestWithSession(endpoint, session)
                .andReturn();
            
            // Assert: Should NOT return 403
            assertThat(result.getResponse().getStatus()).isNotEqualTo(403);
        }
        
        // Cleanup
        userRepository.delete(user);
    }

    // ==================== Helper Methods ====================

    private User createTestUserWithRole(String roleName) {
        return createTestUserWithRoles(Collections.singletonList(roleName));
    }

    private User createTestUserWithRoles(List<String> roleNames) {
        String email = generateRandomEmail();
        String employeeId = generateRandomEmployeeId();
        String password = "password123";
        
        User user = new User();
        user.setEmail(email);
        user.setEmployeeId(employeeId);
        user.setFullName("Test User " + employeeId);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDesignation("Software Engineer");
        user.setDepartment("Engineering");
        user.setIsActive(true);
        
        // Add roles
        for (String roleName : roleNames) {
            Role role = roleCache.get(roleName);
            if (role != null) {
                user.addRole(role);
            }
        }
        
        return userRepository.save(user);
    }

    private MockHttpSession authenticateUser(User user) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginIdentifier(user.getEmail());
        loginRequest.setPassword("password123");
        
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();
        
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private org.springframework.test.web.servlet.ResultActions performRequestWithSession(
            ProtectedEndpoint endpoint, MockHttpSession session) throws Exception {
        
        switch (endpoint.method) {
            case "GET":
                return mockMvc.perform(get(endpoint.path)
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON));
                
            case "POST":
                return mockMvc.perform(post(endpoint.path)
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"));
                
            case "PUT":
                return mockMvc.perform(put(endpoint.path)
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"));
                
            case "DELETE":
                return mockMvc.perform(delete(endpoint.path)
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON));
                
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + endpoint.method);
        }
    }

    private String selectUnauthorizedRole(Set<String> requiredRoles) {
        List<String> unauthorizedRoles = Arrays.stream(ALL_ROLES)
            .filter(role -> !requiredRoles.contains(role))
            .toList();
        
        if (unauthorizedRoles.isEmpty()) {
            return null;
        }
        
        return unauthorizedRoles.get(random.nextInt(unauthorizedRoles.size()));
    }

    private String selectAuthorizedRole(Set<String> requiredRoles) {
        List<String> authorizedRoles = new ArrayList<>(requiredRoles);
        return authorizedRoles.get(random.nextInt(authorizedRoles.size()));
    }

    private String generateRandomEmail() {
        return "user" + UUID.randomUUID().toString().substring(0, 8) + "@tns.com";
    }

    private String generateRandomEmployeeId() {
        return "EMP" + String.format("%06d", random.nextInt(1000000));
    }

    // ==================== Helper Classes ====================

    /**
     * Represents a protected endpoint with HTTP method, path, and required roles
     */
    private static class ProtectedEndpoint {
        final String method;
        final String path;
        final Set<String> requiredRoles;

        ProtectedEndpoint(String method, String path, Set<String> requiredRoles) {
            this.method = method;
            this.path = path;
            this.requiredRoles = requiredRoles;
        }
    }
}
