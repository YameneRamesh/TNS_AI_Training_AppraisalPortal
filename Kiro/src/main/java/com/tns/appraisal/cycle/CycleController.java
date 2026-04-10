package com.tns.appraisal.cycle;

import com.tns.appraisal.common.dto.ApiResponse;
import com.tns.appraisal.exception.ResourceNotFoundException;
import com.tns.appraisal.template.AppraisalTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for managing appraisal cycles.
 * Provides endpoints for cycle CRUD operations, triggering, reopening forms, and backup reviewer assignment.
 */
@RestController
@RequestMapping("/api/cycles")
public class CycleController {

    private static final Logger logger = LoggerFactory.getLogger(CycleController.class);

    private final CycleService cycleService;
    private final AppraisalTemplateRepository templateRepository;

    public CycleController(CycleService cycleService, AppraisalTemplateRepository templateRepository) {
        this.cycleService = cycleService;
        this.templateRepository = templateRepository;
    }

    /**
     * List all appraisal cycles.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<AppraisalCycle>>> listCycles() {
        logger.info("Fetching all appraisal cycles");
        List<AppraisalCycle> cycles = cycleService.findAll();
        return ResponseEntity.ok(ApiResponse.success(cycles));
    }

    /**
     * Get a cycle by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<AppraisalCycle>> getCycleById(@PathVariable Long id) {
        logger.info("Fetching appraisal cycle ID: {}", id);
        AppraisalCycle cycle = cycleService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(cycle));
    }

    /**
     * Create a new appraisal cycle.
     */
    @PostMapping
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<ApiResponse<AppraisalCycle>> createCycle(@RequestBody CreateCycleRequest request) {
        logger.info("Creating new appraisal cycle: {}", request.getName());
        
        // TODO: Extract currentUserId from SecurityContext when authentication is implemented
        Long currentUserId = 8L; // Placeholder
        
        AppraisalCycle cycle = new AppraisalCycle();
        cycle.setName(request.getName());
        cycle.setStartDate(request.getStartDate());
        cycle.setEndDate(request.getEndDate());
        
        // Fetch the actual template entity to avoid detached proxy issues
        if (request.getTemplateId() != null) {
            com.tns.appraisal.template.AppraisalTemplate template = templateRepository.findById(request.getTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Template not found with ID: " + request.getTemplateId()));
            cycle.setTemplate(template);
        }
        
        AppraisalCycle createdCycle = cycleService.create(cycle, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Cycle created successfully", createdCycle));
    }

    /**
     * Update an existing appraisal cycle.
     * 
     * @param id the cycle ID
     * @param request the cycle update request
     * @return the updated cycle
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<ApiResponse<AppraisalCycle>> updateCycle(
            @PathVariable Long id,
            @RequestBody UpdateCycleRequest request) {
        logger.info("Updating appraisal cycle ID: {}", id);
        
        // TODO: Extract currentUserId from SecurityContext when authentication is implemented
        Long currentUserId = 8L; // Placeholder
        
        AppraisalCycle cycle = new AppraisalCycle();
        cycle.setName(request.getName());
        cycle.setStartDate(request.getStartDate());
        cycle.setEndDate(request.getEndDate());
        
        if (request.getTemplateId() != null) {
            com.tns.appraisal.template.AppraisalTemplate template = templateRepository.findById(request.getTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Template not found with ID: " + request.getTemplateId()));
            cycle.setTemplate(template);
        }
        
        AppraisalCycle updatedCycle = cycleService.update(id, cycle, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Cycle updated successfully", updatedCycle));
    }

    /**
     * Trigger an appraisal cycle for selected employees.
     * Creates appraisal forms and sends notifications.
     * Implements bulk processing with partial failure resilience.
     * 
     * @param id the cycle ID
     * @param request the trigger request containing employee IDs
     * @return detailed summary of successes and failures
     */
    @PostMapping("/{id}/trigger")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<ApiResponse<TriggerCycleResult>> triggerCycle(
            @PathVariable Long id,
            @RequestBody TriggerCycleRequest request) {
        logger.info("Triggering appraisal cycle ID: {} for {} employees", id, request.getEmployeeIds().size());
        
        // TODO: Extract currentUserId from SecurityContext when authentication is implemented
        Long currentUserId = 8L; // Placeholder
        
        TriggerCycleResult result = cycleService.triggerCycle(id, request.getEmployeeIds(), currentUserId);
        
        String message = String.format(
                "Cycle triggered: %d successful, %d failed out of %d employees",
                result.getSuccessCount(),
                result.getFailureCount(),
                result.getTotalEmployees()
        );
        
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }

    /**
     * Reopen a submitted or completed appraisal form.
     * Resets the form status to allow re-submission.
     * 
     * @param id the cycle ID
     * @param formId the form ID to reopen
     * @return success response
     */
    @PostMapping("/{id}/reopen/{formId}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<ApiResponse<String>> reopenForm(
            @PathVariable Long id,
            @PathVariable Long formId) {
        logger.info("Reopening form ID: {} in cycle ID: {}", formId, id);
        
        // TODO: Extract currentUserId and roles from SecurityContext when authentication is implemented
        Long currentUserId = 8L; // Placeholder
        List<String> userRoles = List.of("HR"); // Placeholder
        
        cycleService.reopenForm(id, formId, currentUserId, userRoles);
        
        return ResponseEntity.ok(ApiResponse.success("Form reopened successfully", null));
    }

    /**
     * Assign a backup reviewer to an appraisal form.
     * 
     * @param id the cycle ID
     * @param request the backup reviewer assignment request
     * @return success response
     */
    @PutMapping("/{id}/backup-reviewer")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<ApiResponse<String>> assignBackupReviewer(
            @PathVariable Long id,
            @RequestBody AssignBackupReviewerRequest request) {
        logger.info("Assigning backup reviewer {} to form ID: {} in cycle ID: {}", 
                request.getBackupReviewerId(), request.getFormId(), id);
        
        // TODO: Extract currentUserId from SecurityContext when authentication is implemented
        Long currentUserId = 8L; // Placeholder
        
        cycleService.assignBackupReviewer(id, request.getFormId(), request.getBackupReviewerId(), currentUserId);
        
        return ResponseEntity.ok(ApiResponse.success("Backup reviewer assigned successfully", null));
    }

    // ==================== Request DTOs ====================

    /**
     * Request DTO for creating a new appraisal cycle.
     */
    public static class CreateCycleRequest {
        private String name;
        private LocalDate startDate;
        private LocalDate endDate;
        private Long templateId;

        public CreateCycleRequest() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }

        public Long getTemplateId() {
            return templateId;
        }

        public void setTemplateId(Long templateId) {
            this.templateId = templateId;
        }
    }

    /**
     * Request DTO for updating an appraisal cycle.
     */
    public static class UpdateCycleRequest {
        private String name;
        private LocalDate startDate;
        private LocalDate endDate;
        private Long templateId;

        public UpdateCycleRequest() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }

        public Long getTemplateId() {
            return templateId;
        }

        public void setTemplateId(Long templateId) {
            this.templateId = templateId;
        }
    }

    /**
     * Request DTO for triggering an appraisal cycle.
     */
    public static class TriggerCycleRequest {
        private List<Long> employeeIds;

        public TriggerCycleRequest() {
        }

        public List<Long> getEmployeeIds() {
            return employeeIds;
        }

        public void setEmployeeIds(List<Long> employeeIds) {
            this.employeeIds = employeeIds;
        }
    }

    /**
     * Request DTO for assigning a backup reviewer.
     */
    public static class AssignBackupReviewerRequest {
        private Long formId;
        private Long backupReviewerId;

        public AssignBackupReviewerRequest() {
        }

        public Long getFormId() {
            return formId;
        }

        public void setFormId(Long formId) {
            this.formId = formId;
        }

        public Long getBackupReviewerId() {
            return backupReviewerId;
        }

        public void setBackupReviewerId(Long backupReviewerId) {
            this.backupReviewerId = backupReviewerId;
        }
    }
}
