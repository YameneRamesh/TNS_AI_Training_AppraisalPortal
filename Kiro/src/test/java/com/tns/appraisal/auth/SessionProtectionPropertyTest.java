package com.tns.appraisal.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Property-based test for Session Protection (Property 2).
 * 
 * Property: For any protected API endpoint, a request made without a valid active session 
 * SHALL receive a 401 Unauthorized response and SHALL NOT return any protected data.
 * 
 * Validates: Requirements 1.5
 * 
 * This test uses JUnit's @RepeatedTest to run 100 iterations with randomly selected
 * protected endpoints, verifying that all protected resources require authentication.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SessionProtectionPropertyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final Random random = new Random();

    /**
     * Protected endpoints to test - covers all major API routes that require authentication
     */
    private static final ProtectedEndpoint[] PROTECTED_ENDPOINTS = {
        // Auth endpoints (except /login which is public)
        new ProtectedEndpoint("GET", "/api/auth/me"),
        new ProtectedEndpoint("POST", "/api/auth/logout"),
        
        // User management endpoints (Admin only)
        new ProtectedEndpoint("GET", "/api/users"),
        new ProtectedEndpoint("GET", "/api/users/1"),
        new ProtectedEndpoint("POST", "/api/users"),
        new ProtectedEndpoint("PUT", "/api/users/1"),
        new ProtectedEndpoint("DELETE", "/api/users/1"),
        new ProtectedEndpoint("POST", "/api/users/1/roles"),
        new ProtectedEndpoint("POST", "/api/users/1/roles/EMPLOYEE"),
        new ProtectedEndpoint("DELETE", "/api/users/1/roles/EMPLOYEE"),
        new ProtectedEndpoint("GET", "/api/users/1/direct-reports"),
        new ProtectedEndpoint("GET", "/api/users/1/roles"),
        new ProtectedEndpoint("GET", "/api/users/1/roles/EMPLOYEE/check"),
        
        // Reporting hierarchy endpoints
        new ProtectedEndpoint("POST", "/api/reporting-hierarchy/assign"),
        new ProtectedEndpoint("POST", "/api/reporting-hierarchy/change"),
        new ProtectedEndpoint("POST", "/api/reporting-hierarchy/end"),
        new ProtectedEndpoint("GET", "/api/reporting-hierarchy/employee/1/current-manager"),
        new ProtectedEndpoint("GET", "/api/reporting-hierarchy/manager/1/direct-reports"),
        new ProtectedEndpoint("GET", "/api/reporting-hierarchy/employee/1/history"),
        new ProtectedEndpoint("GET", "/api/reporting-hierarchy/employee/1/manager-on-date"),
        new ProtectedEndpoint("GET", "/api/reporting-hierarchy/manager/1/direct-reports-on-date"),
        new ProtectedEndpoint("GET", "/api/reporting-hierarchy/active"),
        
        // Audit log endpoints
        new ProtectedEndpoint("GET", "/api/audit-logs"),
        new ProtectedEndpoint("GET", "/api/audit-logs/1")
    };

    /**
     * Property 2: Session Protection - Random Protected Endpoint
     * 
     * Tests that any protected endpoint returns 401 Unauthorized when accessed without
     * a valid session. Runs 100 iterations, randomly selecting endpoints to test.
     */
    @RepeatedTest(100)
    void protectedEndpointReturns401WithoutSession() throws Exception {
        // Arrange: Select a random protected endpoint
        ProtectedEndpoint endpoint = PROTECTED_ENDPOINTS[random.nextInt(PROTECTED_ENDPOINTS.length)];
        
        // Act & Assert: Request without session should return 401
        switch (endpoint.method) {
            case "GET":
                mockMvc.perform(get(endpoint.path)
                        .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
                break;
                
            case "POST":
                mockMvc.perform(post(endpoint.path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                    .andExpect(status().isUnauthorized());
                break;
                
            case "PUT":
                mockMvc.perform(put(endpoint.path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                    .andExpect(status().isUnauthorized());
                break;
                
            case "DELETE":
                mockMvc.perform(delete(endpoint.path)
                        .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
                break;
        }
    }

    /**
     * Property 2: Session Protection - GET /api/auth/me
     * 
     * Tests that the current user profile endpoint returns 401 without session.
     * Runs 100 iterations to ensure consistent behavior.
     */
    @RepeatedTest(100)
    void getCurrentUserReturns401WithoutSession() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    /**
     * Property 2: Session Protection - POST /api/auth/logout
     * 
     * Tests that logout endpoint returns 401 without session.
     * Runs 100 iterations to ensure consistent behavior.
     */
    @RepeatedTest(100)
    void logoutReturns401WithoutSession() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    /**
     * Property 2: Session Protection - GET /api/users
     * 
     * Tests that user list endpoint returns 401 without session.
     * Runs 100 iterations to ensure consistent behavior.
     */
    @RepeatedTest(100)
    void listUsersReturns401WithoutSession() throws Exception {
        mockMvc.perform(get("/api/users")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    /**
     * Property 2: Session Protection - POST /api/users
     * 
     * Tests that user creation endpoint returns 401 without session.
     * Runs 100 iterations to ensure consistent behavior.
     */
    @RepeatedTest(100)
    void createUserReturns401WithoutSession() throws Exception {
        String requestBody = """
            {
                "email": "test@tns.com",
                "employeeId": "EMP001",
                "fullName": "Test User",
                "password": "password123"
            }
            """;
        
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isUnauthorized());
    }

    /**
     * Property 2: Session Protection - GET /api/audit-logs
     * 
     * Tests that audit log endpoint returns 401 without session.
     * Runs 100 iterations to ensure consistent behavior.
     */
    @RepeatedTest(100)
    void getAuditLogsReturns401WithoutSession() throws Exception {
        mockMvc.perform(get("/api/audit-logs")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    /**
     * Property 2: Session Protection - Reporting Hierarchy Endpoints
     * 
     * Tests that reporting hierarchy endpoints return 401 without session.
     * Runs 100 iterations to ensure consistent behavior.
     */
    @RepeatedTest(100)
    void reportingHierarchyEndpointsReturn401WithoutSession() throws Exception {
        // Test a random reporting hierarchy endpoint
        String[] endpoints = {
            "/api/reporting-hierarchy/employee/1/current-manager",
            "/api/reporting-hierarchy/manager/1/direct-reports",
            "/api/reporting-hierarchy/active"
        };
        
        String endpoint = endpoints[random.nextInt(endpoints.length)];
        
        mockMvc.perform(get(endpoint)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    // ==================== Helper Classes ====================

    /**
     * Represents a protected endpoint with HTTP method and path
     */
    private static class ProtectedEndpoint {
        final String method;
        final String path;

        ProtectedEndpoint(String method, String path) {
            this.method = method;
            this.path = path;
        }
    }
}
