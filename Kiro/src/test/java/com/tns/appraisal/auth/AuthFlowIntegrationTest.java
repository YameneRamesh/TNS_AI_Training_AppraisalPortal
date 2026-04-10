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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the authentication flow.
 *
 * Covers:
 *  1. Successful login → 200, session cookie set
 *  2. Failed login with invalid password → 401, no session
 *  3. Failed login with non-existent user → 401, no session
 *  4. Failed login with inactive user → 401, no session
 *  5. GET /api/auth/me with valid session → 200, full profile (name, designation, department, manager)
 *  6. GET /api/auth/me without session → 401
 *  7. POST /api/auth/logout with valid session → 200, session invalidated
 *  8. Access protected endpoint after logout → 401
 *  9. Access protected endpoint with expired/invalid session → 401
 *
 * Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5
 * Properties: Property 1 (Authentication Correctness), Property 2 (Session Protection)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@Transactional
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User manager;
    private User activeEmployee;
    private User inactiveEmployee;

    private static final String VALID_PASSWORD = "SecurePass123";

    @BeforeEach
    void setUp() {
        // Manager user (no manager themselves)
        manager = new User();
        manager.setEmployeeId("MGR001");
        manager.setFullName("Alice Manager");
        manager.setEmail("alice.manager@tns.com");
        manager.setPasswordHash(passwordEncoder.encode(VALID_PASSWORD));
        manager.setDesignation("Engineering Manager");
        manager.setDepartment("Engineering");
        manager.setIsActive(true);
        manager = userRepository.save(manager);

        // Active employee with a manager
        activeEmployee = new User();
        activeEmployee.setEmployeeId("EMP001");
        activeEmployee.setFullName("Bob Employee");
        activeEmployee.setEmail("bob.employee@tns.com");
        activeEmployee.setPasswordHash(passwordEncoder.encode(VALID_PASSWORD));
        activeEmployee.setDesignation("Software Engineer");
        activeEmployee.setDepartment("Engineering");
        activeEmployee.setManager(manager);
        activeEmployee.setIsActive(true);
        activeEmployee = userRepository.save(activeEmployee);

        // Inactive employee
        inactiveEmployee = new User();
        inactiveEmployee.setEmployeeId("EMP002");
        inactiveEmployee.setFullName("Carol Inactive");
        inactiveEmployee.setEmail("carol.inactive@tns.com");
        inactiveEmployee.setPasswordHash(passwordEncoder.encode(VALID_PASSWORD));
        inactiveEmployee.setDesignation("Software Engineer");
        inactiveEmployee.setDepartment("Engineering");
        inactiveEmployee.setIsActive(false);
        inactiveEmployee = userRepository.save(inactiveEmployee);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 1: Successful login with valid credentials → 200, session set
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void login_withValidCredentials_returns200AndCreatesSession() throws Exception {
        LoginRequest req = new LoginRequest(activeEmployee.getEmail(), VALID_PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value(activeEmployee.getEmail()))
            .andExpect(jsonPath("$.data.employeeId").value(activeEmployee.getEmployeeId()))
            .andReturn();

        // Session must be established
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getAttribute("AUTHENTICATED_USER_ID")).isEqualTo(activeEmployee.getId());
        // 15-minute timeout
        assertThat(session.getMaxInactiveInterval()).isEqualTo(900);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 2: Failed login with invalid password → 401, no session
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void login_withInvalidPassword_returns401AndNoSession() throws Exception {
        LoginRequest req = new LoginRequest(activeEmployee.getEmail(), "WrongPassword!");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNull();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 3: Failed login with non-existent user → 401, no session
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void login_withNonExistentUser_returns401AndNoSession() throws Exception {
        LoginRequest req = new LoginRequest("nobody@tns.com", VALID_PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNull();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 4: Failed login with inactive user → 401, no session
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void login_withInactiveUser_returns401AndNoSession() throws Exception {
        LoginRequest req = new LoginRequest(inactiveEmployee.getEmail(), VALID_PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNull();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 5: GET /api/auth/me with valid session → 200, full profile
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getMe_withValidSession_returns200AndFullProfile() throws Exception {
        MockHttpSession session = loginAndGetSession(activeEmployee.getEmail(), VALID_PASSWORD);

        mockMvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.fullName").value(activeEmployee.getFullName()))
            .andExpect(jsonPath("$.data.designation").value(activeEmployee.getDesignation()))
            .andExpect(jsonPath("$.data.department").value(activeEmployee.getDepartment()))
            .andExpect(jsonPath("$.data.managerName").value(manager.getFullName()))
            .andExpect(jsonPath("$.data.email").value(activeEmployee.getEmail()));
    }

    @Test
    void getMe_withValidSession_returnsManagerNameAsNull_whenNoManager() throws Exception {
        // Manager user has no manager themselves
        MockHttpSession session = loginAndGetSession(manager.getEmail(), VALID_PASSWORD);

        mockMvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.fullName").value(manager.getFullName()))
            .andExpect(jsonPath("$.data.managerName").isEmpty());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 6: GET /api/auth/me without session → 401
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getMe_withoutSession_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 7: POST /api/auth/logout with valid session → 200, session invalidated
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void logout_withValidSession_returns200AndInvalidatesSession() throws Exception {
        MockHttpSession session = loginAndGetSession(activeEmployee.getEmail(), VALID_PASSWORD);

        mockMvc.perform(post("/api/auth/logout").session(session))
            .andExpect(status().isOk());

        assertThat(session.isInvalid()).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 8: Access protected endpoint after logout → 401
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void accessProtectedEndpoint_afterLogout_returns401() throws Exception {
        MockHttpSession session = loginAndGetSession(activeEmployee.getEmail(), VALID_PASSWORD);

        // Logout
        mockMvc.perform(post("/api/auth/logout").session(session))
            .andExpect(status().isOk());

        // Subsequent request with the same (now-invalidated) session must be rejected
        mockMvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void accessUsersEndpoint_afterLogout_returns401() throws Exception {
        MockHttpSession session = loginAndGetSession(activeEmployee.getEmail(), VALID_PASSWORD);

        mockMvc.perform(post("/api/auth/logout").session(session))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/users").session(session))
            .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 9: Access protected endpoint with expired/invalid session → 401
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void accessProtectedEndpoint_withExpiredSession_returns401() throws Exception {
        // Simulate an expired session (maxInactiveInterval = 0 means already expired)
        MockHttpSession expiredSession = new MockHttpSession();
        expiredSession.setMaxInactiveInterval(0);

        mockMvc.perform(get("/api/auth/me").session(expiredSession))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void accessProtectedEndpoint_withManuallyInvalidatedSession_returns401() throws Exception {
        MockHttpSession session = loginAndGetSession(activeEmployee.getEmail(), VALID_PASSWORD);
        session.invalidate();

        mockMvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void accessProtectedEndpoint_withFreshSessionButNoUserId_returns401() throws Exception {
        // A session that exists but has no AUTHENTICATED_USER_ID attribute
        MockHttpSession emptySession = new MockHttpSession();

        mockMvc.perform(get("/api/auth/me").session(emptySession))
            .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    private MockHttpSession loginAndGetSession(String identifier, String password) throws Exception {
        LoginRequest req = new LoginRequest(identifier, password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        return session;
    }
}
