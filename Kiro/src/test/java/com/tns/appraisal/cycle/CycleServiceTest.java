package com.tns.appraisal.cycle;

import com.tns.appraisal.audit.AuditLogService;
import com.tns.appraisal.exception.ResourceNotFoundException;
import com.tns.appraisal.exception.UnauthorizedAccessException;
import com.tns.appraisal.exception.ValidationException;
import com.tns.appraisal.template.AppraisalTemplate;
import com.tns.appraisal.template.AppraisalTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CycleService.
 */
@ExtendWith(MockitoExtension.class)
class CycleServiceTest {

    @Mock
    private AppraisalCycleRepository cycleRepository;

    @Mock
    private AppraisalTemplateRepository templateRepository;

    @Mock
    private AuditLogService auditLogService;

    private CycleService cycleService;

    @BeforeEach
    void setUp() {
        cycleService = new CycleService(cycleRepository, templateRepository, auditLogService);
    }

    @Test
    void create_withValidCycle_shouldSucceed() {
        // Arrange
        AppraisalTemplate template = createTemplate(1L);
        AppraisalCycle cycle = createCycle(null, "2025-26 Cycle", template);
        AppraisalCycle savedCycle = createCycle(1L, "2025-26 Cycle", template);
        savedCycle.setCreatedBy(100L);

        when(cycleRepository.save(any(AppraisalCycle.class))).thenReturn(savedCycle);

        // Act
        AppraisalCycle result = cycleService.create(cycle, 100L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("DRAFT", result.getStatus());
        assertEquals(100L, result.getCreatedBy());
        verify(cycleRepository).save(any(AppraisalCycle.class));
        verify(auditLogService).logAsync(eq(100L), eq("CYCLE_CREATED"), eq("AppraisalCycle"), 
                eq(1L), anyMap(), isNull());
    }

    @Test
    void create_withNullName_shouldThrowValidationException() {
        // Arrange
        AppraisalTemplate template = createTemplate(1L);
        AppraisalCycle cycle = createCycle(null, null, template);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, 
                () -> cycleService.create(cycle, 100L));
        assertEquals("Cycle name is required", exception.getMessage());
        verify(cycleRepository, never()).save(any());
    }

    @Test
    void create_withEmptyName_shouldThrowValidationException() {
        // Arrange
        AppraisalTemplate template = createTemplate(1L);
        AppraisalCycle cycle = createCycle(null, "   ", template);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, 
                () -> cycleService.create(cycle, 100L));
        assertEquals("Cycle name is required", exception.getMessage());
    }

    @Test
    void create_withNullStartDate_shouldThrowValidationException() {
        // Arrange
        AppraisalTemplate template = createTemplate(1L);
        AppraisalCycle cycle = new AppraisalCycle();
        cycle.setName("Test Cycle");
        cycle.setStartDate(null);
        cycle.setEndDate(LocalDate.of(2025, 12, 31));
        cycle.setTemplate(template);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, 
                () -> cycleService.create(cycle, 100L));
        assertEquals("Start date is required", exception.getMessage());
    }

    @Test
    void create_withEndDateBeforeStartDate_shouldThrowValidationException() {
        // Arrange
        AppraisalTemplate template = createTemplate(1L);
        AppraisalCycle cycle = new AppraisalCycle();
        cycle.setName("Test Cycle");
        cycle.setStartDate(LocalDate.of(2025, 12, 31));
        cycle.setEndDate(LocalDate.of(2025, 1, 1));
        cycle.setTemplate(template);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, 
                () -> cycleService.create(cycle, 100L));
        assertEquals("End date must be after start date", exception.getMessage());
    }

    @Test
    void create_withNullTemplate_shouldThrowValidationException() {
        // Arrange
        AppraisalCycle cycle = createCycle(null, "Test Cycle", null);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, 
                () -> cycleService.create(cycle, 100L));
        assertEquals("Template is required", exception.getMessage());
    }

    @Test
    void update_withValidData_shouldSucceed() {
        // Arrange
        AppraisalTemplate template = createTemplate(1L);
        AppraisalCycle existingCycle = createCycle(1L, "Old Name", template);
        AppraisalCycle updatedCycle = createCycle(null, "New Name", template);

        when(cycleRepository.findById(1L)).thenReturn(Optional.of(existingCycle));
        when(cycleRepository.save(any(AppraisalCycle.class))).thenReturn(existingCycle);

        // Act
        AppraisalCycle result = cycleService.update(1L, updatedCycle, 100L);

        // Assert
        assertNotNull(result);
        assertEquals("New Name", result.getName());
        verify(cycleRepository).findById(1L);
        verify(cycleRepository).save(existingCycle);
        verify(auditLogService).logAsync(eq(100L), eq("CYCLE_UPDATED"), eq("AppraisalCycle"), 
                eq(1L), anyMap(), isNull());
    }

    @Test
    void update_withNonExistentId_shouldThrowResourceNotFoundException() {
        // Arrange
        AppraisalTemplate template = createTemplate(1L);
        AppraisalCycle updatedCycle = createCycle(null, "New Name", template);

        when(cycleRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, 
                () -> cycleService.update(999L, updatedCycle, 100L));
        assertTrue(exception.getMessage().contains("not found"));
        verify(cycleRepository, never()).save(any());
    }

    @Test
    void findById_withExistingId_shouldReturnCycle() {
        // Arrange
        AppraisalTemplate template = createTemplate(1L);
        AppraisalCycle cycle = createCycle(1L, "Test Cycle", template);

        when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));

        // Act
        AppraisalCycle result = cycleService.findById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Cycle", result.getName());
        verify(cycleRepository).findById(1L);
    }

    @Test
    void findById_withNonExistentId_shouldThrowResourceNotFoundException() {
        // Arrange
        when(cycleRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> cycleService.findById(999L));
    }

    @Test
    void findAll_shouldReturnAllCycles() {
        // Arrange
        AppraisalTemplate template = createTemplate(1L);
        List<AppraisalCycle> cycles = List.of(
                createCycle(1L, "Cycle 1", template),
                createCycle(2L, "Cycle 2", template)
        );

        when(cycleRepository.findAll()).thenReturn(cycles);

        // Act
        List<AppraisalCycle> result = cycleService.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(cycleRepository).findAll();
    }

    @Test
    void delete_withExistingId_shouldSucceed() {
        // Arrange
        AppraisalTemplate template = createTemplate(1L);
        AppraisalCycle cycle = createCycle(1L, "Test Cycle", template);

        when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));

        // Act
        cycleService.delete(1L, 100L);

        // Assert
        verify(cycleRepository).findById(1L);
        verify(cycleRepository).delete(cycle);
        verify(auditLogService).logAsync(eq(100L), eq("CYCLE_DELETED"), eq("AppraisalCycle"), 
                eq(1L), anyMap(), isNull());
    }

    @Test
    void delete_withNonExistentId_shouldThrowResourceNotFoundException() {
        // Arrange
        when(cycleRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> cycleService.delete(999L, 100L));
        verify(cycleRepository, never()).delete(any());
    }

    @Test
    void triggerCycle_withValidData_shouldSucceed() {
        // Arrange
        AppraisalTemplate template = createTemplate(1L);
        AppraisalCycle cycle = createCycle(1L, "Test Cycle", template);
        List<Long> employeeIds = List.of(10L, 20L, 30L);

        when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
        when(templateRepository.findByIsActiveTrue()).thenReturn(Optional.of(template));
        when(cycleRepository.save(any(AppraisalCycle.class))).thenReturn(cycle);

        // Act
        TriggerCycleResult result = cycleService.triggerCycle(1L, employeeIds, 100L);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getTotalEmployees());
        assertEquals(3, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertTrue(result.getFailures().isEmpty());
        verify(cycleRepository).findById(1L);
        verify(templateRepository).findByIsActiveTrue();
        verify(cycleRepository).save(cycle);
        assertEquals("ACTIVE", cycle.getStatus());
        verify(auditLogService).logAsync(eq(100L), eq("CYCLE_TRIGGERED"), eq("AppraisalCycle"), 
                eq(1L), anyMap(), isNull());
    }

    @Test
    void triggerCycle_withEmptyEmployeeList_shouldReturnZeroCounts() {
        // Arrange
        AppraisalTemplate template = createTemplate(1L);
        AppraisalCycle cycle = createCycle(1L, "Test Cycle", template);
        List<Long> employeeIds = List.of();

        when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
        when(templateRepository.findByIsActiveTrue()).thenReturn(Optional.of(template));
        when(cycleRepository.save(any(AppraisalCycle.class))).thenReturn(cycle);

        // Act
        TriggerCycleResult result = cycleService.triggerCycle(1L, employeeIds, 100L);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalEmployees());
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertTrue(result.getFailures().isEmpty());
        assertEquals("ACTIVE", cycle.getStatus());
    }

    @Test
    void triggerCycle_withNonExistentCycle_shouldThrowResourceNotFoundException() {
        // Arrange
        List<Long> employeeIds = List.of(10L, 20L);

        when(cycleRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, 
                () -> cycleService.triggerCycle(999L, employeeIds, 100L));
    }

    @Test
    void triggerCycle_withNoActiveTemplate_shouldThrowValidationException() {
        // Arrange
        AppraisalTemplate template = createTemplate(1L);
        AppraisalCycle cycle = createCycle(1L, "Test Cycle", template);
        List<Long> employeeIds = List.of(10L, 20L);

        when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
        when(templateRepository.findByIsActiveTrue()).thenReturn(Optional.empty());

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, 
                () -> cycleService.triggerCycle(1L, employeeIds, 100L));
        assertEquals("No active appraisal template found", exception.getMessage());
    }

    @Test
    void reopenForm_withHRRole_shouldSucceed() {
        // Arrange
        AppraisalTemplate template = createTemplate(1L);
        AppraisalCycle cycle = createCycle(1L, "Test Cycle", template);
        List<String> hrRoles = List.of("HR");

        when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));

        // Act
        cycleService.reopenForm(1L, 100L, 200L, hrRoles);

        // Assert
        verify(cycleRepository).findById(1L);
        verify(auditLogService).logAsync(eq(200L), eq("FORM_REOPENED"), eq("AppraisalForm"), 
                eq(100L), anyMap(), isNull());
    }

    @Test
    void reopenForm_withHRRoleAmongMultipleRoles_shouldSucceed() {
        // Arrange
        AppraisalTemplate template = createTemplate(1L);
        AppraisalCycle cycle = createCycle(1L, "Test Cycle", template);
        List<String> multipleRoles = List.of("EMPLOYEE", "HR", "MANAGER");

        when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));

        // Act
        cycleService.reopenForm(1L, 100L, 200L, multipleRoles);

        // Assert
        verify(cycleRepository).findById(1L);
        verify(auditLogService).logAsync(eq(200L), eq("FORM_REOPENED"), eq("AppraisalForm"), 
                eq(100L), anyMap(), isNull());
    }

    @Test
    void reopenForm_withoutHRRole_shouldThrowUnauthorizedAccessException() {
        // Arrange
        List<String> nonHrRoles = List.of("EMPLOYEE", "MANAGER");

        // Act & Assert
        UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class, 
                () -> cycleService.reopenForm(1L, 100L, 200L, nonHrRoles));
        assertEquals("Only HR users can reopen forms", exception.getMessage());
        verify(cycleRepository, never()).findById(any());
        verify(auditLogService, never()).logAsync(any(), any(), any(), any(), any(), any());
    }

    @Test
    void reopenForm_withEmptyRolesList_shouldThrowUnauthorizedAccessException() {
        // Arrange
        List<String> emptyRoles = List.of();

        // Act & Assert
        UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class, 
                () -> cycleService.reopenForm(1L, 100L, 200L, emptyRoles));
        assertEquals("Only HR users can reopen forms", exception.getMessage());
        verify(cycleRepository, never()).findById(any());
    }

    @Test
    void reopenForm_withNonExistentCycle_shouldThrowResourceNotFoundException() {
        // Arrange
        List<String> hrRoles = List.of("HR");

        when(cycleRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, 
                () -> cycleService.reopenForm(999L, 100L, 200L, hrRoles));
        assertTrue(exception.getMessage().contains("Appraisal cycle not found"));
        assertTrue(exception.getMessage().contains("999"));
        verify(auditLogService, never()).logAsync(any(), any(), any(), any(), any(), any());
    }

    @Test
    void assignBackupReviewer_withValidData_shouldSucceed() {
        // Arrange
        AppraisalTemplate template = createTemplate(1L);
        AppraisalCycle cycle = createCycle(1L, "Test Cycle", template);

        when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));

        // Act
        cycleService.assignBackupReviewer(1L, 100L, 300L, 200L);

        // Assert
        verify(cycleRepository).findById(1L);
        verify(auditLogService).logAsync(eq(200L), eq("BACKUP_REVIEWER_ASSIGNED"), 
                eq("AppraisalForm"), eq(100L), anyMap(), isNull());
    }

    @Test
    void assignBackupReviewer_withNonExistentCycle_shouldThrowResourceNotFoundException() {
        // Arrange
        when(cycleRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, 
                () -> cycleService.assignBackupReviewer(999L, 100L, 300L, 200L));
        assertTrue(exception.getMessage().contains("Appraisal cycle not found"));
        assertTrue(exception.getMessage().contains("999"));
        verify(auditLogService, never()).logAsync(any(), any(), any(), any(), any(), any());
    }

    // TODO: Add these tests when AppraisalFormRepository and UserRepository are available:
    //
    // @Test
    // void assignBackupReviewer_withNonExistentForm_shouldThrowResourceNotFoundException() {
    //     // Validates STEP 1: Form existence check
    //     // Arrange
    //     AppraisalTemplate template = createTemplate(1L);
    //     AppraisalCycle cycle = createCycle(1L, "Test Cycle", template);
    //
    //     when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
    //     when(appraisalFormRepository.findById(999L)).thenReturn(Optional.empty());
    //
    //     // Act & Assert
    //     ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
    //             () -> cycleService.assignBackupReviewer(1L, 999L, 300L, 200L));
    //     assertTrue(exception.getMessage().contains("Appraisal form not found"));
    //     assertTrue(exception.getMessage().contains("999"));
    //     verify(auditLogService, never()).logAsync(any(), any(), any(), any(), any(), any());
    // }
    //
    // @Test
    // void assignBackupReviewer_withFormFromDifferentCycle_shouldThrowValidationException() {
    //     // Validates STEP 2: Form belongs to specified cycle (Requirement 15.2)
    //     // Arrange
    //     AppraisalTemplate template = createTemplate(1L);
    //     AppraisalCycle cycle = createCycle(1L, "Test Cycle", template);
    //     AppraisalForm form = createForm(100L, 2L, 50L, 60L); // Form belongs to cycle 2, not cycle 1
    //
    //     when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
    //     when(appraisalFormRepository.findById(100L)).thenReturn(Optional.of(form));
    //
    //     // Act & Assert
    //     ValidationException exception = assertThrows(ValidationException.class,
    //             () -> cycleService.assignBackupReviewer(1L, 100L, 300L, 200L));
    //     assertTrue(exception.getMessage().contains("does not belong to cycle"));
    //     assertTrue(exception.getMessage().contains("100")); // form ID
    //     assertTrue(exception.getMessage().contains("1"));   // expected cycle ID
    //     assertTrue(exception.getMessage().contains("2"));   // actual cycle ID
    //     verify(auditLogService, never()).logAsync(any(), any(), any(), any(), any(), any());
    // }
    //
    // @Test
    // void assignBackupReviewer_withNonExistentBackupReviewer_shouldThrowResourceNotFoundException() {
    //     // Validates STEP 3: Backup reviewer user exists
    //     // Arrange
    //     AppraisalTemplate template = createTemplate(1L);
    //     AppraisalCycle cycle = createCycle(1L, "Test Cycle", template);
    //     AppraisalForm form = createForm(100L, 1L, 50L, 60L);
    //
    //     when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
    //     when(appraisalFormRepository.findById(100L)).thenReturn(Optional.of(form));
    //     when(userRepository.findById(999L)).thenReturn(Optional.empty());
    //
    //     // Act & Assert
    //     ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
    //             () -> cycleService.assignBackupReviewer(1L, 100L, 999L, 200L));
    //     assertTrue(exception.getMessage().contains("Backup reviewer user not found"));
    //     assertTrue(exception.getMessage().contains("999"));
    //     verify(auditLogService, never()).logAsync(any(), any(), any(), any(), any(), any());
    // }
    //
    // @Test
    // void assignBackupReviewer_withBackupReviewerLackingManagerOrHrRole_shouldThrowValidationException() {
    //     // Validates STEP 4: Backup reviewer has appropriate role (Requirements 3.7, 3.8)
    //     // Arrange
    //     AppraisalTemplate template = createTemplate(1L);
    //     AppraisalCycle cycle = createCycle(1L, "Test Cycle", template);
    //     AppraisalForm form = createForm(100L, 1L, 50L, 60L);
    //     User backupReviewer = createUser(300L, "EMPLOYEE"); // Only EMPLOYEE role, not MANAGER or HR
    //
    //     when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
    //     when(appraisalFormRepository.findById(100L)).thenReturn(Optional.of(form));
    //     when(userRepository.findById(300L)).thenReturn(Optional.of(backupReviewer));
    //
    //     // Act & Assert
    //     ValidationException exception = assertThrows(ValidationException.class,
    //             () -> cycleService.assignBackupReviewer(1L, 100L, 300L, 200L));
    //     assertTrue(exception.getMessage().contains("cannot be assigned as backup reviewer"));
    //     assertTrue(exception.getMessage().contains("must have MANAGER or HR role"));
    //     assertTrue(exception.getMessage().contains("300")); // user ID
    //     verify(appraisalFormRepository, never()).save(any());
    //     verify(auditLogService, never()).logAsync(any(), any(), any(), any(), any(), any());
    // }
    //
    // @Test
    // void assignBackupReviewer_withManagerRole_shouldSucceed() {
    //     // Validates STEP 4 & 5: Manager can be assigned as backup reviewer (Requirement 3.7)
    //     // Validates Property 19: Backup reviewer gets same permissions as primary manager
    //     // Arrange
    //     AppraisalTemplate template = createTemplate(1L);
    //     AppraisalCycle cycle = createCycle(1L, "Test Cycle", template);
    //     AppraisalForm form = createForm(100L, 1L, 50L, 60L);
    //     User backupReviewer = createUser(300L, "MANAGER");
    //
    //     when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
    //     when(appraisalFormRepository.findById(100L)).thenReturn(Optional.of(form));
    //     when(userRepository.findById(300L)).thenReturn(Optional.of(backupReviewer));
    //     when(appraisalFormRepository.save(any(AppraisalForm.class))).thenReturn(form);
    //
    //     // Act
    //     cycleService.assignBackupReviewer(1L, 100L, 300L, 200L);
    //
    //     // Assert
    //     verify(cycleRepository).findById(1L);
    //     verify(appraisalFormRepository).findById(100L);
    //     verify(userRepository).findById(300L);
    //     verify(appraisalFormRepository).save(argThat(savedForm ->
    //             savedForm.getBackupReviewerId().equals(300L) &&
    //             savedForm.getUpdatedAt() != null
    //     ));
    //     verify(auditLogService).logAsync(eq(200L), eq("BACKUP_REVIEWER_ASSIGNED"),
    //             eq("AppraisalForm"), eq(100L), anyMap(), isNull());
    // }
    //
    // @Test
    // void assignBackupReviewer_withHrRole_shouldSucceed() {
    //     // Validates STEP 4 & 5: HR can be assigned as backup HR delegate (Requirement 3.8)
    //     // Validates Property 19: Backup reviewer gets same permissions as primary manager
    //     // Arrange
    //     AppraisalTemplate template = createTemplate(1L);
    //     AppraisalCycle cycle = createCycle(1L, "Test Cycle", template);
    //     AppraisalForm form = createForm(100L, 1L, 50L, 60L);
    //     User backupReviewer = createUser(300L, "HR");
    //
    //     when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
    //     when(appraisalFormRepository.findById(100L)).thenReturn(Optional.of(form));
    //     when(userRepository.findById(300L)).thenReturn(Optional.of(backupReviewer));
    //     when(appraisalFormRepository.save(any(AppraisalForm.class))).thenReturn(form);
    //
    //     // Act
    //     cycleService.assignBackupReviewer(1L, 100L, 300L, 200L);
    //
    //     // Assert
    //     verify(cycleRepository).findById(1L);
    //     verify(appraisalFormRepository).findById(100L);
    //     verify(userRepository).findById(300L);
    //     verify(appraisalFormRepository).save(argThat(savedForm ->
    //             savedForm.getBackupReviewerId().equals(300L) &&
    //             savedForm.getUpdatedAt() != null
    //     ));
    //     verify(auditLogService).logAsync(eq(200L), eq("BACKUP_REVIEWER_ASSIGNED"),
    //             eq("AppraisalForm"), eq(100L), anyMap(), isNull());
    // }
    //
    // @Test
    // void assignBackupReviewer_withMultipleRolesIncludingManager_shouldSucceed() {
    //     // Validates STEP 4: User with multiple roles including MANAGER can be backup reviewer
    //     // Arrange
    //     AppraisalTemplate template = createTemplate(1L);
    //     AppraisalCycle cycle = createCycle(1L, "Test Cycle", template);
    //     AppraisalForm form = createForm(100L, 1L, 50L, 60L);
    //     User backupReviewer = createUser(300L, "EMPLOYEE", "MANAGER"); // Multiple roles
    //
    //     when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
    //     when(appraisalFormRepository.findById(100L)).thenReturn(Optional.of(form));
    //     when(userRepository.findById(300L)).thenReturn(Optional.of(backupReviewer));
    //     when(appraisalFormRepository.save(any(AppraisalForm.class))).thenReturn(form);
    //
    //     // Act
    //     cycleService.assignBackupReviewer(1L, 100L, 300L, 200L);
    //
    //     // Assert
    //     verify(appraisalFormRepository).save(argThat(savedForm ->
    //             savedForm.getBackupReviewerId().equals(300L)
    //     ));
    //     verify(auditLogService).logAsync(eq(200L), eq("BACKUP_REVIEWER_ASSIGNED"),
    //             eq("AppraisalForm"), eq(100L), anyMap(), isNull());
    // }
    //
    // @Test
    // void assignBackupReviewer_updatesFormTimestamp() {
    //     // Validates STEP 6: Form's last modified timestamp is updated
    //     // Arrange
    //     AppraisalTemplate template = createTemplate(1L);
    //     AppraisalCycle cycle = createCycle(1L, "Test Cycle", template);
    //     AppraisalForm form = createForm(100L, 1L, 50L, 60L);
    //     User backupReviewer = createUser(300L, "MANAGER");
    //     LocalDateTime beforeUpdate = LocalDateTime.now();
    //
    //     when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
    //     when(appraisalFormRepository.findById(100L)).thenReturn(Optional.of(form));
    //     when(userRepository.findById(300L)).thenReturn(Optional.of(backupReviewer));
    //     when(appraisalFormRepository.save(any(AppraisalForm.class))).thenReturn(form);
    //
    //     // Act
    //     cycleService.assignBackupReviewer(1L, 100L, 300L, 200L);
    //
    //     // Assert
    //     verify(appraisalFormRepository).save(argThat(savedForm -> {
    //         LocalDateTime updatedAt = savedForm.getUpdatedAt();
    //         return updatedAt != null && 
    //                !updatedAt.isBefore(beforeUpdate) &&
    //                !updatedAt.isAfter(LocalDateTime.now().plusSeconds(1));
    //     }));
    // }
    //
    // Helper method to create AppraisalForm (add when entity is available):
    // private AppraisalForm createForm(Long id, Long cycleId, Long employeeId, Long managerId) {
    //     AppraisalForm form = new AppraisalForm();
    //     form.setId(id);
    //     form.setCycleId(cycleId);
    //     form.setEmployeeId(employeeId);
    //     form.setManagerId(managerId);
    //     form.setStatus("NOT_STARTED");
    //     form.setCreatedAt(LocalDateTime.now());
    //     form.setUpdatedAt(LocalDateTime.now());
    //     return form;
    // }
    //
    // Helper method to create User with roles (add when entity is available):
    // private User createUser(Long id, String... roleNames) {
    //     User user = new User();
    //     user.setId(id);
    //     user.setEmployeeId("EMP" + id);
    //     user.setFullName("User " + id);
    //     user.setEmail("user" + id + "@example.com");
    //     
    //     Set<Role> roles = new HashSet<>();
    //     for (String roleName : roleNames) {
    //         Role role = new Role();
    //         role.setName(roleName);
    //         roles.add(role);
    //     }
    //     user.setRoles(roles);
    //     return user;
    // }

    // Helper methods

    private AppraisalTemplate createTemplate(Long id) {
        AppraisalTemplate template = new AppraisalTemplate();
        template.setId(id);
        template.setVersion("3.0");
        template.setSchemaJson("{}");
        template.setIsActive(true);
        return template;
    }

    private AppraisalCycle createCycle(Long id, String name, AppraisalTemplate template) {
        AppraisalCycle cycle = new AppraisalCycle();
        cycle.setId(id);
        cycle.setName(name);
        cycle.setStartDate(LocalDate.of(2025, 1, 1));
        cycle.setEndDate(LocalDate.of(2025, 12, 31));
        cycle.setTemplate(template);
        cycle.setStatus("DRAFT");
        return cycle;
    }
}
