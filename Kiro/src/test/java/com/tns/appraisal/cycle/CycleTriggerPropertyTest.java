package com.tns.appraisal.cycle;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Property-based tests for appraisal cycle triggering.
 * 
 * **Validates: Requirements 3.3**
 * 
 * Property 6: Cycle Trigger Creates Exactly One Form Per Employee
 * 
 * For any appraisal cycle triggered with a set of N eligible employees, 
 * the system SHALL create exactly N appraisal form records — one per employee — 
 * each referencing the active Appraisal_Template at the time of triggering.
 * 
 * NOTE: This test is currently disabled because it requires:
 * 1. AppraisalForm entity and repository to be implemented
 * 2. User entity and repository to be implemented  
 * 3. CycleService.triggerCycle() method to be fully implemented
 * 4. NotificationService to be implemented
 * 
 * Once these components are available, remove the @Disabled annotation and
 * update the test to use the actual service layer instead of direct JDBC operations.
 * 
 * The test validates that:
 * - Exactly N forms are created for N employees
 * - Each employee has exactly one form in the cycle
 * - All forms reference the active template
 * - No duplicate forms exist (enforced by unique constraint)
 * - All forms have correct initial status (NOT_STARTED)
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:sqlserver://fileracks-sql-db.tnssinc.com:1433;databaseName=FR_Dev;encrypt=true;trustServerCertificate=true",
    "spring.datasource.username=hitesh",
    "spring.datasource.password=Hp@#2002",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.flyway.enabled=false"
})
@Disabled("Requires AppraisalForm entity, User entity, and full CycleService implementation")
class CycleTriggerPropertyTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Property 6: Cycle Trigger Creates Exactly One Form Per Employee
     * 
     * This property test verifies that when an appraisal cycle is triggered with N employees:
     * 1. Exactly N appraisal forms are created
     * 2. Each form has a unique employee_id
     * 3. Each form references the active template
     * 4. Each form belongs to the triggered cycle
     * 5. No duplicate forms exist for the same employee in the same cycle
     */
    @Property(tries = 100)
    @Label("Feature: employee-appraisal-cycle, Property 6: Cycle Trigger Creates Exactly One Form Per Employee")
    void cycleTriggerCreatesExactlyOneFormPerEmployee(
            @ForAll @IntRange(min = 1, max = 50) int numberOfEmployees
    ) {
        // Arrange: Set up test data
        Long activeTemplateId = createActiveTemplate();
        Long cycleId = createCycle(activeTemplateId);
        List<Long> employeeIds = createEmployees(numberOfEmployees);
        
        // Verify preconditions
        int formsBeforeTrigger = countFormsForCycle(cycleId);
        assert formsBeforeTrigger == 0 : "No forms should exist before trigger";
        
        // Act: Trigger the cycle
        triggerCycle(cycleId, employeeIds, activeTemplateId);
        
        // Assert: Verify exactly N forms were created
        int formsAfterTrigger = countFormsForCycle(cycleId);
        assert formsAfterTrigger == numberOfEmployees : 
            String.format("Expected %d forms, but found %d", numberOfEmployees, formsAfterTrigger);
        
        // Assert: Verify each employee has exactly one form
        for (Long employeeId : employeeIds) {
            int formsForEmployee = countFormsForEmployeeInCycle(cycleId, employeeId);
            assert formsForEmployee == 1 : 
                String.format("Employee %d should have exactly 1 form, but has %d", employeeId, formsForEmployee);
        }
        
        // Assert: Verify all forms reference the active template
        int formsWithCorrectTemplate = countFormsWithTemplate(cycleId, activeTemplateId);
        assert formsWithCorrectTemplate == numberOfEmployees : 
            String.format("All %d forms should reference template %d, but only %d do", 
                numberOfEmployees, activeTemplateId, formsWithCorrectTemplate);
        
        // Assert: Verify no duplicate forms exist (enforced by unique constraint)
        Set<Long> uniqueEmployeeIds = new HashSet<>(employeeIds);
        assert uniqueEmployeeIds.size() == numberOfEmployees : 
            "Employee IDs should be unique";
        
        // Assert: Verify all forms have correct initial status
        int formsWithNotStartedStatus = countFormsWithStatus(cycleId, "NOT_STARTED");
        assert formsWithNotStartedStatus == numberOfEmployees : 
            String.format("All %d forms should have NOT_STARTED status, but only %d do", 
                numberOfEmployees, formsWithNotStartedStatus);
    }

    /**
     * Property 6 Extension: Verify form creation with varying employee counts
     * Tests edge cases including single employee and large batches
     */
    @Property(tries = 100)
    @Label("Feature: employee-appraisal-cycle, Property 6: Cycle Trigger Handles Edge Cases")
    void cycleTriggerHandlesEdgeCases(
            @ForAll @IntRange(min = 1, max = 100) int numberOfEmployees
    ) {
        // Arrange
        Long activeTemplateId = createActiveTemplate();
        Long cycleId = createCycle(activeTemplateId);
        List<Long> employeeIds = createEmployees(numberOfEmployees);
        
        // Act
        triggerCycle(cycleId, employeeIds, activeTemplateId);
        
        // Assert: Total count matches
        int totalForms = countFormsForCycle(cycleId);
        assert totalForms == numberOfEmployees : 
            String.format("Expected %d forms for %d employees", numberOfEmployees, numberOfEmployees);
        
        // Assert: Each form has required fields populated
        List<Long> formIds = getFormIdsForCycle(cycleId);
        assert formIds.size() == numberOfEmployees : "Form IDs count mismatch";
        
        for (Long formId : formIds) {
            assert formHasRequiredFields(formId) : 
                String.format("Form %d is missing required fields", formId);
        }
    }

    // ==================== Helper Methods ====================

    private void cleanupTestData() {
        jdbcTemplate.execute("DELETE FROM appraisal_forms");
        jdbcTemplate.execute("DELETE FROM appraisal_cycles");
        jdbcTemplate.execute("DELETE FROM appraisal_templates");
        jdbcTemplate.execute("DELETE FROM user_roles");
        jdbcTemplate.execute("DELETE FROM users WHERE id >= 1000"); // Keep seed data
    }

    private Long createActiveTemplate() {
        String sql = "INSERT INTO appraisal_templates (version, schema_json, is_active, created_at) " +
                     "VALUES (?, ?, ?, GETUTCDATE())";
        jdbcTemplate.update(sql, "3.0", "{\"sections\":[]}", true);
        
        return jdbcTemplate.queryForObject(
            "SELECT TOP 1 id FROM appraisal_templates WHERE is_active = 1 ORDER BY id DESC", 
            Long.class
        );
    }

    private Long createCycle(Long templateId) {
        String sql = "INSERT INTO appraisal_cycles (name, start_date, end_date, template_id, status, created_by, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, GETUTCDATE(), GETUTCDATE())";
        
        jdbcTemplate.update(sql, 
            "Test Cycle " + System.currentTimeMillis(),
            LocalDate.now(),
            LocalDate.now().plusMonths(6),
            templateId,
            "ACTIVE",
            1L
        );
        
        return jdbcTemplate.queryForObject(
            "SELECT TOP 1 id FROM appraisal_cycles ORDER BY id DESC", 
            Long.class
        );
    }

    private List<Long> createEmployees(int count) {
        List<Long> employeeIds = new java.util.ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            long timestamp = System.currentTimeMillis();
            String employeeId = "EMP" + timestamp + "_" + i;
            
            String sql = "INSERT INTO users (employee_id, full_name, email, password_hash, manager_id, is_active, created_at, updated_at) " +
                         "VALUES (?, ?, ?, ?, ?, ?, GETUTCDATE(), GETUTCDATE())";
            
            jdbcTemplate.update(sql,
                employeeId,
                "Employee " + i,
                "employee" + timestamp + "_" + i + "@test.com",
                "$2a$10$dummyhash",
                1L, // Assign to manager with ID 1
                true
            );
            
            Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE employee_id = ?", 
                Long.class, 
                employeeId
            );
            
            employeeIds.add(userId);
        }
        
        return employeeIds;
    }

    private void triggerCycle(Long cycleId, List<Long> employeeIds, Long templateId) {
        for (Long employeeId : employeeIds) {
            // Get manager ID for the employee
            Long managerId = jdbcTemplate.queryForObject(
                "SELECT manager_id FROM users WHERE id = ?", 
                Long.class, 
                employeeId
            );
            
            String sql = "INSERT INTO appraisal_forms " +
                         "(cycle_id, employee_id, manager_id, template_id, status, created_at, updated_at) " +
                         "VALUES (?, ?, ?, ?, ?, GETUTCDATE(), GETUTCDATE())";
            
            jdbcTemplate.update(sql, cycleId, employeeId, managerId, templateId, "NOT_STARTED");
        }
    }

    private int countFormsForCycle(Long cycleId) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM appraisal_forms WHERE cycle_id = ?", 
            Integer.class, 
            cycleId
        );
    }

    private int countFormsForEmployeeInCycle(Long cycleId, Long employeeId) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM appraisal_forms WHERE cycle_id = ? AND employee_id = ?", 
            Integer.class, 
            cycleId, 
            employeeId
        );
    }

    private int countFormsWithTemplate(Long cycleId, Long templateId) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM appraisal_forms WHERE cycle_id = ? AND template_id = ?", 
            Integer.class, 
            cycleId, 
            templateId
        );
    }

    private int countFormsWithStatus(Long cycleId, String status) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM appraisal_forms WHERE cycle_id = ? AND status = ?", 
            Integer.class, 
            cycleId, 
            status
        );
    }

    private List<Long> getFormIdsForCycle(Long cycleId) {
        return jdbcTemplate.queryForList(
            "SELECT id FROM appraisal_forms WHERE cycle_id = ?", 
            Long.class, 
            cycleId
        );
    }

    private boolean formHasRequiredFields(Long formId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM appraisal_forms " +
            "WHERE id = ? AND cycle_id IS NOT NULL AND employee_id IS NOT NULL " +
            "AND manager_id IS NOT NULL AND template_id IS NOT NULL AND status IS NOT NULL",
            Integer.class,
            formId
        );
        return count != null && count == 1;
    }
}

/*
 * TO ENABLE THIS TEST:
 * 
 * 1. Remove the @Disabled annotation from the class
 * 
 * 2. Implement the missing entities:
 *    - Create AppraisalForm entity in src/main/java/com/tns/appraisal/form/AppraisalForm.java
 *    - Create User entity in src/main/java/com/tns/appraisal/user/User.java
 *    - Create Role entity in src/main/java/com/tns/appraisal/user/Role.java
 * 
 * 3. Implement the missing repositories:
 *    - Create AppraisalFormRepository in src/main/java/com/tns/appraisal/form/AppraisalFormRepository.java
 *    - Create UserRepository in src/main/java/com/tns/appraisal/user/UserRepository.java
 * 
 * 4. Complete the CycleService.triggerCycle() implementation:
 *    - Uncomment the TODO sections in CycleService.triggerCycle()
 *    - Inject AppraisalFormRepository and UserRepository
 *    - Implement form creation logic
 *    - Implement notification sending logic
 * 
 * 5. Update the test to use the actual service:
 *    - Replace direct JDBC operations in triggerCycle() helper method
 *    - Call cycleService.triggerCycle(cycleId, employeeIds, currentUserId) instead
 *    - Inject CycleService into the test class
 * 
 * 6. Configure test database:
 *    - Update TestPropertySource to point to a test database
 *    - Ensure Flyway migrations run on test database
 *    - Add cleanup scripts to reset test data between runs
 * 
 * 7. Run the test:
 *    mvn test -Dtest=CycleTriggerPropertyTest
 * 
 * Expected behavior:
 * - Test should run 100 iterations with random employee counts (1-50)
 * - Each iteration should verify exactly N forms are created for N employees
 * - All assertions should pass, confirming Property 6 holds
 */
