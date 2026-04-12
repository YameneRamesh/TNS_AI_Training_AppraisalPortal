package com.tns.appraisal.cycle;

import com.tns.appraisal.common.dto.ApiResponse;
import com.tns.appraisal.exception.ResourceNotFoundException;
import com.tns.appraisal.form.AppraisalFormDto;
import com.tns.appraisal.template.AppraisalTemplateRepository;
import com.tns.appraisal.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

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

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<AppraisalCycle>>> listCycles() {
        logger.info("Fetching all appraisal cycles");
        List<AppraisalCycle> cycles = cycleService.findAll();
        return ResponseEntity.ok(ApiResponse.success(cycles));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<AppraisalCycle>> getCycleById(@PathVariable Long id) {
        logger.info("Fetching appraisal cycle ID: {}", id);
        AppraisalCycle cycle = cycleService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(cycle));
    }

    @GetMapping("/{id}/forms")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<AppraisalFormDto>>> getCycleForms(@PathVariable Long id) {
        logger.info("Fetching forms for cycle ID: {}", id);
        List<AppraisalFormDto> forms = cycleService.getCycleForms(id);
        return ResponseEntity.ok(ApiResponse.success(forms));
    }

    @PostMapping
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<ApiResponse<AppraisalCycle>> createCycle(
            @RequestBody CreateCycleRequest request,
            Authentication authentication) {
        logger.info("Creating new appraisal cycle: {}", request.getName());

        Long currentUserId = extractUserId(authentication);

        AppraisalCycle cycle = new AppraisalCycle();
        cycle.setName(request.getName());
        cycle.setStartDate(request.getStartDate());
        cycle.setEndDate(request.getEndDate());

        if (request.getTemplateId() != null) {
            com.tns.appraisal.template.AppraisalTemplate template = templateRepository.findById(request.getTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Template not found with ID: " + request.getTemplateId()));
            cycle.setTemplate(template);
        }

        AppraisalCycle createdCycle = cycleService.create(cycle, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Cycle created successfully", createdCycle));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<ApiResponse<AppraisalCycle>> updateCycle(
            @PathVariable Long id,
            @RequestBody UpdateCycleRequest request,
            Authentication authentication) {
        logger.info("Updating appraisal cycle ID: {}", id);

        Long currentUserId = extractUserId(authentication);

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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<ApiResponse<String>> deleteCycle(
            @PathVariable Long id,
            Authentication authentication) {
        logger.info("Deleting appraisal cycle ID: {}", id);

        Long currentUserId = extractUserId(authentication);
        cycleService.delete(id, currentUserId);

        return ResponseEntity.ok(ApiResponse.success("Cycle deleted successfully", null));
    }

    @PostMapping("/{id}/trigger")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<ApiResponse<TriggerCycleResult>> triggerCycle(
            @PathVariable Long id,
            @RequestBody TriggerCycleRequest request,
            Authentication authentication) {
        logger.info("Triggering appraisal cycle ID: {} for {} employees",
                id, request.getEmployeeIds() != null ? request.getEmployeeIds().size() : 0);

        Long currentUserId = extractUserId(authentication);

        TriggerCycleResult result = cycleService.triggerCycle(id, request.getEmployeeIds(), currentUserId);

        String message = String.format(
                "Cycle triggered: %d successful, %d failed out of %d employees",
                result.getSuccessCount(),
                result.getFailureCount(),
                result.getTotalEmployees()
        );

        return ResponseEntity.ok(ApiResponse.success(message, result));
    }

    @PostMapping("/{id}/reopen/{formId}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<ApiResponse<String>> reopenForm(
            @PathVariable Long id,
            @PathVariable Long formId,
            Authentication authentication) {
        logger.info("Reopening form ID: {} in cycle ID: {}", formId, id);

        Long currentUserId = extractUserId(authentication);
        List<String> userRoles = extractRoles(authentication);

        cycleService.reopenForm(id, formId, currentUserId, userRoles);

        return ResponseEntity.ok(ApiResponse.success("Form reopened successfully", null));
    }

    @PutMapping("/{id}/backup-reviewer")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<ApiResponse<String>> assignBackupReviewer(
            @PathVariable Long id,
            @RequestBody AssignBackupReviewerRequest request,
            Authentication authentication) {
        logger.info("Assigning backup reviewer {} to form ID: {} in cycle ID: {}",
                request.getBackupReviewerId(), request.getFormId(), id);

        Long currentUserId = extractUserId(authentication);

        cycleService.assignBackupReviewer(id, request.getFormId(), request.getBackupReviewerId(), currentUserId);

        return ResponseEntity.ok(ApiResponse.success("Backup reviewer assigned successfully", null));
    }

    /**
     * Extract user ID from the Authentication principal.
     * The SessionAuthFilter sets either a Long (cached) or User (first request) as principal.
     */
    private Long extractUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        if (principal instanceof User user) {
            return user.getId();
        }
        throw new IllegalStateException("Unexpected principal type: " + principal.getClass().getName());
    }

    private List<String> extractRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(auth -> auth.startsWith("ROLE_") ? auth.substring(5) : auth)
                .collect(Collectors.toList());
    }

    // ==================== Request DTOs ====================

    public static class CreateCycleRequest {
        private String name;
        private LocalDate startDate;
        private LocalDate endDate;
        private Long templateId;

        public CreateCycleRequest() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public Long getTemplateId() { return templateId; }
        public void setTemplateId(Long templateId) { this.templateId = templateId; }
    }

    public static class UpdateCycleRequest {
        private String name;
        private LocalDate startDate;
        private LocalDate endDate;
        private Long templateId;

        public UpdateCycleRequest() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public Long getTemplateId() { return templateId; }
        public void setTemplateId(Long templateId) { this.templateId = templateId; }
    }

    public static class TriggerCycleRequest {
        private List<Long> employeeIds;

        public TriggerCycleRequest() {}

        public List<Long> getEmployeeIds() { return employeeIds; }
        public void setEmployeeIds(List<Long> employeeIds) { this.employeeIds = employeeIds; }
    }

    public static class AssignBackupReviewerRequest {
        private Long formId;
        private Long backupReviewerId;

        public AssignBackupReviewerRequest() {}

        public Long getFormId() { return formId; }
        public void setFormId(Long formId) { this.formId = formId; }
        public Long getBackupReviewerId() { return backupReviewerId; }
        public void setBackupReviewerId(Long backupReviewerId) { this.backupReviewerId = backupReviewerId; }
    }
}
