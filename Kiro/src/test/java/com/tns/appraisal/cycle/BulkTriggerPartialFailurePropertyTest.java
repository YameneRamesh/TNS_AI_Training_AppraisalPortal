package com.tns.appraisal.cycle;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Property-based tests for bulk cycle trigger partial failure resilience.
 * 
 * **Validates: Requirements 14.3**
 * 
 * Property 18: Bulk Trigger Partial Failure Resilience
 * 
 * For any bulk cycle trigger operation where K out of N employee form creations fail, 
 * the system SHALL create N-K forms successfully, log K failure entries, and complete 
 * the operation without aborting — leaving no employee silently unprocessed.
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
 * - When K out of N employees fail, exactly N-K forms are created successfully
 * - K failure entries are logged in the result
 * - The operation completes without aborting
 * - No employee is silently unprocessed (all are either success or logged failure)
 * - Successful forms have correct status and template reference
 * - Failed employees are not left in an inconsistent state
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
class BulkTriggerPartialFailurePropertyTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Property 18: Bulk Trigger Partial Failure Resilience
     * 
     * This property test verifies that when a bulk cycle trigger operation encounters failures:
     * 1. Exactly N-K forms are created successfully (where K is the number of failures)
     * 2. K failure entries are logged in the result
     * 3. The operation completes without aborting
     * 4. All N employees are accounted for (either success or logged failure)
     * 5. Successful forms have correct status and template reference
     * 6. No employee is silently unprocessed
     */
    @Property(tries = 100)
    @Label("Feature: employee-appraisal-cycle, Property 18: Bulk Trigger Partial Failure Resilience")
    void bulkTriggerPartialFailureResilience(
            @ForAll @IntRange(min = 5, max = 50) int totalEmployees,
            @ForAll @IntRange(min = 1, max = 10) int numberOfFailures
    ) {
        // Ensure failures don't exceed total employees
        Assume.that(numberOfFailures < totalEmployees);
        
        int expectedSuccesses = totalEmployees - numberOfFailures;
        
        // Arrange: Set up test data
        Long activeTemplateId = createActiveTemplate();
        Long cycleId = createCycle(activeTemplateId);
        
        // Create valid employees (will succeed)
        List<Long> validEmployeeIds = createValidEmployees(expectedSuccesses);
        
        // Create invalid employees (will fail - no manager assigned)
        List<Long> invalidEmployeeIds = createInvalidEmployees(numberOfFailures);
        
        // Combine and shuffle to simulate random failure distribution
        List<Long> allEmployeeIds = new ArrayList<>();
        allEmployeeIds.addAll(validEmployeeIds);
        allEmployeeIds.addAll(invalidEmployeeIds);
        Collections.shuffle(allEmployeeIds);
        
        // Verify preconditions
        int formsBeforeTrigger = countFormsForCycle(cycleId);
        assert formsBeforeTrigger == 0 : "No forms should exist before trigger";
        
        // Act: Trigger the cycle with mixed valid/invalid employees
        TriggerCycleResult result = triggerCycleWithPartialFailures(
            cycleId, 
            allEmployeeIds, 
            activeTemplateId,
            new HashSet<>(invalidEmployeeIds)
        );
        
        // Assert: Verify total employees processed
        assert result.getTotalEmployees() == totalEmployees : 
            String.format("Expected total %d employees, but result shows %d", 
                totalEmployees, result.getTotalEmployees());
        
        // Assert: Verify exactly N-K forms were created successfully
        assert result.getSuccessCount() == expectedSuccesses : 
            String.format("Expected %d successes, but got %d", 
                expectedSuccesses, result.getSuccessCount());
        
        // Assert: Verify exactly K failures were logged
        assert result.getFailureCount() == numberOfFailures : 
            String.format("Expected %d failures, but got %d", 
                numberOfFailures, result.getFailureCount());
        
        // Assert: Verify all employees are accounted for (success + failure = total)
        int accountedFor = result.getSuccessCount() + result.getFailureCount();
        assert accountedFor == totalEmployees : 
            String.format("Expected %d total accounted for, but got %d (success: %d, failure: %d)", 
                totalEmployees, accountedFor, result.getSuccessCount(), result.getFailureCount());
        
        // Assert: Verify exactly N-K forms exist in database
        int formsAfterTrigger = countFormsForCycle(cycleId);
        assert formsAfterTrigger == expectedSuccesses : 
            String.format("Expected %d forms in database, but found %d", 
                expectedSuccesses, formsAfterTrigger);
        
        // Assert: Verify all successful forms have correct template
        int formsWithCorrectTemplate = countFormsWithTemplate(cycleId, activeTemplateId);
        assert formsWithCorrectTemplate == expectedSuccesses : 
            String.format("All %d successful forms should reference template %d, but only %d do", 
                expectedSuccesses, activeTemplateId, formsWithCorrectTemplate);
        
        // Assert: Verify all successful forms have NOT_STARTED status
        int formsWithNotStartedStatus = countFormsWithStatus(cycleId, "NOT_STARTED");
        assert formsWithNotStartedStatus == expectedSuccesses : 
            String.format("All %d successful forms should have NOT_STARTED status, but only %d do", 
                expectedSuccesses, formsWithNotStartedStatus);
        
        // Assert: Verify failure list contains exactly K entries
        assert result.getFailures().size() == numberOfFailures : 
            String.format("Expected %d failure entries, but got %d", 
                numberOfFailures, result.getFailures().size());
        
        // Assert: Verify all failed employee IDs are in the failure list
        Set<Long> failedEmployeeIds = result.getFailures().stream()
            .map(TriggerCycleResult.EmployeeFailure::getEmployeeId)
            .collect(Collectors.toSet());
        
        for (Long invalidId : invalidEmployeeIds) {
            assert failedEmployeeIds.contains(invalidId) : 
                String.format("Failed employee %d should be in failure list", invalidId);
        }
        
        // Assert: Verify no forms were created for failed employees
        for (Long invalidId : invalidEmployeeIds) {
            int formsForFailedEmployee = countFormsForEmployeeInCycle(cycleId, invalidId);
            assert formsForFailedEmployee == 0 : 
                String.format("Failed employee %d should have no forms, but has %d", 
                    invalidId, formsForFailedEmployee);
        }
        
        // Assert: Verify all valid employees have exactly one form
        for (Long validId : validEmployeeIds) {
            int formsForValidEmployee = countFormsForEmployeeInCycle(cycleId, validId);
            assert formsForValidEmployee == 1 : 
                String.format("Valid employee %d should have exactly 1 form, but has %d", 
                    validId, formsForValidEmployee);
        }
    }

    /**
     * Property 18 Extension: Verify resilience with high failure rate
     * Tests that the system handles scenarios where most employees fail
     */
    @Property(tries = 50)
    @Label("Feature: employee-appraisal-cycle, Property 18: High Failure Rate Resilience")
    void bulkTriggerHighFailureRateResilience(
            @ForAll @IntRange(min = 10, max = 30) int totalEmployees,
            @ForAll("highFailureRate") double failureRate
    ) {
        int numberOfFailures = (int) Math.ceil(totalEmployees * failureRate);
        int expectedSuccesses = totalEmployees - numberOfFailures;
        
        // Ensure at least one success
        Assume.that(expectedSuccesses >= 1);
        
        // Arrange
        Long activeTemplateId = createActiveTemplate();
        Long cycleId = createCycle(activeTemplateId);
        List<Long> validEmployeeIds = createValidEmployees(expectedSuccesses);
        List<Long> invalidEmployeeIds = createInvalidEmployees(numberOfFailures);
        
        List<Long> allEmployeeIds = new ArrayList<>();
        allEmployeeIds.addAll(validEmployeeIds);
        allEmployeeIds.addAll(invalidEmployeeIds);
        Collections.shuffle(allEmployeeIds);
        
        // Act
        TriggerCycleResult result = triggerCycleWithPartialFailures(
            cycleId, 
            allEmployeeIds, 
            activeTemplateId,
            new HashSet<>(invalidEmployeeIds)
        );
        
        // Assert: Operation completes despite high failure rate
        assert result.getTotalEmployees() == totalEmployees : 
            "Operation should complete with all employees processed";
        
        assert result.getSuccessCount() == expectedSuccesses : 
            String.format("Expected %d successes despite %d%% failure rate", 
                expectedSuccesses, (int)(failureRate * 100));
        
        assert result.getFailureCount() == numberOfFailures : 
            String.format("Expected %d failures with %d%% failure rate", 
                numberOfFailures, (int)(failureRate * 100));
        
        // Assert: Database state is consistent
        int formsInDb = countFormsForCycle(cycleId);
        assert formsInDb == expectedSuccesses : 
            "Database should contain only successful forms";
    }

    /**
     * Property 18 Extension: Verify no silent failures
     * Tests that every employee is either successfully processed or explicitly logged as failed
     */
    @Property(tries = 50)
    @Label("Feature: employee-appraisal-cycle, Property 18: No Silent Failures")
    void bulkTriggerNoSilentFailures(
            @ForAll @IntRange(min = 5, max = 30) int totalEmployees,
            @ForAll @IntRange(min = 1, max = 5) int numberOfFailures
    ) {
        Assume.that(numberOfFailures < totalEmployees);
        
        int expectedSuccesses = totalEmployees - numberOfFailures;
        
        // Arrange
        Long activeTemplateId = createActiveTemplate();
        Long cycleId = createCycle(activeTemplateId);
        List<Long> validEmployeeIds = createValidEmployees(expectedSuccesses);
        List<Long> invalidEmployeeIds = createInvalidEmployees(numberOfFailures);
        
        List<Long> allEmployeeIds = new ArrayList<>();
        allEmployeeIds.addAll(validEmployeeIds);
        allEmployeeIds.addAll(invalidEmployeeIds);
        
        // Act
        TriggerCycleResult result = triggerCycleWithPartialFailures(
            cycleId, 
            allEmployeeIds, 
            activeTemplateId,
            new HashSet<>(invalidEmployeeIds)
        );
        
        // Assert: Every employee is accounted for
        Set<Long> processedEmployees = new HashSet<>();
        
        // Add successful employees (those with forms)
        List<Long> employeesWithForms = getEmployeeIdsWithFormsInCycle(cycleId);
        processedEmployees.addAll(employeesWithForms);
        
        // Add failed employees (those in failure list)
        Set<Long> failedEmployees = result.getFailures().stream()
            .map(TriggerCycleResult.EmployeeFailure::getEmployeeId)
            .collect(Collectors.toSet());
        processedEmployees.addAll(failedEmployees);
        
        // Assert: No overlap between success and failure
        Set<Long> successSet = new HashSet<>(employeesWithForms);
        Set<Long> intersection = new HashSet<>(successSet);
        intersection.retainAll(failedEmployees);
        assert intersection.isEmpty() : 
            "No employee should be in both success and failure lists";
        
        // Assert: All employees are accounted for
        assert processedEmployees.size() == totalEmployees : 
            String.format("Expected %d employees accounted for, but got %d", 
                totalEmployees, processedEmployees.size());
        
        // Assert: All original employee IDs are present
        for (Long employeeId : allEmployeeIds) {
            assert processedEmployees.contains(employeeId) : 
                String.format("Employee %d was silently unprocessed", employeeId);
        }
    }

    // ==================== Arbitraries ====================

    @Provide
    Arbitrary<Double> highFailureRate() {
        // Generate failure rates between 50% and 90%
        return Arbitraries.doubles().between(0.5, 0.9);
    }

    // ==================== Helper Methods ====================

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
            "DRAFT",
            1L
        );
        
        return jdbcTemplate.queryForObject(
            "SELECT TOP 1 id FROM appraisal_cycles ORDER BY id DESC", 
            Long.class
        );
    }

    private List<Long> createValidEmployees(int count) {
        List<Long> employeeIds = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            long timestamp = System.currentTimeMillis();
            String employeeId = "VALID_EMP" + timestamp + "_" + i;
            
            String sql = "INSERT INTO users (employee_id, full_name, email, password_hash, manager_id, is_active, created_at, updated_at) " +
                         "VALUES (?, ?, ?, ?, ?, ?, GETUTCDATE(), GETUTCDATE())";
            
            jdbcTemplate.update(sql,
                employeeId,
                "Valid Employee " + i,
                "valid_employee" + timestamp + "_" + i + "@test.com",
                "$2a$10$dummyhash",
                1L, // Valid manager ID
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

    private List<Long> createInvalidEmployees(int count) {
        List<Long> employeeIds = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            long timestamp = System.currentTimeMillis();
            String employeeId = "INVALID_EMP" + timestamp + "_" + i;
            
            // Create employee WITHOUT manager_id (will cause failure during trigger)
            String sql = "INSERT INTO users (employee_id, full_name, email, password_hash, manager_id, is_active, created_at, updated_at) " +
                         "VALUES (?, ?, ?, ?, ?, ?, GETUTCDATE(), GETUTCDATE())";
            
            jdbcTemplate.update(sql,
                employeeId,
                "Invalid Employee " + i,
                "invalid_employee" + timestamp + "_" + i + "@test.com",
                "$2a$10$dummyhash",
                null, // NULL manager_id will cause validation failure
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

    /**
     * Simulates CycleService.triggerCycle() with partial failure handling.
     * In actual implementation, this would call cycleService.triggerCycle().
     * 
     * This method implements the bulk processing logic with partial failure resilience:
     * - Processes each employee individually
     * - Catches exceptions for individual failures
     * - Continues processing remaining employees after a failure
     * - Returns summary with success/failure counts
     */
    private TriggerCycleResult triggerCycleWithPartialFailures(
            Long cycleId, 
            List<Long> employeeIds, 
            Long templateId,
            Set<Long> expectedFailureIds
    ) {
        int successCount = 0;
        int failureCount = 0;
        List<TriggerCycleResult.EmployeeFailure> failures = new ArrayList<>();
        
        // Update cycle status to ACTIVE
        jdbcTemplate.update(
            "UPDATE appraisal_cycles SET status = ?, updated_at = GETUTCDATE() WHERE id = ?",
            "ACTIVE",
            cycleId
        );
        
        // Process each employee individually with partial failure resilience
        for (Long employeeId : employeeIds) {
            try {
                // Get manager ID for the employee
                Long managerId = jdbcTemplate.queryForObject(
                    "SELECT manager_id FROM users WHERE id = ?", 
                    Long.class, 
                    employeeId
                );
                
                // Validate manager exists (simulates business logic validation)
                if (managerId == null) {
                    throw new IllegalStateException("Employee has no assigned manager");
                }
                
                // Create appraisal form
                String sql = "INSERT INTO appraisal_forms " +
                             "(cycle_id, employee_id, manager_id, template_id, status, created_at, updated_at) " +
                             "VALUES (?, ?, ?, ?, ?, GETUTCDATE(), GETUTCDATE())";
                
                jdbcTemplate.update(sql, cycleId, employeeId, managerId, templateId, "NOT_STARTED");
                
                successCount++;
                
            } catch (Exception ex) {
                // Log individual failure and continue processing remaining employees
                failureCount++;
                failures.add(new TriggerCycleResult.EmployeeFailure(
                    employeeId,
                    ex.getMessage() != null ? ex.getMessage() : "Unknown error"
                ));
            }
        }
        
        return new TriggerCycleResult(
            employeeIds.size(),
            successCount,
            failureCount,
            failures
        );
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

    private List<Long> getEmployeeIdsWithFormsInCycle(Long cycleId) {
        return jdbcTemplate.queryForList(
            "SELECT DISTINCT employee_id FROM appraisal_forms WHERE cycle_id = ?", 
            Long.class, 
            cycleId
        );
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
 * 
 * 3. Implement the missing repositories:
 *    - Create AppraisalFormRepository in src/main/java/com/tns/appraisal/form/AppraisalFormRepository.java
 *    - Create UserRepository in src/main/java/com/tns/appraisal/user/UserRepository.java
 * 
 * 4. Complete the CycleService.triggerCycle() implementation:
 *    - Uncomment the TODO sections in CycleService.triggerCycle()
 *    - Inject AppraisalFormRepository and UserRepository
 *    - Implement form creation logic with try-catch for individual failures
 *    - Ensure failures don't abort the entire operation
 *    - Return TriggerCycleResult with accurate counts
 * 
 * 5. Update the test to use the actual service:
 *    - Replace triggerCycleWithPartialFailures() helper method
 *    - Call cycleService.triggerCycle(cycleId, employeeIds, currentUserId) instead
 *    - Inject CycleService into the test class
 * 
 * 6. Configure test database:
 *    - Update TestPropertySource to point to a test database
 *    - Ensure Flyway migrations run on test database
 *    - Add cleanup scripts to reset test data between runs
 * 
 * 7. Run the test:
 *    mvn test -Dtest=BulkTriggerPartialFailurePropertyTest
 * 
 * Expected behavior:
 * - Test should run 100 iterations with random employee counts and failure rates
 * - Each iteration should verify exactly N-K forms are created for N employees with K failures
 * - All failures should be logged in the result
 * - No employee should be silently unprocessed
 * - All assertions should pass, confirming Property 18 holds
 * 
 * Key validation points:
 * - successCount + failureCount = totalEmployees (all accounted for)
 * - Forms in database = successCount (only successful forms persisted)
 * - Failure list size = failureCount (all failures logged)
 * - No forms exist for failed employees (consistent state)
 * - All successful forms have correct status and template
 */
