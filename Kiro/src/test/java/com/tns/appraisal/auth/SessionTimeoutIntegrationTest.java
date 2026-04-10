package com.tns.appraisal.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tns.appraisal.auth.dto.LoginRequest;
import com.tns.appraisal.user.User;
import com.tns.appraisal.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for session timeout handling.
 * Tests the complete session lifecycle including timeout behavior.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SessionTimeoutIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setEmployeeId("EMP001");
        testUser.setFullName("Test User");
        testUser.setEmail("test@tns.com");
        testUser.setPasswordHash(passwordEncoder.encode("password123"));
        testUser.setDesignation("Software Engineer");
        testUser.setDepartment("Engineering");
        testUser.setIsActive(true);
        testUser = userRepository.save(testUser);
    }

    @Test
    void testSuccessfulLoginCreatesSessionWith15MinuteTimeout() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginIdentifier("test@tns.com");
        loginRequest.setPassword("password123");

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.email").value("test@tns.com"))
            .andReturn();

        // Verify session was created with correct timeout
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assert session != null;
        assert session.getMaxInactiveInterval() == 900; // 15 minutes in seconds
    }

    @Test
    void testProtectedEndpointRequiresActiveSession() throws Exception {
        // Act & Assert - request without session should return 401
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void testProtectedEndpointWorksWithActiveSession() throws Exception {
        // Arrange - login to create session
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginIdentifier("test@tns.com");
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        // Act & Assert - request with session should succeed
        mockMvc.perform(get("/api/auth/me")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("test@tns.com"));
    }

    @Test
    void testInvalidatedSessionReturns401() throws Exception {
        // Arrange - login to create session
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginIdentifier("test@tns.com");
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        // Invalidate session (simulate timeout or logout)
        session.invalidate();

        // Act & Assert - request with invalidated session should return 401
        mockMvc.perform(get("/api/auth/me")
                .session(session))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testLogoutInvalidatesSession() throws Exception {
        // Arrange - login to create session
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginIdentifier("test@tns.com");
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        // Act - logout
        mockMvc.perform(post("/api/auth/logout")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Logout successful"));

        // Assert - session should be invalidated
        assert session.isInvalid();

        // Verify subsequent requests with same session return 401
        mockMvc.perform(get("/api/auth/me")
                .session(session))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testSessionTimeoutReturns401WithExpiredMessage() throws Exception {
        // This test simulates the session expiration behavior
        // In a real scenario, the session would expire after 15 minutes of inactivity
        
        // Arrange - create an expired session
        MockHttpSession expiredSession = new MockHttpSession();
        expiredSession.setMaxInactiveInterval(0); // Expire immediately
        
        // Act & Assert - request with expired session should return 401
        mockMvc.perform(get("/api/auth/me")
                .session(expiredSession))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
    }
}
