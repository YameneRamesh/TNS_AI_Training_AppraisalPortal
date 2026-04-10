package com.tns.appraisal.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tns.appraisal.auth.dto.LoginRequest;
import com.tns.appraisal.user.User;
import com.tns.appraisal.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
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

import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Property-based test for Authentication Correctness (Property 1).
 * 
 * Property: For any user credential pair, authentication SHALL succeed if and only if 
 * the credentials match a valid active user account — establishing a session on success 
 * and returning an error with no session on failure.
 * 
 * Validates: Requirements 1.1, 1.2
 * 
 * This test uses JUnit's @RepeatedTest to run 100 iterations with randomly generated
 * test data, simulating property-based testing behavior while maintaining Spring Boot
 * integration test compatibility.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthenticationCorrectnessPropertyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final Random random = new Random();

    @BeforeEach
    void setUp() {
        // Clean up any test users from previous runs
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        // Clean up test users after each test
        userRepository.deleteAll();
    }

    /**
     * Property 1: Authentication Correctness - Valid Credentials
     * 
     * Tests that valid credentials for an active user result in successful authentication
     * with session creation. Runs 100 iterations with random test data.
     */
    @RepeatedTest(100)
    void authenticationSucceedsWithValidCredentials() throws Exception {
        // Arrange: Generate random user credentials
        String email = generateRandomEmail();
        String employeeId = generateRandomEmployeeId();
        String password = generateRandomPassword();
        
        User user = createTestUser(email, employeeId, password, true);
        
        // Act & Assert: Valid credentials should succeed
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginIdentifier(email);
        loginRequest.setPassword(password);
        
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value(email))
            .andReturn();
        
        // Verify session was created
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getAttribute("AUTHENTICATED_USER_ID")).isEqualTo(user.getId());
        assertThat(session.getMaxInactiveInterval()).isEqualTo(900); // 15 minutes
        
        // Cleanup
        userRepository.delete(user);
    }

    /**
     * Property 1: Authentication Correctness - Invalid Password
     * 
     * Tests that invalid password results in authentication failure without session creation.
     * Runs 100 iterations with random test data.
     */
    @RepeatedTest(100)
    void authenticationFailsWithInvalidPassword() throws Exception {
        // Arrange: Generate random user credentials
        String email = generateRandomEmail();
        String employeeId = generateRandomEmployeeId();
        String correctPassword = generateRandomPassword();
        String wrongPassword = generateRandomPassword();
        
        // Ensure passwords are different
        while (correctPassword.equals(wrongPassword)) {
            wrongPassword = generateRandomPassword();
        }
        
        User user = createTestUser(email, employeeId, correctPassword, true);
        
        // Act & Assert: Invalid password should fail
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginIdentifier(email);
        loginRequest.setPassword(wrongPassword);
        
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andReturn();
        
        // Verify no session was created
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNull();
        
        // Cleanup
        userRepository.delete(user);
    }

    /**
     * Property 1: Authentication Correctness - Nonexistent User
     * 
     * Tests that authentication fails for nonexistent users without session creation.
     * Runs 100 iterations with random test data.
     */
    @RepeatedTest(100)
    void authenticationFailsForNonexistentUser() throws Exception {
        // Arrange: Generate random credentials for nonexistent user
        String email = generateRandomEmail();
        String password = generateRandomPassword();
        
        // Ensure user doesn't exist
        assertThat(userRepository.findByEmail(email)).isEmpty();
        
        // Act & Assert: Nonexistent user should fail
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginIdentifier(email);
        loginRequest.setPassword(password);
        
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andReturn();
        
        // Verify no session was created
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNull();
    }

    /**
     * Property 1: Authentication Correctness - Inactive User
     * 
     * Tests that authentication fails for inactive users even with correct password.
     * Runs 100 iterations with random test data.
     */
    @RepeatedTest(100)
    void authenticationFailsForInactiveUser() throws Exception {
        // Arrange: Generate random user credentials
        String email = generateRandomEmail();
        String employeeId = generateRandomEmployeeId();
        String password = generateRandomPassword();
        
        User inactiveUser = createTestUser(email, employeeId, password, false);
        
        // Act & Assert: Inactive user should fail even with correct password
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginIdentifier(email);
        loginRequest.setPassword(password);
        
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andReturn();
        
        // Verify no session was created
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNull();
        
        // Cleanup
        userRepository.delete(inactiveUser);
    }

    /**
     * Property 1: Authentication Correctness - Login by Employee ID
     * 
     * Tests that authentication succeeds when using employee ID as login identifier.
     * Runs 100 iterations with random test data.
     */
    @RepeatedTest(100)
    void authenticationSucceedsWithEmployeeIdAsIdentifier() throws Exception {
        // Arrange: Generate random user credentials
        String email = generateRandomEmail();
        String employeeId = generateRandomEmployeeId();
        String password = generateRandomPassword();
        
        User user = createTestUser(email, employeeId, password, true);
        
        // Act & Assert: Login with employee ID should succeed
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginIdentifier(employeeId);
        loginRequest.setPassword(password);
        
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.employeeId").value(employeeId))
            .andReturn();
        
        // Verify session was created
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getAttribute("AUTHENTICATED_USER_ID")).isEqualTo(user.getId());
        
        // Cleanup
        userRepository.delete(user);
    }

    // ==================== Helper Methods ====================

    private User createTestUser(String email, String employeeId, String password, boolean isActive) {
        User user = new User();
        user.setEmail(email);
        user.setEmployeeId(employeeId);
        user.setFullName("Test User " + employeeId);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDesignation("Software Engineer");
        user.setDepartment("Engineering");
        user.setIsActive(isActive);
        return userRepository.save(user);
    }

    private String generateRandomEmail() {
        return "user" + UUID.randomUUID().toString().substring(0, 8) + "@tns.com";
    }

    private String generateRandomEmployeeId() {
        return "EMP" + String.format("%06d", random.nextInt(1000000));
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        int length = 8 + random.nextInt(13); // 8-20 characters
        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }
}
