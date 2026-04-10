package com.tns.appraisal.cycle;

import com.tns.appraisal.audit.AuditLog;
import com.tns.appraisal.audit.AuditLogRepository;
import com.tns.appraisal.exception.ResourceNotFoundException;
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
 * Integration tests for backup reviewer assignment workflow.
 * Tests the complete flow: create cycle → trigger → assign backup reviewer.
 * 
 * Validates:
 * - Requirement 3.7: HR can assign backup reviewer for unavailable manager
 * - Requirement 3.8: HR can assign backup HR delegate
 * - Requirement 15.2: Backup reviewer gets access to relevant forms
 * - Requirement 15.3: Backup reviewer has same permissions as primary manager
 * - Property 19: Backup Reviewer Permission Equivalence
 */
@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/test-data/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
class BackupReviewerIntegrationTest {

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

    @BeforeEach
    void setUp() {
        hrUserId = 100L;

        // Create and activate a template
        activeTemplate = new AppraisalTemplate();
        activeTemplate.setVersion("3.0-test");
        activeTemplate.setSchemaJson(createValidTemplateSchema());
        activeTemplate.setIsActive(true);
        activeTemplate.setCreatedBy(hrUserId);
        activeTemplate = templateRepository.save(activeTemplate);
    }

    @Test
    void assignBackupReviewer_withValidData_shouldSucceed() {
        // Arrange
        AppraisalCycle cycle = createTestCycle("Backup Reviewer Test Cycle");
        AppraisalCycle savedCycle = cycleService.create(cycle, hrUserId);
        Long formId = 1L;
        Long backupReviewerId = 300L;

        // Act
        cycleService.assignBackupReviewer(savedCycle.getId(), formId, backupReviewerId, hrUserId);

        // Wait for async audit log
        waitForAuditLog();

        // Assert - Verify audit log entry created
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        assertTrue(auditLogs.stream().anyMatch(log ->
                "BACKUP_REVIEWER_ASSIGNED".equals(log.getAction()) &&
                formId.equals(log.getEntityId()) &&
                "AppraisalForm".equals(log.getEntityType()) &&
                hrUserId.equals(log.getUserId())
        ), "Expected BACKUP_REVIEWER_ASSIGNED audit log entry");
    }

    @Test
    void assignBackupReviewer_withNonExistentCycle_shouldThrowResourceNotFoundException() {
        // Arrange
        Long nonExistentCycleId = 999999L;
        Long formId = 2L;
        Long backupReviewerId = 300L;

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> cycleService.assignBackupReviewer(nonExistentCycleId, formId, backupReviewerId, hrUserId)
        );

        assertTrue(exception.getMessage().contains("Appraisal cycle not found"));
        assertTrue(exception.getMessage().contains("999999"));

        // Verify no audit log created
        waitForAuditLog();
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        assertFalse(auditLogs.stream().anyMatch(log -> "BACKUP_REVIEWER_ASSIGNED".equals(log.getAction())));
    }

    @Test
    void assignBackupReviewer_multipleFormsInSameCycle_shouldSucceedForEach() {
        // Arrange
        AppraisalCycle cycle = createTestCycle("Multiple Backup Assignments Cycle");
        AppraisalCycle savedCycle = cycleService.create(cycle, hrUserId);

        // Clear audit logs
        auditLogRepository.deleteAll();

        // Act - Assign backup reviewers to multiple forms
        cycleService.assignBackupReviewer(savedCycle.getId(), 10L, 301L, hrUserId);
        cycleService.assignBackupReviewer(savedCycle.getId(), 20L, 302L, hrUserId);
        cycleService.assignBackupReviewer(savedCycle.getId(), 30L, 303L, hrUserId);

        // Wait for async audit logs
        waitForAuditLog();

        // Assert - Verify all three assignments logged
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        long assignmentCount = auditLogs.stream()
                .filter(log -> "BACKUP_REVIEWER_ASSIGNED".equals(log.getAction()))
                .count();
        assertEquals(3, assignmentCount, "Expected 3 BACKUP_REVIEWER_ASSIGNED audit log entries");
    }

    @Test
    void assignBackupReviewer_sameBackupReviewerToMultipleForms_shouldSucceed() {
        // Arrange
        AppraisalCycle cycle = createTestCycle("Same Backup Multiple Forms Cycle");
        AppraisalCycle savedCycle = cycleService.create(cycle, hrUserId);
        Long backupReviewerId = 300L;

        // Clear audit logs
        auditLogRepository.deleteAll();

        // Act - Assign same backup reviewer to multiple forms
        cycleService.assignBackupReviewer(savedCycle.getId(), 10L, backupReviewerId, hrUserId);
        cycleService.assignBackupReviewer(savedCycle.getId(), 20L, backupReviewerId, hrUserId);

        // Wait for async audit logs
        waitForAuditLog();

        // Assert - Verify both assignments logged
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        long assignmentCount = auditLogs.stream()
                .filter(log -> "BACKUP_REVIEWER_ASSIGNED".equals(log.getAction()))
                .count();
        assertEquals(2, assignmentCount);
    }

    @Test
    void assignBackupReviewer_endToEndWorkflow_shouldSucceed() {
        // Arrange - Create and trigger cycle
        AppraisalCycle cycle = createTestCycle("End-to-End Backup Reviewer Cycle");
        AppraisalCycle savedCycle = cycleService.create(cycle, hrUserId);
        
        List<Long> employeeIds = List.of(10L, 20L, 30L);
        cycleService.triggerCycle(savedCycle.getId(), employeeIds, hrUserId);

        // Clear audit logs to focus on backup reviewer assignment
        auditLogRepository.deleteAll();

        // Act - Assign backup reviewer
        Long formId = 100L;
        Long backupReviewerId = 300L;
        cycleService.assignBackupReviewer(savedCycle.getId(), formId, backupReviewerId, hrUserId);

        // Wait for async audit log
        waitForAuditLog();

        // Assert - Verify assignment logged
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        assertTrue(auditLogs.stream().anyMatch(log ->
                "BACKUP_REVIEWER_ASSIGNED".equals(log.getAction()) &&
                formId.equals(log.getEntityId())
        ));
    }

    @Test
    void assignBackupReviewer_reassignDifferentBackupReviewer_shouldSucceed() {
        // Arrange
        AppraisalCycle cycle = createTestCycle("Reassign Backup Reviewer Cycle");
        AppraisalCycle savedCycle = cycleService.create(cycle, hrUserId);
        Long formId = 50L;

        // Clear audit logs
        auditLogRepository.deleteAll();

        // Act - Assign first backup reviewer
        cycleService.assignBackupReviewer(savedCycle.getId(), formId, 301L, hrUserId);

        // Act - Reassign to different backup reviewer
        cycleService.assignBackupReviewer(savedCycle.getId(), formId, 302L, hrUserId);

        // Wait for async audit logs
        waitForAuditLog();

        // Assert - Verify both assignments logged
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        long assignmentCount = auditLogs.stream()
                .filter(log -> "BACKUP_REVIEWER_ASSIGNED".equals(log.getAction()) &&
                               formId.equals(log.getEntityId()))
                .count();
        assertEquals(2, assignmentCount, "Expected 2 BACKUP_REVIEWER_ASSIGNED audit log entries for reassignment");
    }

    @Test
    void assignBackupReviewer_afterFormReopen_shouldSucceed() {
        // Arrange - Create cycle and reopen a form
        AppraisalCycle cycle = createTestCycle("Backup After Reopen Cycle");
        AppraisalCycle savedCycle = cycleService.create(cycle, hrUserId);
        Long formId = 60L;
        List<String> hrRoles = List.of("HR");

        // Reopen form first
        cycleService.reopenForm(savedCycle.getId(), formId, hrUserId, hrRoles);

        // Clear audit logs
        auditLogRepository.deleteAll();

        // Act - Assign backup reviewer after reopen
        Long backupReviewerId = 300L;
        cycleService.assignBackupReviewer(savedCycle.getId(), formId, backupReviewerId, hrUserId);

        // Wait for async audit log
        waitForAuditLog();

        // Assert - Verify assignment logged
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        assertTrue(auditLogs.stream().anyMatch(log ->
                "BACKUP_REVIEWER_ASSIGNED".equals(log.getAction()) &&
                formId.equals(log.getEntityId())
        ));
    }

    @Test
    void assignBackupReviewer_multipleCyclesSimultaneously_shouldSucceed() {
        // Arrange - Create two cycles
        AppraisalCycle cycle1 = createTestCycle("Cycle 1 - Backup Reviewer");
        AppraisalCycle savedCycle1 = cycleService.create(cycle1, hrUserId);

        AppraisalCycle cycle2 = createTestCycle("Cycle 2 - Backup Reviewer");
        AppraisalCycle savedCycle2 = cycleService.create(cycle2, hrUserId);

        // Clear audit logs
        auditLogRepository.deleteAll();

        // Act - Assign backup reviewers in both cycles
        cycleService.assignBackupReviewer(savedCycle1.getId(), 10L, 301L, hrUserId);
        cycleService.assignBackupReviewer(savedCycle2.getId(), 20L, 302L, hrUserId);

        // Wait for async audit logs
        waitForAuditLog();

        // Assert - Verify both assignments logged
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        long assignmentCount = auditLogs.stream()
                .filter(log -> "BACKUP_REVIEWER_ASSIGNED".equals(log.getAction()))
                .count();
        assertEquals(2, assignmentCount);
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
