package com.tns.appraisal.template;

import com.tns.appraisal.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing appraisal templates.
 * Provides endpoints for CRUD operations, versioning, and activation.
 * Access restricted to HR and Admin roles.
 */
@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateService templateService;
    private final TemplateSchemaValidator schemaValidator;

    public TemplateController(TemplateService templateService, TemplateSchemaValidator schemaValidator) {
        this.templateService = templateService;
        this.schemaValidator = schemaValidator;
    }

    /**
     * Get all templates.
     * 
     * @return list of all templates
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<AppraisalTemplate>>> getAllTemplates() {
        List<AppraisalTemplate> templates = templateService.getAllTemplates();
        return ResponseEntity.ok(ApiResponse.success(templates));
    }

    /**
     * Get a template by ID.
     * 
     * @param id the template ID
     * @return the template
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<AppraisalTemplate>> getTemplateById(@PathVariable Long id) {
        AppraisalTemplate template = templateService.getTemplateById(id);
        return ResponseEntity.ok(ApiResponse.success(template));
    }

    /**
     * Get the currently active template.
     * 
     * @return the active template
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<AppraisalTemplate>> getActiveTemplate() {
        AppraisalTemplate template = templateService.getActiveTemplate();
        return ResponseEntity.ok(ApiResponse.success(template));
    }

    /**
     * Create a new template.
     * 
     * @param template the template to create
     * @return the created template
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<AppraisalTemplate>> createTemplate(@RequestBody AppraisalTemplate template) {
        AppraisalTemplate createdTemplate = templateService.createTemplate(template);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Template created successfully", createdTemplate));
    }

    /**
     * Update an existing template.
     * 
     * @param id the template ID
     * @param template the updated template data
     * @return the updated template
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<AppraisalTemplate>> updateTemplate(
            @PathVariable Long id,
            @RequestBody AppraisalTemplate template) {
        AppraisalTemplate updatedTemplate = templateService.updateTemplate(id, template);
        return ResponseEntity.ok(ApiResponse.success("Template updated successfully", updatedTemplate));
    }

    /**
     * Activate a template.
     * 
     * @param id the template ID to activate
     * @return the activated template
     */
    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<AppraisalTemplate>> activateTemplate(@PathVariable Long id) {
        AppraisalTemplate activatedTemplate = templateService.activateTemplate(id);
        return ResponseEntity.ok(ApiResponse.success("Template activated successfully", activatedTemplate));
    }

    /**
     * Deactivate a template.
     * 
     * @param id the template ID to deactivate
     * @return the deactivated template
     */
    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<AppraisalTemplate>> deactivateTemplate(@PathVariable Long id) {
        AppraisalTemplate deactivatedTemplate = templateService.deactivateTemplate(id);
        return ResponseEntity.ok(ApiResponse.success("Template deactivated successfully", deactivatedTemplate));
    }

    /**
     * Delete a template.
     * 
     * @param id the template ID to delete
     * @return success response
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.ok(ApiResponse.success("Template deleted successfully", null));
    }

    /**
     * Create a new version of an existing template.
     * 
     * @param id the source template ID
     * @param request the version creation request
     * @return the newly created template version
     */
    @PostMapping("/{id}/version")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<AppraisalTemplate>> createNewVersion(
            @PathVariable Long id,
            @RequestBody CreateVersionRequest request) {
        AppraisalTemplate newVersion = templateService.createNewVersion(
                id, 
                request.getNewVersion(), 
                request.getCreatedBy()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("New template version created successfully", newVersion));
    }

    /**
     * Validate a template schema JSON without creating a template.
     * Useful for pre-validation before template creation.
     * 
     * @param request the validation request containing schema JSON
     * @return validation result with any errors found
     */
    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<ValidationResult>> validateSchema(@RequestBody ValidateSchemaRequest request) {
        List<String> errors = schemaValidator.getValidationErrors(request.getSchemaJson());
        
        ValidationResult result = new ValidationResult();
        result.setValid(errors.isEmpty());
        result.setErrors(errors);
        
        if (result.isValid()) {
            return ResponseEntity.ok(ApiResponse.success("Schema is valid", result));
        } else {
            return ResponseEntity.ok(ApiResponse.success("Schema validation completed with errors", result));
        }
    }

    /**
     * Request DTO for creating a new template version.
     */
    public static class CreateVersionRequest {
        private String newVersion;
        private Long createdBy;

        public CreateVersionRequest() {
        }

        public String getNewVersion() {
            return newVersion;
        }

        public void setNewVersion(String newVersion) {
            this.newVersion = newVersion;
        }

        public Long getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(Long createdBy) {
            this.createdBy = createdBy;
        }
    }

    /**
     * Request DTO for validating a template schema.
     */
    public static class ValidateSchemaRequest {
        private String schemaJson;

        public ValidateSchemaRequest() {
        }

        public String getSchemaJson() {
            return schemaJson;
        }

        public void setSchemaJson(String schemaJson) {
            this.schemaJson = schemaJson;
        }
    }

    /**
     * Response DTO for schema validation results.
     */
    public static class ValidationResult {
        private boolean valid;
        private List<String> errors;

        public ValidationResult() {
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public void setErrors(List<String> errors) {
            this.errors = errors;
        }
    }
}
