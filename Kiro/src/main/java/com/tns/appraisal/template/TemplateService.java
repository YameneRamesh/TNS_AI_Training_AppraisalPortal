package com.tns.appraisal.template;

import com.tns.appraisal.exception.BusinessException;
import com.tns.appraisal.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing appraisal templates including CRUD operations, versioning, and activation.
 */
@Service
public class TemplateService {

    private final AppraisalTemplateRepository templateRepository;
    private final TemplateSchemaValidator schemaValidator;

    public TemplateService(AppraisalTemplateRepository templateRepository,
                          TemplateSchemaValidator schemaValidator) {
        this.templateRepository = templateRepository;
        this.schemaValidator = schemaValidator;
    }

    /**
     * Create a new appraisal template.
     * 
     * @param template the template to create
     * @return the created template
     * @throws BusinessException if a template with the same version already exists
     */
    @Transactional
    public AppraisalTemplate createTemplate(AppraisalTemplate template) {
        // Validate schema JSON structure
        if (template.getSchemaJson() != null) {
            schemaValidator.validateSchema(template.getSchemaJson());
        }
        
        // Validate version uniqueness
        if (template.getVersion() != null && templateRepository.findByVersion(template.getVersion()).isPresent()) {
            throw new BusinessException("Template with version '" + template.getVersion() + "' already exists");
        }

        // Ensure new templates are not active by default
        if (template.getIsActive() == null) {
            template.setIsActive(false);
        }

        return templateRepository.save(template);
    }

    /**
     * Get a template by ID.
     * 
     * @param id the template ID
     * @return the template
     * @throws ResourceNotFoundException if template not found
     */
    @Transactional(readOnly = true)
    public AppraisalTemplate getTemplateById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AppraisalTemplate", id));
    }

    /**
     * Get a template by version.
     * 
     * @param version the template version
     * @return the template
     * @throws ResourceNotFoundException if template not found
     */
    @Transactional(readOnly = true)
    public AppraisalTemplate getTemplateByVersion(String version) {
        return templateRepository.findByVersion(version)
                .orElseThrow(() -> new ResourceNotFoundException("AppraisalTemplate", version));
    }

    /**
     * Get the currently active template.
     * 
     * @return the active template
     * @throws ResourceNotFoundException if no active template exists
     */
    @Transactional(readOnly = true)
    public AppraisalTemplate getActiveTemplate() {
        return templateRepository.findByIsActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No active AppraisalTemplate found"));
    }

    /**
     * Get all templates.
     * 
     * @return list of all templates
     */
    @Transactional(readOnly = true)
    public List<AppraisalTemplate> getAllTemplates() {
        return templateRepository.findAll();
    }

    /**
     * Update an existing template.
     * Only non-active templates can be updated to prevent breaking existing forms.
     * 
     * @param id the template ID
     * @param updatedTemplate the updated template data
     * @return the updated template
     * @throws ResourceNotFoundException if template not found
     * @throws BusinessException if attempting to update an active template or version conflict
     */
    @Transactional
    public AppraisalTemplate updateTemplate(Long id, AppraisalTemplate updatedTemplate) {
        AppraisalTemplate existingTemplate = getTemplateById(id);

        // Prevent updating active templates
        if (existingTemplate.getIsActive()) {
            throw new BusinessException("Cannot update an active template. Deactivate it first or create a new version.");
        }

        // Validate schema JSON structure if being updated
        if (updatedTemplate.getSchemaJson() != null) {
            schemaValidator.validateSchema(updatedTemplate.getSchemaJson());
        }

        // Check version uniqueness if version is being changed
        if (updatedTemplate.getVersion() != null && 
            !updatedTemplate.getVersion().equals(existingTemplate.getVersion())) {
            Optional<AppraisalTemplate> conflictingTemplate = templateRepository.findByVersion(updatedTemplate.getVersion());
            if (conflictingTemplate.isPresent() && !conflictingTemplate.get().getId().equals(id)) {
                throw new BusinessException("Template with version '" + updatedTemplate.getVersion() + "' already exists");
            }
        }

        // Update fields
        if (updatedTemplate.getVersion() != null) {
            existingTemplate.setVersion(updatedTemplate.getVersion());
        }
        if (updatedTemplate.getSchemaJson() != null) {
            existingTemplate.setSchemaJson(updatedTemplate.getSchemaJson());
        }
        if (updatedTemplate.getCreatedBy() != null) {
            existingTemplate.setCreatedBy(updatedTemplate.getCreatedBy());
        }

        return templateRepository.save(existingTemplate);
    }

    /**
     * Activate a template. Deactivates all other templates to ensure only one is active.
     * 
     * @param id the template ID to activate
     * @return the activated template
     * @throws ResourceNotFoundException if template not found
     */
    @Transactional
    public AppraisalTemplate activateTemplate(Long id) {
        AppraisalTemplate templateToActivate = getTemplateById(id);

        // Deactivate all currently active templates
        Optional<AppraisalTemplate> currentlyActive = templateRepository.findByIsActiveTrue();
        if (currentlyActive.isPresent() && !currentlyActive.get().getId().equals(id)) {
            AppraisalTemplate activeTemplate = currentlyActive.get();
            activeTemplate.setIsActive(false);
            templateRepository.save(activeTemplate);
        }

        // Activate the target template
        templateToActivate.setIsActive(true);
        return templateRepository.save(templateToActivate);
    }

    /**
     * Deactivate a template.
     * 
     * @param id the template ID to deactivate
     * @return the deactivated template
     * @throws ResourceNotFoundException if template not found
     */
    @Transactional
    public AppraisalTemplate deactivateTemplate(Long id) {
        AppraisalTemplate template = getTemplateById(id);
        template.setIsActive(false);
        return templateRepository.save(template);
    }

    /**
     * Delete a template.
     * Only non-active templates that are not referenced by any forms can be deleted.
     * 
     * @param id the template ID to delete
     * @throws ResourceNotFoundException if template not found
     * @throws BusinessException if attempting to delete an active template
     */
    @Transactional
    public void deleteTemplate(Long id) {
        AppraisalTemplate template = getTemplateById(id);

        // Prevent deleting active templates
        if (template.getIsActive()) {
            throw new BusinessException("Cannot delete an active template. Deactivate it first.");
        }

        // Note: In production, should also check if template is referenced by any appraisal forms
        // This check will be added when the AppraisalForm entity is implemented

        templateRepository.delete(template);
    }

    /**
     * Create a new version of an existing template.
     * Copies the schema from the source template and creates a new inactive template.
     * 
     * @param sourceId the ID of the template to copy from
     * @param newVersion the version string for the new template
     * @param createdBy the user ID creating the new version
     * @return the newly created template version
     * @throws ResourceNotFoundException if source template not found
     * @throws BusinessException if new version already exists
     */
    @Transactional
    public AppraisalTemplate createNewVersion(Long sourceId, String newVersion, Long createdBy) {
        AppraisalTemplate sourceTemplate = getTemplateById(sourceId);

        // Check if new version already exists
        if (templateRepository.findByVersion(newVersion).isPresent()) {
            throw new BusinessException("Template with version '" + newVersion + "' already exists");
        }

        String copiedSchema = sourceTemplate.getSchemaJson();
        if (copiedSchema != null) {
            schemaValidator.validateSchema(copiedSchema);
        }

        AppraisalTemplate newTemplate = new AppraisalTemplate();
        newTemplate.setVersion(newVersion);
        newTemplate.setSchemaJson(copiedSchema);
        newTemplate.setIsActive(false);
        newTemplate.setCreatedBy(createdBy);

        return templateRepository.save(newTemplate);
    }
}
