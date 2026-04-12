package com.tns.appraisal.cycle;

import com.tns.appraisal.audit.AuditLogService;
import com.tns.appraisal.exception.ResourceNotFoundException;
import com.tns.appraisal.exception.UnauthorizedAccessException;
import com.tns.appraisal.exception.ValidationException;
import com.tns.appraisal.form.AppraisalForm;
import com.tns.appraisal.form.AppraisalFormDto;
import com.tns.appraisal.form.AppraisalFormRepository;
import com.tns.appraisal.template.AppraisalTemplate;
import com.tns.appraisal.user.User;
import com.tns.appraisal.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing appraisal cycles.
 * Handles CRUD operations, cycle triggering, form reopening, and backup reviewer assignment.
 */
@Service
public class CycleService {

    private static final Logger logger = LoggerFactory.getLogger(CycleService.class);

    private static final Set<String> REOPENABLE_STATUSES = Set.of("SUBMITTED", "REVIEWED_AND_COMPLETED");

    private final AppraisalCycleRepository cycleRepository;
    private final AppraisalFormRepository formRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public CycleService(
            AppraisalCycleRepository cycleRepository,
            AppraisalFormRepository formRepository,
            UserRepository userRepository,
            AuditLogService auditLogService) {
        this.cycleRepository = cycleRepository;
        this.formRepository = formRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

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

    @Transactional
    public AppraisalCycle update(Long id, AppraisalCycle updatedCycle, Long currentUserId) {
        logger.info("Updating appraisal cycle ID: {}", id);

        AppraisalCycle existingCycle = cycleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal cycle not found with ID: " + id));

        if (!"DRAFT".equals(existingCycle.getStatus())) {
            throw new ValidationException("Only DRAFT cycles can be updated");
        }

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

    @Transactional(readOnly = true)
    public AppraisalCycle findById(Long id) {
        return cycleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal cycle not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<AppraisalCycle> findAll() {
        return cycleRepository.findAll();
    }

    @Transactional
    public void delete(Long id, Long currentUserId) {
        logger.info("Deleting appraisal cycle ID: {}", id);

        AppraisalCycle cycle = cycleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal cycle not found with ID: " + id));

        if (!"DRAFT".equals(cycle.getStatus())) {
            throw new ValidationException("Only DRAFT cycles can be deleted");
        }

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
     * Get all forms for a given cycle, mapped to DTOs with employee/manager names.
     */
    @Transactional(readOnly = true)
    public List<AppraisalFormDto> getCycleForms(Long cycleId) {
        cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal cycle not found with ID: " + cycleId));

        List<AppraisalForm> forms = formRepository.findByCycleId(cycleId);
        return forms.stream()
                .map(AppraisalFormDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Trigger an appraisal cycle for selected employees.
     * Creates AppraisalForm records using the cycle's assigned template.
     * Implements bulk processing with partial failure resilience.
     */
    @Transactional
    public TriggerCycleResult triggerCycle(Long cycleId, List<Long> employeeIds, Long currentUserId) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            throw new ValidationException("At least one employee must be selected");
        }

        logger.info("Triggering appraisal cycle ID: {} for {} employees", cycleId, employeeIds.size());

        AppraisalCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal cycle not found with ID: " + cycleId));

        if (!"DRAFT".equals(cycle.getStatus()) && !"ACTIVE".equals(cycle.getStatus())) {
            throw new ValidationException("Cycle must be in DRAFT or ACTIVE status to trigger. Current: " + cycle.getStatus());
        }

        AppraisalTemplate cycleTemplate = cycle.getTemplate();
        if (cycleTemplate == null) {
            throw new ValidationException("Cycle has no assigned template");
        }

        cycle.setStatus("ACTIVE");
        cycleRepository.save(cycle);

        int successCount = 0;
        int failureCount = 0;
        List<TriggerCycleResult.EmployeeFailure> failures = new ArrayList<>();

        for (Long employeeId : employeeIds) {
            try {
                if (formRepository.existsByCycleIdAndEmployeeId(cycleId, employeeId)) {
                    throw new ValidationException("Form already exists for this employee in this cycle");
                }

                User employee = userRepository.findById(employeeId)
                        .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));

                User manager = employee.getManager();
                if (manager == null) {
                    throw new ValidationException("Employee has no assigned manager: " + employee.getFullName());
                }

                AppraisalForm form = new AppraisalForm();
                form.setCycle(cycle);
                form.setEmployee(employee);
                form.setManager(manager);
                form.setTemplate(cycleTemplate);
                form.setStatus("NOT_STARTED");

                formRepository.save(form);

                logger.info("Created form for employee {} (ID: {})", employee.getFullName(), employeeId);
                successCount++;

            } catch (Exception ex) {
                logger.error("Failed to process employee ID {}: {}", employeeId, ex.getMessage());
                failureCount++;
                failures.add(new TriggerCycleResult.EmployeeFailure(
                        employeeId,
                        ex.getMessage() != null ? ex.getMessage() : "Unknown error"
                ));
            }
        }

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
                        "templateId", cycleTemplate.getId()
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
     * Resets form status to DRAFT_SAVED to allow re-submission.
     */
    @Transactional
    public void reopenForm(Long cycleId, Long formId, Long currentUserId, List<String> userRoles) {
        logger.info("Reopening form ID: {} in cycle ID: {}", formId, cycleId);

        if (!userRoles.contains("HR")) {
            throw new UnauthorizedAccessException("Only HR users can reopen forms");
        }

        cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal cycle not found with ID: " + cycleId));

        AppraisalForm form = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal form not found with ID: " + formId));

        if (!form.getCycle().getId().equals(cycleId)) {
            throw new ValidationException("Form does not belong to the specified cycle");
        }

        String currentStatus = form.getStatus();
        if (!REOPENABLE_STATUSES.contains(currentStatus)) {
            throw new ValidationException(
                    "Form can only be reopened from SUBMITTED or REVIEWED_AND_COMPLETED status. Current: " + currentStatus
            );
        }

        form.setStatus("DRAFT_SAVED");
        form.setSubmittedAt(null);
        form.setReviewStartedAt(null);
        form.setReviewedAt(null);

        formRepository.save(form);

        auditLogService.logAsync(
                currentUserId,
                "FORM_REOPENED",
                "AppraisalForm",
                formId,
                Map.of("cycleId", cycleId, "previousStatus", currentStatus),
                null
        );

        logger.info("Form {} reopened from status: {} to DRAFT_SAVED", formId, currentStatus);
    }

    /**
     * Assign a backup reviewer to an appraisal form.
     * The backup reviewer must have MANAGER or HR role.
     */
    @Transactional
    public void assignBackupReviewer(Long cycleId, Long formId, Long backupReviewerId, Long currentUserId) {
        logger.info("Assigning backup reviewer {} to form ID: {} in cycle ID: {}",
                    backupReviewerId, formId, cycleId);

        cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal cycle not found with ID: " + cycleId));

        AppraisalForm form = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal form not found with ID: " + formId));

        if (!form.getCycle().getId().equals(cycleId)) {
            throw new ValidationException("Form does not belong to the specified cycle");
        }

        User backupReviewer = userRepository.findById(backupReviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Backup reviewer user not found with ID: " + backupReviewerId));

        Set<String> backupRoles = backupReviewer.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet());

        if (!backupRoles.contains("MANAGER") && !backupRoles.contains("HR")) {
            throw new ValidationException(
                    "Backup reviewers must have MANAGER or HR role. User has roles: " + backupRoles
            );
        }

        if (form.getEmployee().getId().equals(backupReviewerId)) {
            throw new ValidationException("The appraisee cannot be assigned as their own backup reviewer");
        }

        form.setBackupReviewer(backupReviewer);
        formRepository.save(form);

        auditLogService.logAsync(
                currentUserId,
                "BACKUP_REVIEWER_ASSIGNED",
                "AppraisalForm",
                formId,
                Map.of(
                        "cycleId", cycleId,
                        "backupReviewerId", backupReviewerId,
                        "backupReviewerName", backupReviewer.getFullName()
                ),
                null
        );

        logger.info("Backup reviewer {} assigned to form {}", backupReviewerId, formId);
    }

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
