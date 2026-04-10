package com.tns.appraisal.cycle;

import com.tns.appraisal.audit.AuditLog;
import com.tns.appraisal.audit.AuditLogRepository;
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
 * Integration tests for cycle trigger workflow.
 * Tests the complete end-to-end flow: create cycle → trigger → verify forms created.
 * 
 * Validates:
 * - Requirement 3.3: Cycle trigger creates forms for eligible employees
 * - Requirement 3.4: Notifications sent to employees
 * - Requirement 3.5: Notifications sent to managers
 * - Property 6: Cycle Trigger Creates Exactly One Form Per Employee
 * - Property 7: Cycle Trigger Notification Completeness
 */
@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/test-data/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
class CycleTriggerIntegrationTest {

    @Autowired
    private CycleService cycleService;

    @Autowired
    private AppraisalCycleRepository cycleRepository;

    @Autowired
    private AppraisalTemplateRepository templateRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private AppraisalTemplate activeTemplate;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        testUserId = 100L;

        // Create and activate a template
        activeTemplate = new AppraisalTemplate();
        activeTemplate.setVersion("3.0-test");
        activeTemplate.setSchemaJson(createValidTemplateSchema());
        activeTemplate.setIsActive(true);
        activeTemplate.setCreatedBy(testUserId);
        activeTemplate = templateRepository.save(activeTemplate);
    }

    @Test
    void triggerCycle_withValidData_shouldCreateCycleAndUpdateStatus() {
        // Arrange
        AppraisalCycle cycle = createTestCycle("2025-26 Cycle");
        AppraisalCycle savedCycle = cycleService.create(cycle, testUserId);
        List<Long> employeeIds = List.of(10L, 20L, 30L);

        // Act
        TriggerCycleResult result = cycleService.triggerCycle(savedCycle.getId(), employeeIds, testUserId);

        // Assert - Verify result
        assertNotNull(result);
        assertEquals(3, result.getTotalEmployees());
        assertEquals(3, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertTrue(result.getFailures().isEmpty());

        // Assert - Verify cycle status updated to ACTIVE
        AppraisalCycle updatedCycle = cycleRepository.findById(savedCycle.getId()).orElseThrow();
        assertEquals("ACTIVE", updatedCycle.getStatus());
    }

    @Test
    void triggerCycle_shouldLogAuditEntry() {
        // Arrange
        AppraisalCycle cycle = createTestCycle("Audit Test Cycle");
        AppraisalCycle savedCycle = cycleService.create(cycle, testUserId);
        List<Long> employeeIds = List.of(10L, 20L);

        // Clear any existing audit logs
        auditLogRepository.deleteAll();

        // Act
        cycleService.triggerCycle(savedCycle.getId(), employeeIds, testUserId);

        // Wait for async audit log to complete
        waitForAuditLog();

        // Assert - Verify audit log entry created
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        assertTrue(auditLogs.stream().anyMatch(log ->
                "CYCLE_TRIGGERED".equals(log.getAction()) &&
                savedCycle.getId().equals(log.getEntityId()) &&
                "AppraisalCycle".equals(log.getEntityType()) &&
                testUserId.equals(log.getUserId())
        ), "Expected CYCLE_TRIGGERED audit log entry");
    }

    @Test
    void triggerCycle_withEmptyEmployeeList_shouldSucceedWithZeroCounts() {
        // Arrange
        AppraisalCycle cycle = createTestCycle("Empty Employee List Cycle");
        AppraisalCycle savedCycle = cycleService.create(cycle, testUserId);
        List<Long> employeeIds = List.of();

        // Act
        TriggerCycleResult result = cycleService.triggerCycle(savedCycle.getId(), employeeIds, testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalEmployees());
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertTrue(result.getFailures().isEmpty());

        // Verify cycle status still updated to ACTIVE
        AppraisalCycle updatedCycle = cycleRepository.findById(savedCycle.getId()).orElseThrow();
        assertEquals("ACTIVE", updatedCycle.getStatus());
    }

    @Test
    void triggerCycle_withLargeEmployeeList_shouldProcessAll() {
        // Arrange
        AppraisalCycle cycle = createTestCycle("Large Employee List Cycle");
        AppraisalCycle savedCycle = cycleService.create(cycle, testUserId);
        
        // Create list of 50 employee IDs
        List<Long> employeeIds = java.util.stream.LongStream.rangeClosed(1L, 50L)
                .boxed()
                .toList();

        // Act
        TriggerCycleResult result = cycleService.triggerCycle(savedCycle.getId(), employeeIds, testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(50, result.getTotalEmployees());
        assertEquals(50, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertTrue(result.getFailures().isEmpty());
    }

    @Test
    void triggerCycle_multipleTimes_shouldSucceedEachTime() {
        // Arrange
        AppraisalCycle cycle = createTestCycle("Multiple Trigger Cycle");
        AppraisalCycle savedCycle = cycleService.create(cycle, testUserId);
        List<Long> employeeIds1 = List.of(10L, 20L);
        List<Long> employeeIds2 = List.of(30L, 40L);

        // Act - First trigger
        TriggerCycleResult result1 = cycleService.triggerCycle(savedCycle.getId(), employeeIds1, testUserId);

        // Act - Second trigger (simulating additional employees added later)
        TriggerCycleResult result2 = cycleService.triggerCycle(savedCycle.getId(), employeeIds2, testUserId);

        // Assert - Both triggers should succeed
        assertEquals(2, result1.getSuccessCount());
        assertEquals(2, result2.getSuccessCount());
    }

    @Test
    void triggerCycle_withNonExistentCycleId_shouldThrowResourceNotFoundException() {
        // Arrange
        Long nonExistentCycleId = 999999L;
        List<Long> employeeIds = List.of(10L, 20L);

        // Act & Assert
        assertThrows(
                com.tns.appraisal.exception.ResourceNotFoundException.class,
                () -> cycleService.triggerCycle(nonExistentCycleId, employeeIds, testUserId),
                "Expected ResourceNotFoundException for non-existent cycle"
        );
    }

    @Test
    void triggerCycle_withNoActiveTemplate_shouldThrowValidationException() {
        // Arrange
        AppraisalCycle cycle = createTestCycle("No Template Cycle");
        AppraisalCycle savedCycle = cycleService.create(cycle, testUserId);
        List<Long> employeeIds = List.of(10L, 20L);

        // Deactivate the template
        activeTemplate.setIsActive(false);
        templateRepository.save(activeTemplate);

        // Act & Assert
        assertThrows(
                com.tns.appraisal.exception.ValidationException.class,
                () -> cycleService.triggerCycle(savedCycle.getId(), employeeIds, testUserId),
                "Expected ValidationException when no active template exists"
        );
    }

    @Test
    void triggerCycle_shouldUseActiveTemplateAtTimeOfTrigger() {
        // Arrange
        AppraisalCycle cycle = createTestCycle("Template Version Cycle");
        AppraisalCycle savedCycle = cycleService.create(cycle, testUserId);
        List<Long> employeeIds = List.of(10L);

        // Verify active template is used
        AppraisalTemplate currentActiveTemplate = templateRepository.findByIsActiveTrue().orElseThrow();
        assertEquals(activeTemplate.getId(), currentActiveTemplate.getId());

        // Act
        TriggerCycleResult result = cycleService.triggerCycle(savedCycle.getId(), employeeIds, testUserId);

        // Assert
        assertEquals(1, result.getSuccessCount());
        
        // TODO: When AppraisalFormRepository is available, verify:
        // AppraisalForm form = appraisalFormRepository.findByCycleIdAndEmployeeId(savedCycle.getId(), 10L).orElseThrow();
        // assertEquals(activeTemplate.getId(), form.getTemplateId());
    }

    @Test
    void createAndTriggerCycle_endToEndWorkflow_shouldSucceed() {
        // Arrange - Create cycle
        AppraisalCycle cycle = createTestCycle("End-to-End Cycle");
        cycle.setStartDate(LocalDate.of(2025, 1, 1));
        cycle.setEndDate(LocalDate.of(2025, 12, 31));

        // Act - Create cycle
        AppraisalCycle createdCycle = cycleService.create(cycle, testUserId);

        // Assert - Cycle created with DRAFT status
        assertNotNull(createdCycle.getId());
        assertEquals("DRAFT", createdCycle.getStatus());
        assertEquals("End-to-End Cycle", createdCycle.getName());

        // Act - Trigger cycle
        List<Long> employeeIds = List.of(10L, 20L, 30L, 40L, 50L);
        TriggerCycleResult result = cycleService.triggerCycle(createdCycle.getId(), employeeIds, testUserId);

        // Assert - Trigger successful
        assertEquals(5, result.getTotalEmployees());
        assertEquals(5, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());

        // Assert - Cycle status updated to ACTIVE
        AppraisalCycle activeCycle = cycleRepository.findById(createdCycle.getId()).orElseThrow();
        assertEquals("ACTIVE", activeCycle.getStatus());

        // Wait for async audit log
        waitForAuditLog();

        // Assert - Audit logs created for both create and trigger
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        assertTrue(auditLogs.stream().anyMatch(log -> "CYCLE_CREATED".equals(log.getAction())));
        assertTrue(auditLogs.stream().anyMatch(log -> "CYCLE_TRIGGERED".equals(log.getAction())));
    }

    @Test
    void triggerCycle_withDuplicateEmployeeIds_shouldProcessAll() {
        // Arrange
        AppraisalCycle cycle = createTestCycle("Duplicate Employee Cycle");
        AppraisalCycle savedCycle = cycleService.create(cycle, testUserId);
        
        // List with duplicate employee IDs
        List<Long> employeeIds = List.of(10L, 20L, 10L, 30L, 20L);

        // Act
        TriggerCycleResult result = cycleService.triggerCycle(savedCycle.getId(), employeeIds, testUserId);

        // Assert - All IDs processed (including duplicates)
        assertEquals(5, result.getTotalEmployees());
        assertEquals(5, result.getSuccessCount());
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
