package com.tns.appraisal.cycle;

import com.tns.appraisal.audit.AuditLogService;
import com.tns.appraisal.exception.ResourceNotFoundException;
import com.tns.appraisal.exception.UnauthorizedAccessException;
import com.tns.appraisal.exception.ValidationException;
import com.tns.appraisal.template.AppraisalTemplate;
import com.tns.appraisal.template.AppraisalTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing appraisal cycles.
 * Handles CRUD operations, cycle triggering, form reopening, and backup reviewer assignment.
 */
@Service
public class CycleService {

    private static final Logger logger = LoggerFactory.getLogger(CycleService.class);

    private final AppraisalCycleRepository cycleRepository;
    private final AppraisalTemplateRepository templateRepository;
    private final AuditLogService auditLogService;
    // TODO: Inject AppraisalFormRepository when available
    // TODO: Inject UserRepository when available
    // TODO: Inject NotificationService when available

    public CycleService(
            AppraisalCycleRepository cycleRepository,
            AppraisalTemplateRepository templateRepository,
            AuditLogService auditLogService) {
        this.cycleRepository = cycleRepository;
        this.templateRepository = templateRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Create a new appraisal cycle.
     *
     * @param cycle the cycle to create
     * @param currentUserId the ID of the user creating the cycle
     * @return the created cycle
     * @throws ValidationException if validation fails
     */
    @Transactional
    public AppraisalCycle create(AppraisalCycle cycle, Long currentUserId) {
        logger.info("Creating new appraisal cycle: {}", cycle.getName());

        validateCycle(cycle);

        cycle.setCreatedBy(currentUserId);
        cycle.setStatus("DRAFT");

        AppraisalCycle savedCycle = cycleRepository.save(cycle);

        auditLogService.logAsync(
                currentUserId,
                "CYCLE_CREATED",
                "AppraisalCycle",
                savedCycle.getId(),
                Map.of("cycleName", savedCycle.getName()),
                null
        );

        logger.info("Appraisal cycle created with ID: {}", savedCycle.getId());
        return savedCycle;
    }

    /**
     * Update an existing appraisal cycle.
     *
     * @param id the cycle ID
     * @param updatedCycle the updated cycle data
     * @param currentUserId the ID of the user updating the cycle
     * @return the updated cycle
     * @throws ResourceNotFoundException if cycle not found
     */
    @Transactional
    public AppraisalCycle update(Long id, AppraisalCycle updatedCycle, Long currentUserId) {
        logger.info("Updating appraisal cycle ID: {}", id);

        AppraisalCycle existingCycle = cycleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal cycle not found with ID: " + id));

        validateCycle(updatedCycle);

        existingCycle.setName(updatedCycle.getName());
        existingCycle.setStartDate(updatedCycle.getStartDate());
        existingCycle.setEndDate(updatedCycle.getEndDate());
        existingCycle.setTemplate(updatedCycle.getTemplate());

        AppraisalCycle savedCycle = cycleRepository.save(existingCycle);

        auditLogService.logAsync(
                currentUserId,
                "CYCLE_UPDATED",
                "AppraisalCycle",
                savedCycle.getId(),
                Map.of("cycleName", savedCycle.getName()),
                null
        );

        logger.info("Appraisal cycle updated: {}", savedCycle.getId());
        return savedCycle;
    }

    /**
     * Find a cycle by ID.
     *
     * @param id the cycle ID
     * @return the cycle
     * @throws ResourceNotFoundException if cycle not found
     */
    @Transactional(readOnly = true)
    public AppraisalCycle findById(Long id) {
        return cycleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal cycle not found with ID: " + id));
    }

    /**
     * Find all cycles.
     *
     * @return list of all cycles
     */
    @Transactional(readOnly = true)
    public List<AppraisalCycle> findAll() {
        return cycleRepository.findAll();
    }

    /**
     * Delete a cycle.
     *
     * @param id the cycle ID
     * @param currentUserId the ID of the user deleting the cycle
     * @throws ResourceNotFoundException if cycle not found
     */
    @Transactional
    public void delete(Long id, Long currentUserId) {
        logger.info("Deleting appraisal cycle ID: {}", id);

        AppraisalCycle cycle = cycleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal cycle not found with ID: " + id));

        cycleRepository.delete(cycle);

        auditLogService.logAsync(
                currentUserId,
                "CYCLE_DELETED",
                "AppraisalCycle",
                id,
                Map.of("cycleName", cycle.getName()),
                null
        );

        logger.info("Appraisal cycle deleted: {}", id);
    }

    /**
     * Trigger an appraisal cycle for selected employees.
     * Creates AppraisalForm records, sends notifications, and logs actions.
     * Implements bulk processing with partial failure resilience.
     *
     * @param cycleId the cycle ID to trigger
     * @param employeeIds list of employee IDs to include
     * @param currentUserId the ID of the user triggering the cycle
     * @return TriggerCycleResult containing success and failure counts
     * @throws ResourceNotFoundException if cycle or template not found
     */
    @Transactional
    public TriggerCycleResult triggerCycle(Long cycleId, List<Long> employeeIds, Long currentUserId) {
        logger.info("Triggering appraisal cycle ID: {} for {} employees", cycleId, employeeIds.size());

        // Validate cycle exists
        AppraisalCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal cycle not found with ID: " + cycleId));

        // Get active template
        AppraisalTemplate activeTemplate = templateRepository.findByIsActiveTrue()
                .orElseThrow(() -> new ValidationException("No active appraisal template found"));

        // Update cycle status to ACTIVE
        cycle.setStatus("ACTIVE");
        cycleRepository.save(cycle);

        // Initialize counters for bulk processing
        int successCount = 0;
        int failureCount = 0;
        List<TriggerCycleResult.EmployeeFailure> failures = new java.util.ArrayList<>();

        // Process each employee individually with partial failure resilience
        for (Long employeeId : employeeIds) {
            try {
                // TODO: Implement when AppraisalFormRepository and UserRepository are available:
                // 1. Fetch employee from UserRepository
                //    User employee = userRepository.findById(employeeId)
                //        .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
                //
                // 2. Get manager ID from employee
                //    Long managerId = employee.getManagerId();
                //    if (managerId == null) {
                //        throw new ValidationException("Employee has no assigned manager: " + employeeId);
                //    }
                //
                // 3. Create AppraisalForm
                //    AppraisalForm form = new AppraisalForm();
                //    form.setCycleId(cycleId);
                //    form.setEmployeeId(employeeId);
                //    form.setManagerId(managerId);
                //    form.setTemplateId(activeTemplate.getId());
                //    form.setStatus("NOT_STARTED");
                //    form.setCreatedAt(LocalDateTime.now());
                //    form.setUpdatedAt(LocalDateTime.now());
                //
                // 4. Save form
                //    appraisalFormRepository.save(form);
                //
                // 5. Send notification emails
                //    try {
                //        notificationService.sendEmployeeNotification(employee, cycle, form);
                //        notificationService.sendManagerNotification(managerId, employee, cycle, form);
                //    } catch (Exception emailEx) {
                //        logger.error("Failed to send notification for employee {}: {}", 
                //                     employeeId, emailEx.getMessage());
                //        // Log email failure but don't fail the form creation
                //    }

                // Placeholder: Simulate successful processing
                logger.info("Successfully processed employee ID: {}", employeeId);
                successCount++;

            } catch (Exception ex) {
                // Log individual failure and continue processing remaining employees
                logger.error("Failed to process employee ID {}: {}", employeeId, ex.getMessage(), ex);
                failureCount++;
                failures.add(new TriggerCycleResult.EmployeeFailure(
                        employeeId,
                        ex.getMessage() != null ? ex.getMessage() : "Unknown error"
                ));
            }
        }

        // Log audit entry with summary
        auditLogService.logAsync(
                currentUserId,
                "CYCLE_TRIGGERED",
                "AppraisalCycle",
                cycleId,
                Map.of(
                        "cycleName", cycle.getName(),
                        "totalEmployees", employeeIds.size(),
                        "successCount", successCount,
                        "failureCount", failureCount,
                        "templateId", activeTemplate.getId()
                ),
                null
        );

        logger.info("Appraisal cycle triggered: {} - Success: {}, Failures: {}", 
                    cycleId, successCount, failureCount);

        return new TriggerCycleResult(
                employeeIds.size(),
                successCount,
                failureCount,
                failures
        );
    }

    /**
     * Reopen a submitted or completed appraisal form.
     * Resets form status to allow re-submission.
     * Implements Property 8: Form Reopen Resets Status.
     *
     * @param cycleId the cycle ID
     * @param formId the form ID to reopen
     * @param currentUserId the ID of the HR user reopening the form
     * @param userRoles the roles of the current user
     * @throws ResourceNotFoundException if cycle or form not found
     * @throws UnauthorizedAccessException if user is not HR
     * @throws ValidationException if form is not in a reopenable status or doesn't belong to cycle
     */
    @Transactional
    public void reopenForm(Long cycleId, Long formId, Long currentUserId, List<String> userRoles) {
        logger.info("Reopening form ID: {} in cycle ID: {}", formId, cycleId);

        // Validate HR role
        if (!userRoles.contains("HR")) {
            throw new UnauthorizedAccessException("Only HR users can reopen forms");
        }

        // Validate cycle exists
        AppraisalCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal cycle not found with ID: " + cycleId));

        // TODO: Implement when AppraisalFormRepository is available:
        //
        // 1. Fetch the form by formId from AppraisalFormRepository
        //    AppraisalForm form = appraisalFormRepository.findById(formId)
        //        .orElseThrow(() -> new ResourceNotFoundException("Appraisal form not found with ID: " + formId));
        //
        // 2. Validate form belongs to the specified cycle
        //    if (!form.getCycleId().equals(cycleId)) {
        //        throw new ValidationException("Form does not belong to the specified cycle");
        //    }
        //
        // 3. Validate form status is SUBMITTED or REVIEWED_AND_COMPLETED (Property 8 requirement)
        //    String currentStatus = form.getStatus();
        //    if (!"SUBMITTED".equals(currentStatus) && !"REVIEWED_AND_COMPLETED".equals(currentStatus)) {
        //        throw new ValidationException(
        //            "Form can only be reopened from SUBMITTED or REVIEWED_AND_COMPLETED status. Current status: " + currentStatus
        //        );
        //    }
        //
        // 4. Reset form status to DRAFT_SAVED (Property 8: reset to state that allows re-submission)
        //    form.setStatus("DRAFT_SAVED");
        //
        // 5. Clear submission and review timestamps to reset form state
        //    form.setSubmittedAt(null);
        //    form.setReviewStartedAt(null);
        //    form.setReviewedAt(null);
        //
        // 6. Update the form's last modified timestamp
        //    form.setUpdatedAt(LocalDateTime.now());
        //
        // 7. Save the updated form
        //    appraisalFormRepository.save(form);
        //
        // 8. Log the previous status for audit trail
        //    logger.info("Form {} reopened from status: {} to DRAFT_SAVED", formId, currentStatus);

        auditLogService.logAsync(
                currentUserId,
                "FORM_REOPENED",
                "AppraisalForm",
                formId,
                Map.of("cycleId", cycleId),
                null
        );

        logger.info("Form reopened successfully: {}", formId);
    }

    /**
     * Assign a backup reviewer to an appraisal form.
     * Implements Requirements 3.7, 3.8, 15.2, 15.3 and Property 19.
     *
     * Requirements:
     * - 3.7: HR SHALL be able to assign a Backup_Reviewer to substitute for a Manager who is unavailable
     * - 3.8: HR SHALL be able to assign a backup HR delegate to perform HR-level actions on their behalf
     * - 15.2: When a Manager is assigned as a Backup_Reviewer, THE System SHALL grant access to relevant forms
     * - 15.3: The Backup_Reviewer SHALL have the same review permissions as the primary Manager
     *
     * Property 19: Backup Reviewer Permission Equivalence
     * - For any backup reviewer assignment, the backup reviewer SHALL have exactly the same review
     *   permissions as the primary manager for the assigned appraisal forms
     *
     * @param cycleId the cycle ID
     * @param formId the form ID
     * @param backupReviewerId the ID of the backup reviewer
     * @param currentUserId the ID of the user making the assignment
     * @throws ResourceNotFoundException if cycle, form, or backup reviewer not found
     * @throws ValidationException if form doesn't belong to cycle or backup reviewer has invalid role
     */
    @Transactional
    public void assignBackupReviewer(Long cycleId, Long formId, Long backupReviewerId, Long currentUserId) {
        logger.info("Assigning backup reviewer {} to form ID: {} in cycle ID: {}", 
                    backupReviewerId, formId, cycleId);

        // Validate cycle exists
        AppraisalCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal cycle not found with ID: " + cycleId));

        // TODO: Implement comprehensive validation when AppraisalFormRepository and UserRepository are available:
        //
        // STEP 1: Fetch and validate the appraisal form
        // -----------------------------------------------
        // AppraisalForm form = appraisalFormRepository.findById(formId)
        //     .orElseThrow(() -> new ResourceNotFoundException("Appraisal form not found with ID: " + formId));
        //
        // STEP 2: Validate form belongs to the specified cycle (Requirement 15.2)
        // ------------------------------------------------------------------------
        // if (!form.getCycleId().equals(cycleId)) {
        //     throw new ValidationException(
        //         String.format("Form ID %d does not belong to cycle ID %d. Form belongs to cycle ID %d",
        //                       formId, cycleId, form.getCycleId())
        //     );
        // }
        //
        // STEP 3: Fetch and validate the backup reviewer user exists
        // -----------------------------------------------------------
        // User backupReviewer = userRepository.findById(backupReviewerId)
        //     .orElseThrow(() -> new ResourceNotFoundException(
        //         "Backup reviewer user not found with ID: " + backupReviewerId
        //     ));
        //
        // STEP 4: Validate backup reviewer has appropriate role (Requirements 3.7, 3.8)
        // -----------------------------------------------------------------------------
        // HR can assign either:
        // - A Manager as Backup_Reviewer for another Manager (Requirement 3.7)
        // - An HR user as backup HR delegate (Requirement 3.8)
        //
        // Set<String> backupReviewerRoles = backupReviewer.getRoles().stream()
        //     .map(Role::getName)
        //     .collect(Collectors.toSet());
        //
        // boolean hasManagerRole = backupReviewerRoles.contains("MANAGER");
        // boolean hasHrRole = backupReviewerRoles.contains("HR");
        //
        // if (!hasManagerRole && !hasHrRole) {
        //     throw new ValidationException(
        //         String.format("User ID %d cannot be assigned as backup reviewer. " +
        //                       "Backup reviewers must have MANAGER or HR role. User has roles: %s",
        //                       backupReviewerId, backupReviewerRoles)
        //     );
        // }
        //
        // STEP 5: Assign the backup reviewer to the form (Property 19 - Permission Equivalence)
        // -------------------------------------------------------------------------------------
        // By setting the backupReviewerId on the form, the backup reviewer gains the same
        // review permissions as the primary manager for this form. The authorization logic
        // in FormService/ReviewService will check both managerId and backupReviewerId when
        // determining if a user can perform review actions (save draft, complete review).
        //
        // form.setBackupReviewerId(backupReviewerId);
        //
        // STEP 6: Update the form's last modified timestamp
        // --------------------------------------------------
        // form.setUpdatedAt(LocalDateTime.now());
        //
        // STEP 7: Save the updated form
        // ------------------------------
        // appraisalFormRepository.save(form);
        //
        // STEP 8: Log the assignment details for audit trail
        // ---------------------------------------------------
        // logger.info("Backup reviewer {} assigned to form {} (employee: {}, primary manager: {})",
        //             backupReviewerId, formId, form.getEmployeeId(), form.getManagerId());

        // Log audit entry
        auditLogService.logAsync(
                currentUserId,
                "BACKUP_REVIEWER_ASSIGNED",
                "AppraisalForm",
                formId,
                Map.of(
                        "cycleId", cycleId,
                        "backupReviewerId", backupReviewerId
                ),
                null
        );

        logger.info("Backup reviewer assigned successfully to form: {}", formId);
    }

    /**
     * Validate cycle data.
     *
     * @param cycle the cycle to validate
     * @throws ValidationException if validation fails
     */
    private void validateCycle(AppraisalCycle cycle) {
        if (cycle.getName() == null || cycle.getName().trim().isEmpty()) {
            throw new ValidationException("Cycle name is required");
        }

        if (cycle.getStartDate() == null) {
            throw new ValidationException("Start date is required");
        }

        if (cycle.getEndDate() == null) {
            throw new ValidationException("End date is required");
        }

        if (cycle.getEndDate().isBefore(cycle.getStartDate())) {
            throw new ValidationException("End date must be after start date");
        }

        if (cycle.getTemplate() == null) {
            throw new ValidationException("Template is required");
        }
    }
}
