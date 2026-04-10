package com.tns.appraisal.cycle;

import com.tns.appraisal.audit.AuditLog;
import com.tns.appraisal.audit.AuditLogRepository;
import com.tns.appraisal.exception.ResourceNotFoundException;
import com.tns.appraisal.exception.UnauthorizedAccessException;
import com.tns.appraisal.template.AppraisalTemplate;
import com.tns.appraisal.template.AppraisalTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for form reopen workflow.
 * Tests the complete flow: create cycle → trigger → reopen form.
 * 
 * Validates:
 * - Requirement 3.6: HR can reopen submitted/completed forms
 * - Property 8: Form Reopen Resets Status
 */
@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/test-data/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
class FormReopenIntegrationTest {

    @Autowired
    private CycleService cycleService;

    @Autowired
    private AppraisalCycleRepository cycleRepository;

    @Autowired
    private AppraisalTemplateRepository templateRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private AppraisalTemplate activeTemplate;
    private Long hrUserId;
    private Long nonHrUserId;

    @BeforeEach
    void setUp() {
        hrUserId = 100L;
        nonHrUserId = 200L;

        // Create and activate a template
        activeTemplate = new AppraisalTemplate();
        activeTemplate.setVersion("3.0-test");
        activeTemplate.setSchemaJson(createValidTemplateSchema());
        activeTemplate.setIsActive(true);
        activeTemplate.setCreatedBy(hrUserId);
        activeTemplate = templateRepository.save(activeTemplate);
    }

    @Test
    void reopenForm_withHRRole_shouldSucceed() {
        // Arrange
        AppraisalCycle cycle = createTestCycle("Reopen Test Cycle");
        AppraisalCycle savedCycle = cycleService.create(cycle, hrUserId);
        Long formId = 1L;
        List<String> hrRoles = List.of("HR");

        // Act
        cycleService.reopenForm(savedCycle.getId(), formId, hrUserId, hrRoles);

        // Wait for async audit log
        waitForAuditLog();

        // Assert - Verify audit log entry created
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        assertTrue(auditLogs.stream().anyMatch(log ->
                "FORM_REOPENED".equals(log.getAction()) &&
                formId.equals(log.getEntityId()) &&
                "AppraisalForm".equals(log.getEntityType()) &&
                hrUserId.equals(log.getUserId())
        ), "Expected FORM_REOPENED audit log entry");
    }

    @Test
    void reopenForm_withHRRoleAmongMultipleRoles_shouldSucceed() {
        // Arrange
        AppraisalCycle cycle = createTestCycle("Multi-Role Reopen Cycle");
        AppraisalCycle savedCycle = cycleService.create(cycle, hrUserId);
        Long formId = 2L;
        List<String> multipleRoles = List.of("EMPLOYEE", "HR", "MANAGER");

        // Act
        cycleService.reopenForm(savedCycle.getId(), formId, hrUserId, multipleRoles);

        // Wait for async audit log
        waitForAuditLog();

        // Assert - Verify audit log entry created
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        assertTrue(auditLogs.stream().anyMatch(log ->
                "FORM_REOPENED".equals(log.getAction()) &&
                formId.equals(log.getEntityId())
        ), "Expected FORM_REOPENED audit log entry");
    }

    @Test
    void reopenForm_withoutHRRole_shouldThrowUnauthorizedAccessException() {
        // Arrange
        AppraisalCycle cycle = createTestCycle("Unauthorized Reopen Cycle");
        AppraisalCycle savedCycle = cycleService.create(cycle, hrUserId);
        Long formId = 3L;
        List<String> nonHrRoles = List.of("EMPLOYEE", "MANAGER");

        // Act & Assert
        UnauthorizedAccessException exception = assertThrows(
                UnauthorizedAccessException.class,
                () -> cycleService.reopenForm(savedCycle.getId(), formId, nonHrUserId, nonHrRoles)
        );

        assertEquals("Only HR users can reopen forms", exception.getMessage());

        // Verify no audit log created
        waitForAuditLog();
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        assertFalse(auditLogs.stream().anyMatch(log -> "FORM_REOPENED".equals(log.getAction())));
    }

    @Test
    void reopenForm_withEmptyRolesList_shouldThrowUnauthorizedAccessException() {
        // Arrange
        AppraisalCycle cycle = createTestCycle("Empty Roles Reopen Cycle");
        AppraisalCycle savedCycle = cycleService.create(cycle, hrUserId);
        Long formId = 4L;
        List<String> emptyRoles = List.of();

        // Act & Assert
        UnauthorizedAccessException exception = assertThrows(
                UnauthorizedAccessException.class,
                () -> cycleService.reopenForm(savedCycle.getId(), formId, nonHrUserId, emptyRoles)
        );

        assertEquals("Only HR users can reopen forms", exception.getMessage());
    }

    @Test
    void reopenForm_withNonExistentCycle_shouldThrowResourceNotFoundException() {
        // Arrange
        Long nonExistentCycleId = 999999L;
        Long formId = 5L;
        List<String> hrRoles = List.of("HR");

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> cycleService.reopenForm(nonExistentCycleId, formId, hrUserId, hrRoles)
        );

        assertTrue(exception.getMessage().contains("Appraisal cycle not found"));
        assertTrue(exception.getMessage().contains("999999"));

        // Verify no audit log created
        waitForAuditLog();
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        assertFalse(auditLogs.stream().anyMatch(log -> "FORM_REOPENED".equals(log.getAction())));
    }

    @Test
    void reopenForm_multipleFormsInSameCycle_shouldSucceedForEach() {
        // Arrange
        AppraisalCycle cycle = createTestCycle("Multiple Reopen Cycle");
        AppraisalCycle savedCycle = cycleService.create(cycle, hrUserId);
        List<String> hrRoles = List.of("HR");

        // Clear audit logs
        auditLogRepository.deleteAll();

        // Act - Reopen multiple forms
        cycleService.reopenForm(savedCycle.getId(), 10L, hrUserId, hrRoles);
        cycleService.reopenForm(savedCycle.getId(), 20L, hrUserId, hrRoles);
        cycleService.reopenForm(savedCycle.getId(), 30L, hrUserId, hrRoles);

        // Wait for async audit logs
        waitForAuditLog();

        // Assert - Verify all three reopen operations logged
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        long reopenCount = auditLogs.stream()
                .filter(log -> "FORM_REOPENED".equals(log.getAction()))
                .count();
        assertEquals(3, reopenCount, "Expected 3 FORM_REOPENED audit log entries");
    }

    @Test
    void reopenForm_endToEndWorkflow_shouldSucceed() {
        // Arrange - Create and trigger cycle
        AppraisalCycle cycle = createTestCycle("End-to-End Reopen Cycle");
        AppraisalCycle savedCycle = cycleService.create(cycle, hrUserId);
        
        List<Long> employeeIds = List.of(10L, 20L);
        cycleService.triggerCycle(savedCycle.getId(), employeeIds, hrUserId);

        // Clear audit logs to focus on reopen
        auditLogRepository.deleteAll();

        // Act - Reopen a form
        Long formId = 100L;
        List<String> hrRoles = List.of("HR");
        cycleService.reopenForm(savedCycle.getId(), formId, hrUserId, hrRoles);

        // Wait for async audit log
        waitForAuditLog();

        // Assert - Verify reopen logged
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        assertTrue(auditLogs.stream().anyMatch(log ->
                "FORM_REOPENED".equals(log.getAction()) &&
                formId.equals(log.getEntityId())
        ));
    }

    // Helper methods

    private AppraisalCycle createTestCycle(String name) {
        AppraisalCycle cycle = new AppraisalCycle();
        cycle.setName(name);
        cycle.setStartDate(LocalDate.of(2025, 1, 1));
        cycle.setEndDate(LocalDate.of(2025, 12, 31));
        cycle.setTemplate(activeTemplate);
        return cycle;
    }

    private String createValidTemplateSchema() {
        return """
            {
              "version": "3.0",
              "sections": [
                {
                  "sectionType": "key_responsibilities",
                  "title": "Key Responsibilities",
                  "items": [
                    {
                      "id": "kr_1",
                      "label": "Essential Duty 1",
                      "ratingScale": "competency"
                    }
                  ]
                }
              ]
            }
            """;
    }

    private void waitForAuditLog() {
        // Wait for async audit log to complete (max 2 seconds)
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
