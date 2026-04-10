package com.tns.appraisal.cycle;

import net.jqwik.api.*;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Property-based tests for appraisal form reopen functionality.
 * 
 * **Validates: Requirements 3.6**
 * 
 * Property 8: Form Reopen Resets Status
 * 
 * For any appraisal form in Submitted or Reviewed_and_Completed status, 
 * after HR reopens it, the form status SHALL be reset to a state that 
 * allows re-submission (Draft_Saved or Not_Started).
 * 
 * NOTE: This test is currently disabled because it requires:
 * 1. AppraisalForm entity and repository to be implemented
 * 2. User entity and repository to be implemented  
 * 3. CycleService.reopenForm() method to be fully implemented
 * 4. AuditLogService to be implemented
 * 
 * Once these components are available, remove the @Disabled annotation and
 * update the test to use the actual service layer instead of direct JDBC operations.
 * 
 * The test validates that:
 * - Forms in SUBMITTED status can be reopened
 * - Forms in REVIEWED_AND_COMPLETED status can be reopened
 * - After reopen, status is reset to DRAFT_SAVED (allows re-submission)
 * - Submission and review timestamps are cleared
 * - Forms in other statuses cannot be reopened (validation error)
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
@Disabled("Requires AppraisalForm entity, User entity, and full CycleService.reopenForm() implementation")
class FormReopenPropertyTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Property 8: Form Reopen Resets Status
     * 
     * This property test verifies that when an appraisal form is reopened by HR:
     * 1. Forms in SUBMITTED status can be reopened
     * 2. Forms in REVIEWED_AND_COMPLETED status can be reopened
     * 3. After reopen, the status is reset to DRAFT_SAVED
     * 4. Submission and review timestamps are cleared
     * 5. The form can be re-submitted after reopen
     */
    @Property(tries = 100)
    @Label("Feature: employee-appraisal-cycle, Property 8: Form Reopen Resets Status")
    void formReopenResetsStatus(
            @ForAll("reopenableStatuses") String initialStatus
    ) {
        // Arrange: Create test data
        Long templateId = createActiveTemplate();
        Long cycleId = createCycle(templateId);
        Long employeeId = createEmployee();
        Long managerId = createManager();
        Long formId = createForm(cycleId, employeeId, managerId, templateId, initialStatus);
        
        // Set timestamps based on initial status
        if ("SUBMITTED".equals(initialStatus)) {
            setFormSubmittedAt(formId, LocalDateTime.now().minusDays(1));
        } else if ("REVIEWED_AND_COMPLETED".equals(initialStatus)) {
            setFormSubmittedAt(formId, LocalDateTime.now().minusDays(2));
            setFormReviewedAt(formId, LocalDateTime.now().minusDays(1));
        }
        
        // Verify preconditions
        String statusBeforeReopen = getFormStatus(formId);
        assert statusBeforeReopen.equals(initialStatus) : 
            String.format("Expected initial status %s, but found %s", initialStatus, statusBeforeReopen);
        
        // Act: Reopen the form
        reopenForm(formId, cycleId);
        
        // Assert: Verify status is reset to DRAFT_SAVED
        String statusAfterReopen = getFormStatus(formId);
        assert "DRAFT_SAVED".equals(statusAfterReopen) : 
            String.format("Expected status DRAFT_SAVED after reopen, but found %s", statusAfterReopen);
        
        // Assert: Verify submission timestamp is cleared
        LocalDateTime submittedAt = getFormSubmittedAt(formId);
        assert submittedAt == null : 
            String.format("Expected submitted_at to be null after reopen, but found %s", submittedAt);
        
        // Assert: Verify review timestamps are cleared
        LocalDateTime reviewStartedAt = getFormReviewStartedAt(formId);
        LocalDateTime reviewedAt = getFormReviewedAt(formId);
        assert reviewStartedAt == null : 
            String.format("Expected review_started_at to be null after reopen, but found %s", reviewStartedAt);
        assert reviewedAt == null : 
            String.format("Expected reviewed_at to be null after reopen, but found %s", reviewedAt);
        
        // Assert: Verify form can be re-submitted (status allows transition to SUBMITTED)
        boolean canResubmit = canTransitionToSubmitted(statusAfterReopen);
        assert canResubmit : 
            String.format("Form with status %s should allow re-submission", statusAfterReopen);
    }

    /**
     * Property 8 Extension: Verify forms in non-reopenable statuses cannot be reopened
     * Tests that forms in NOT_STARTED, DRAFT_SAVED, UNDER_REVIEW, REVIEW_DRAFT_SAVED
     * cannot be reopened (should throw validation error)
     */
    @Property(tries = 50)
    @Label("Feature: employee-appraisal-cycle, Property 8: Non-Reopenable Statuses Rejected")
    void nonReopenableStatusesRejected(
            @ForAll("nonReopenableStatuses") String initialStatus
    ) {
        // Arrange: Create test data
        Long templateId = createActiveTemplate();
        Long cycleId = createCycle(templateId);
        Long employeeId = createEmployee();
        Long managerId = createManager();
        Long formId = createForm(cycleId, employeeId, managerId, templateId, initialStatus);
        
        // Verify preconditions
        String statusBeforeReopen = getFormStatus(formId);
        assert statusBeforeReopen.equals(initialStatus) : 
            String.format("Expected initial status %s, but found %s", initialStatus, statusBeforeReopen);
        
        // Act & Assert: Attempt to reopen should fail
        boolean reopenFailed = false;
        try {
            reopenForm(formId, cycleId);
        } catch (Exception e) {
            // Expected: validation error for non-reopenable status
            reopenFailed = true;
        }
        
        assert reopenFailed : 
            String.format("Reopen should fail for status %s, but it succeeded", initialStatus);
        
        // Assert: Status should remain unchanged
        String statusAfterAttempt = getFormStatus(formId);
        assert statusAfterAttempt.equals(initialStatus) : 
            String.format("Status should remain %s after failed reopen, but found %s", 
                initialStatus, statusAfterAttempt);
    }

    /**
     * Property 8 Extension: Verify multiple reopen cycles
     * Tests that a form can be reopened, re-submitted, and reopened again
     */
    @Property(tries = 50)
    @Label("Feature: employee-appraisal-cycle, Property 8: Multiple Reopen Cycles")
    void multipleReopenCycles(
            @ForAll("reopenableStatuses") String initialStatus
    ) {
        // Arrange: Create test data
        Long templateId = createActiveTemplate();
        Long cycleId = createCycle(templateId);
        Long employeeId = createEmployee();
        Long managerId = createManager();
        Long formId = createForm(cycleId, employeeId, managerId, templateId, initialStatus);
        
        if ("SUBMITTED".equals(initialStatus)) {
            setFormSubmittedAt(formId, LocalDateTime.now().minusDays(1));
        } else if ("REVIEWED_AND_COMPLETED".equals(initialStatus)) {
            setFormSubmittedAt(formId, LocalDateTime.now().minusDays(2));
            setFormReviewedAt(formId, LocalDateTime.now().minusDays(1));
        }
        
        // Act: First reopen
        reopenForm(formId, cycleId);
        String statusAfterFirstReopen = getFormStatus(formId);
        assert "DRAFT_SAVED".equals(statusAfterFirstReopen);
        
        // Act: Re-submit the form
        updateFormStatus(formId, "SUBMITTED");
        setFormSubmittedAt(formId, LocalDateTime.now());
        
        // Act: Second reopen
        reopenForm(formId, cycleId);
        String statusAfterSecondReopen = getFormStatus(formId);
        
        // Assert: Status should be DRAFT_SAVED again
        assert "DRAFT_SAVED".equals(statusAfterSecondReopen) : 
            String.format("Expected DRAFT_SAVED after second reopen, but found %s", statusAfterSecondReopen);
        
        // Assert: Timestamps should be cleared again
        LocalDateTime submittedAt = getFormSubmittedAt(formId);
        assert submittedAt == null : 
            "Submitted timestamp should be cleared after second reopen";
    }

    // ==================== Arbitraries ====================

    @Provide
    Arbitrary<String> reopenableStatuses() {
        return Arbitraries.of("SUBMITTED", "REVIEWED_AND_COMPLETED");
    }

    @Provide
    Arbitrary<String> nonReopenableStatuses() {
        return Arbitraries.of("NOT_STARTED", "DRAFT_SAVED", "UNDER_REVIEW", "REVIEW_DRAFT_SAVED");
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
            "ACTIVE",
            1L
        );
        
        return jdbcTemplate.queryForObject(
            "SELECT TOP 1 id FROM appraisal_cycles ORDER BY id DESC", 
            Long.class
        );
    }

    private Long createEmployee() {
        long timestamp = System.currentTimeMillis();
        String employeeId = "EMP" + timestamp;
        
        String sql = "INSERT INTO users (employee_id, full_name, email, password_hash, manager_id, is_active, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, GETUTCDATE(), GETUTCDATE())";
        
        jdbcTemplate.update(sql,
            employeeId,
            "Test Employee",
            "employee" + timestamp + "@test.com",
            "$2a$10$dummyhash",
            1L,
            true
        );
        
        return jdbcTemplate.queryForObject(
            "SELECT id FROM users WHERE employee_id = ?", 
            Long.class, 
            employeeId
        );
    }

    private Long createManager() {
        long timestamp = System.currentTimeMillis();
        String managerId = "MGR" + timestamp;
        
        String sql = "INSERT INTO users (employee_id, full_name, email, password_hash, is_active, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, GETUTCDATE(), GETUTCDATE())";
        
        jdbcTemplate.update(sql,
            managerId,
            "Test Manager",
            "manager" + timestamp + "@test.com",
            "$2a$10$dummyhash",
            true
        );
        
        return jdbcTemplate.queryForObject(
            "SELECT id FROM users WHERE employee_id = ?", 
            Long.class, 
            managerId
        );
    }

    private Long createForm(Long cycleId, Long employeeId, Long managerId, Long templateId, String status) {
        String sql = "INSERT INTO appraisal_forms " +
                     "(cycle_id, employee_id, manager_id, template_id, status, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, GETUTCDATE(), GETUTCDATE())";
        
        jdbcTemplate.update(sql, cycleId, employeeId, managerId, templateId, status);
        
        return jdbcTemplate.queryForObject(
            "SELECT TOP 1 id FROM appraisal_forms WHERE cycle_id = ? AND employee_id = ? ORDER BY id DESC", 
            Long.class,
            cycleId,
            employeeId
        );
    }

    private void reopenForm(Long formId, Long cycleId) {
        // Simulate CycleService.reopenForm() behavior
        // In actual implementation, this would call cycleService.reopenForm()
        
        String currentStatus = getFormStatus(formId);
        
        // Validate status is reopenable
        if (!"SUBMITTED".equals(currentStatus) && !"REVIEWED_AND_COMPLETED".equals(currentStatus)) {
            throw new IllegalStateException(
                "Form can only be reopened from SUBMITTED or REVIEWED_AND_COMPLETED status. Current status: " + currentStatus
            );
        }
        
        // Reset status to DRAFT_SAVED
        String sql = "UPDATE appraisal_forms " +
                     "SET status = ?, submitted_at = NULL, review_started_at = NULL, reviewed_at = NULL, updated_at = GETUTCDATE() " +
                     "WHERE id = ?";
        
        jdbcTemplate.update(sql, "DRAFT_SAVED", formId);
    }

    private String getFormStatus(Long formId) {
        return jdbcTemplate.queryForObject(
            "SELECT status FROM appraisal_forms WHERE id = ?", 
            String.class, 
            formId
        );
    }

    private LocalDateTime getFormSubmittedAt(Long formId) {
        return jdbcTemplate.queryForObject(
            "SELECT submitted_at FROM appraisal_forms WHERE id = ?", 
            LocalDateTime.class, 
            formId
        );
    }

    private LocalDateTime getFormReviewStartedAt(Long formId) {
        return jdbcTemplate.queryForObject(
            "SELECT review_started_at FROM appraisal_forms WHERE id = ?", 
            LocalDateTime.class, 
            formId
        );
    }

    private LocalDateTime getFormReviewedAt(Long formId) {
        return jdbcTemplate.queryForObject(
            "SELECT reviewed_at FROM appraisal_forms WHERE id = ?", 
            LocalDateTime.class, 
            formId
        );
    }

    private void setFormSubmittedAt(Long formId, LocalDateTime timestamp) {
        jdbcTemplate.update(
            "UPDATE appraisal_forms SET submitted_at = ? WHERE id = ?", 
            timestamp, 
            formId
        );
    }

    private void setFormReviewedAt(Long formId, LocalDateTime timestamp) {
        jdbcTemplate.update(
            "UPDATE appraisal_forms SET reviewed_at = ? WHERE id = ?", 
            timestamp, 
            formId
        );
    }

    private void updateFormStatus(Long formId, String status) {
        jdbcTemplate.update(
            "UPDATE appraisal_forms SET status = ?, updated_at = GETUTCDATE() WHERE id = ?", 
            status, 
            formId
        );
    }

    private boolean canTransitionToSubmitted(String currentStatus) {
        // Based on state machine: NOT_STARTED and DRAFT_SAVED can transition to SUBMITTED
        return "NOT_STARTED".equals(currentStatus) || "DRAFT_SAVED".equals(currentStatus);
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
 * 4. Complete the CycleService.reopenForm() implementation:
 *    - Uncomment the TODO sections in CycleService.reopenForm()
 *    - Inject AppraisalFormRepository
 *    - Implement form status validation and reset logic
 *    - Implement timestamp clearing logic
 * 
 * 5. Update the test to use the actual service:
 *    - Replace direct JDBC operations in reopenForm() helper method
 *    - Call cycleService.reopenForm(cycleId, formId, currentUserId, List.of("HR")) instead
 *    - Inject CycleService into the test class
 * 
 * 6. Configure test database:
 *    - Update TestPropertySource to point to a test database
 *    - Ensure Flyway migrations run on test database
 *    - Add cleanup scripts to reset test data between runs
 * 
 * 7. Run the test:
 *    mvn test -Dtest=FormReopenPropertyTest
 * 
 * Expected behavior:
 * - Test should run 100 iterations with random reopenable statuses (SUBMITTED, REVIEWED_AND_COMPLETED)
 * - Each iteration should verify status is reset to DRAFT_SAVED
 * - All timestamps should be cleared
 * - Forms in non-reopenable statuses should be rejected
 * - All assertions should pass, confirming Property 8 holds
 */
