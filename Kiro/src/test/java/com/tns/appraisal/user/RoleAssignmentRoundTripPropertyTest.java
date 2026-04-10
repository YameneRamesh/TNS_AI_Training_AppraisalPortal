package com.tns.appraisal.user;

import com.tns.appraisal.auth.Role;
import com.tns.appraisal.auth.RoleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for Role Assignment Round Trip (Property 5).
 * 
 * **Validates: Requirements 2.5**
 * 
 * Property: For any user and any valid role, assigning the role SHALL result in the user 
 * having that role; revoking the role SHALL result in the user no longer having that role.
 * 
 * This test uses JUnit's @RepeatedTest to run 100 iterations with randomly generated
 * test data, simulating property-based testing behavior while maintaining Spring Boot
 * integration test compatibility.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RoleAssignmentRoundTripPropertyTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final Random random = new Random();
    private List<String> availableRoles;
    private User adminUser;

    @BeforeEach
    void setUp() {
        // Clean up any test users from previous runs
        userRepository.deleteAll();
        
        // Get all available roles from the database
        availableRoles = roleRepository.findAll().stream()
            .map(Role::getName)
            .collect(Collectors.toList());
        
        // Create an admin user for audit logging
        adminUser = createTestUser("admin@tns.com", "ADMIN001", "admin123", true);
        adminUser = userRepository.save(adminUser);
    }

    @AfterEach
    void tearDown() {
        // Clean up test users after each test
        userRepository.deleteAll();
    }

    /**
     * Property 5: Role Assignment Round Trip - Single Role Assignment and Revocation
     * 
     * Tests that assigning a role to a user results in the user having that role,
     * and revoking it results in the user no longer having that role.
     * Runs 100 iterations with random test data.
     */
    @RepeatedTest(100)
    void roleAssignmentAndRevocationRoundTrip() {
        // Arrange: Generate random user and select a random role
        String email = generateRandomEmail();
        String employeeId = generateRandomEmployeeId();
        String password = generateRandomPassword();
        
        User user = createTestUser(email, employeeId, password, true);
        user = userRepository.save(user);
        
        // Start with a base role (EMPLOYEE) so we can safely remove roles later
        String baseRole = "EMPLOYEE";
        userService.addRole(user.getId(), baseRole, adminUser.getId());
        
        // Select a random role to test (different from base role)
        String roleToTest = selectRandomRoleDifferentFrom(baseRole);
        
        // Act: Assign the role
        User userAfterAssignment = userService.addRole(user.getId(), roleToTest, adminUser.getId());
        
        // Assert: User should have the assigned role
        assertThat(userAfterAssignment.getRoles())
            .extracting(Role::getName)
            .contains(roleToTest);
        
        // Verify by fetching fresh from database
        User userFromDb = userRepository.findById(user.getId()).orElseThrow();
        assertThat(userFromDb.getRoles())
            .extracting(Role::getName)
            .contains(roleToTest);
        
        // Act: Revoke the role
        User userAfterRevocation = userService.removeRole(user.getId(), roleToTest, adminUser.getId());
        
        // Assert: User should no longer have the revoked role
        assertThat(userAfterRevocation.getRoles())
            .extracting(Role::getName)
            .doesNotContain(roleToTest);
        
        // Verify by fetching fresh from database
        userFromDb = userRepository.findById(user.getId()).orElseThrow();
        assertThat(userFromDb.getRoles())
            .extracting(Role::getName)
            .doesNotContain(roleToTest);
        
        // Cleanup
        userRepository.delete(user);
    }

    /**
     * Property 5: Role Assignment Round Trip - Multiple Roles Assignment and Revocation
     * 
     * Tests that assigning multiple roles to a user results in the user having all those roles,
     * and revoking them results in the user no longer having those roles.
     * Runs 100 iterations with random test data.
     */
    @RepeatedTest(100)
    void multipleRolesAssignmentAndRevocationRoundTrip() {
        // Arrange: Generate random user
        String email = generateRandomEmail();
        String employeeId = generateRandomEmployeeId();
        String password = generateRandomPassword();
        
        User user = createTestUser(email, employeeId, password, true);
        user = userRepository.save(user);
        
        // Select 2-3 random roles to assign
        int numberOfRoles = 2 + random.nextInt(2); // 2 or 3 roles
        Set<String> rolesToAssign = selectRandomRoles(numberOfRoles);
        
        // Act: Assign all roles at once using assignRoles
        User userAfterAssignment = userService.assignRoles(user.getId(), rolesToAssign, adminUser.getId());
        
        // Assert: User should have all assigned roles
        Set<String> userRoleNames = userAfterAssignment.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toSet());
        assertThat(userRoleNames).containsAll(rolesToAssign);
        
        // Verify by fetching fresh from database
        User userFromDb = userRepository.findById(user.getId()).orElseThrow();
        Set<String> userRoleNamesFromDb = userFromDb.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toSet());
        assertThat(userRoleNamesFromDb).containsAll(rolesToAssign);
        
        // Act: Remove one role at a time (keeping at least one role)
        List<String> rolesToRemove = new ArrayList<>(rolesToAssign);
        // Keep the last role to avoid violating the "at least one role" constraint
        for (int i = 0; i < rolesToRemove.size() - 1; i++) {
            String roleToRemove = rolesToRemove.get(i);
            userService.removeRole(user.getId(), roleToRemove, adminUser.getId());
            
            // Assert: User should no longer have the removed role
            userFromDb = userRepository.findById(user.getId()).orElseThrow();
            assertThat(userFromDb.getRoles())
                .extracting(Role::getName)
                .doesNotContain(roleToRemove);
        }
        
        // Cleanup
        userRepository.delete(user);
    }

    /**
     * Property 5: Role Assignment Round Trip - Idempotent Assignment
     * 
     * Tests that assigning a role that a user already has is idempotent
     * (no error, user still has the role).
     * Runs 100 iterations with random test data.
     */
    @RepeatedTest(100)
    void roleAssignmentIsIdempotent() {
        // Arrange: Generate random user and select a random role
        String email = generateRandomEmail();
        String employeeId = generateRandomEmployeeId();
        String password = generateRandomPassword();
        
        User user = createTestUser(email, employeeId, password, true);
        user = userRepository.save(user);
        
        String roleToTest = selectRandomRole();
        
        // Act: Assign the role twice
        User userAfterFirstAssignment = userService.addRole(user.getId(), roleToTest, adminUser.getId());
        User userAfterSecondAssignment = userService.addRole(user.getId(), roleToTest, adminUser.getId());
        
        // Assert: User should have the role (only once)
        assertThat(userAfterSecondAssignment.getRoles())
            .extracting(Role::getName)
            .contains(roleToTest);
        
        // Count occurrences - should be exactly 1
        long roleCount = userAfterSecondAssignment.getRoles().stream()
            .filter(r -> r.getName().equals(roleToTest))
            .count();
        assertThat(roleCount).isEqualTo(1);
        
        // Cleanup
        userRepository.delete(user);
    }

    /**
     * Property 5: Role Assignment Round Trip - Reassignment After Revocation
     * 
     * Tests that a role can be reassigned after being revoked (round trip multiple times).
     * Runs 100 iterations with random test data.
     */
    @RepeatedTest(100)
    void roleCanBeReassignedAfterRevocation() {
        // Arrange: Generate random user
        String email = generateRandomEmail();
        String employeeId = generateRandomEmployeeId();
        String password = generateRandomPassword();
        
        User user = createTestUser(email, employeeId, password, true);
        user = userRepository.save(user);
        
        // Start with two roles so we can safely remove one
        String baseRole = "EMPLOYEE";
        String roleToTest = selectRandomRoleDifferentFrom(baseRole);
        
        userService.addRole(user.getId(), baseRole, adminUser.getId());
        
        // Perform multiple assignment/revocation cycles
        int cycles = 2 + random.nextInt(3); // 2-4 cycles
        for (int i = 0; i < cycles; i++) {
            // Assign
            User userAfterAssignment = userService.addRole(user.getId(), roleToTest, adminUser.getId());
            assertThat(userAfterAssignment.getRoles())
                .extracting(Role::getName)
                .contains(roleToTest);
            
            // Revoke
            User userAfterRevocation = userService.removeRole(user.getId(), roleToTest, adminUser.getId());
            assertThat(userAfterRevocation.getRoles())
                .extracting(Role::getName)
                .doesNotContain(roleToTest);
        }
        
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
        return user;
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

    private String selectRandomRole() {
        return availableRoles.get(random.nextInt(availableRoles.size()));
    }

    private String selectRandomRoleDifferentFrom(String excludeRole) {
        List<String> filteredRoles = availableRoles.stream()
            .filter(role -> !role.equals(excludeRole))
            .collect(Collectors.toList());
        
        if (filteredRoles.isEmpty()) {
            // Fallback: if only one role exists, return it anyway
            return availableRoles.get(0);
        }
        
        return filteredRoles.get(random.nextInt(filteredRoles.size()));
    }

    private Set<String> selectRandomRoles(int count) {
        Set<String> selectedRoles = new HashSet<>();
        List<String> rolesCopy = new ArrayList<>(availableRoles);
        Collections.shuffle(rolesCopy, random);
        
        int actualCount = Math.min(count, rolesCopy.size());
        for (int i = 0; i < actualCount; i++) {
            selectedRoles.add(rolesCopy.get(i));
        }
        
        return selectedRoles;
    }
}
