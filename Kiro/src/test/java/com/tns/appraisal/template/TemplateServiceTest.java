package com.tns.appraisal.template;

import com.tns.appraisal.exception.BusinessException;
import com.tns.appraisal.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TemplateService.
 * Tests CRUD operations, versioning, activation/deactivation, and validation logic.
 */
@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    @Mock
    private AppraisalTemplateRepository templateRepository;

    @Mock
    private TemplateSchemaValidator schemaValidator;

    private TemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new TemplateService(templateRepository, schemaValidator);
    }

    // ========== CREATE TEMPLATE TESTS ==========

    @Test
    void createTemplate_withValidData_shouldSucceed() {
        // Arrange
        AppraisalTemplate template = createTemplate(null, "3.0", "{}", false);
        AppraisalTemplate savedTemplate = createTemplate(1L, "3.0", "{}", false);

        when(templateRepository.findByVersion("3.0")).thenReturn(Optional.empty());
        when(templateRepository.save(any(AppraisalTemplate.class))).thenReturn(savedTemplate);

        // Act
        AppraisalTemplate result = templateService.createTemplate(template);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("3.0", result.getVersion());
        assertFalse(result.getIsActive());
        verify(schemaValidator).validateSchema("{}");
        verify(templateRepository).findByVersion("3.0");
        verify(templateRepository).save(any(AppraisalTemplate.class));
    }

    @Test
    void createTemplate_withNullIsActive_shouldDefaultToFalse() {
        // Arrange
        AppraisalTemplate template = createTemplate(null, "3.0", "{}", null);
        AppraisalTemplate savedTemplate = createTemplate(1L, "3.0", "{}", false);

        when(templateRepository.findByVersion("3.0")).thenReturn(Optional.empty());
        when(templateRepository.save(any(AppraisalTemplate.class))).thenAnswer(invocation -> {
            AppraisalTemplate arg = invocation.getArgument(0);
            assertFalse(arg.getIsActive(), "IsActive should be set to false by default");
            return savedTemplate;
        });

        // Act
        AppraisalTemplate result = templateService.createTemplate(template);

        // Assert
        assertNotNull(result);
        assertFalse(result.getIsActive());
        verify(templateRepository).save(any(AppraisalTemplate.class));
    }

    @Test
    void createTemplate_withDuplicateVersion_shouldThrowBusinessException() {
        // Arrange
        AppraisalTemplate existingTemplate = createTemplate(1L, "3.0", "{}", false);
        AppraisalTemplate newTemplate = createTemplate(null, "3.0", "{}", false);

        when(templateRepository.findByVersion("3.0")).thenReturn(Optional.of(existingTemplate));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> templateService.createTemplate(newTemplate));
        assertEquals("Template with version '3.0' already exists", exception.getMessage());
        verify(templateRepository, never()).save(any());
    }

    @Test
    void createTemplate_withInvalidSchema_shouldThrowBusinessException() {
        // Arrange
        AppraisalTemplate template = createTemplate(null, "3.0", "invalid json", false);

        doThrow(new BusinessException("Invalid JSON schema"))
                .when(schemaValidator).validateSchema("invalid json");

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> templateService.createTemplate(template));
        assertEquals("Invalid JSON schema", exception.getMessage());
        verify(templateRepository, never()).save(any());
    }

    @Test
    void createTemplate_withNullSchemaJson_shouldNotValidate() {
        // Arrange
        AppraisalTemplate template = createTemplate(null, "3.0", null, false);
        AppraisalTemplate savedTemplate = createTemplate(1L, "3.0", null, false);

        when(templateRepository.findByVersion("3.0")).thenReturn(Optional.empty());
        when(templateRepository.save(any(AppraisalTemplate.class))).thenReturn(savedTemplate);

        // Act
        AppraisalTemplate result = templateService.createTemplate(template);

        // Assert
        assertNotNull(result);
        verify(schemaValidator, never()).validateSchema(any());
        verify(templateRepository).save(any(AppraisalTemplate.class));
    }

    // ========== GET TEMPLATE TESTS ==========

    @Test
    void getTemplateById_withExistingId_shouldReturnTemplate() {
        // Arrange
        AppraisalTemplate template = createTemplate(1L, "3.0", "{}", false);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));

        // Act
        AppraisalTemplate result = templateService.getTemplateById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("3.0", result.getVersion());
        verify(templateRepository).findById(1L);
    }

    @Test
    void getTemplateById_withNonExistentId_shouldThrowResourceNotFoundException() {
        // Arrange
        when(templateRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> templateService.getTemplateById(999L));
        assertTrue(exception.getMessage().contains("AppraisalTemplate"));
        assertTrue(exception.getMessage().contains("999"));
        verify(templateRepository).findById(999L);
    }

    @Test
    void getTemplateByVersion_withExistingVersion_shouldReturnTemplate() {
        // Arrange
        AppraisalTemplate template = createTemplate(1L, "3.0", "{}", false);

        when(templateRepository.findByVersion("3.0")).thenReturn(Optional.of(template));

        // Act
        AppraisalTemplate result = templateService.getTemplateByVersion("3.0");

        // Assert
        assertNotNull(result);
        assertEquals("3.0", result.getVersion());
        verify(templateRepository).findByVersion("3.0");
    }

    @Test
    void getTemplateByVersion_withNonExistentVersion_shouldThrowResourceNotFoundException() {
        // Arrange
        when(templateRepository.findByVersion("99.0")).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> templateService.getTemplateByVersion("99.0"));
        assertTrue(exception.getMessage().contains("AppraisalTemplate"));
        assertTrue(exception.getMessage().contains("99.0"));
        verify(templateRepository).findByVersion("99.0");
    }

    @Test
    void getActiveTemplate_withActiveTemplate_shouldReturnTemplate() {
        // Arrange
        AppraisalTemplate activeTemplate = createTemplate(1L, "3.0", "{}", true);

        when(templateRepository.findByIsActiveTrue()).thenReturn(Optional.of(activeTemplate));

        // Act
        AppraisalTemplate result = templateService.getActiveTemplate();

        // Assert
        assertNotNull(result);
        assertTrue(result.getIsActive());
        assertEquals("3.0", result.getVersion());
        verify(templateRepository).findByIsActiveTrue();
    }

    @Test
    void getActiveTemplate_withNoActiveTemplate_shouldThrowResourceNotFoundException() {
        // Arrange
        when(templateRepository.findByIsActiveTrue()).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> templateService.getActiveTemplate());
        assertEquals("No active AppraisalTemplate found", exception.getMessage());
        verify(templateRepository).findByIsActiveTrue();
    }

    @Test
    void getAllTemplates_shouldReturnAllTemplates() {
        // Arrange
        List<AppraisalTemplate> templates = List.of(
                createTemplate(1L, "3.0", "{}", true),
                createTemplate(2L, "2.0", "{}", false)
        );

        when(templateRepository.findAll()).thenReturn(templates);

        // Act
        List<AppraisalTemplate> result = templateService.getAllTemplates();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(templateRepository).findAll();
    }

    @Test
    void getAllTemplates_withNoTemplates_shouldReturnEmptyList() {
        // Arrange
        when(templateRepository.findAll()).thenReturn(List.of());

        // Act
        List<AppraisalTemplate> result = templateService.getAllTemplates();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(templateRepository).findAll();
    }

    // ========== UPDATE TEMPLATE TESTS ==========

    @Test
    void updateTemplate_withValidData_shouldSucceed() {
        // Arrange
        AppraisalTemplate existingTemplate = createTemplate(1L, "3.0", "{}", false);
        AppraisalTemplate updatedTemplate = createTemplate(null, "3.1", "{\"new\":\"schema\"}", false);
        updatedTemplate.setCreatedBy(100L);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(existingTemplate));
        when(templateRepository.findByVersion("3.1")).thenReturn(Optional.empty());
        when(templateRepository.save(any(AppraisalTemplate.class))).thenReturn(existingTemplate);

        // Act
        AppraisalTemplate result = templateService.updateTemplate(1L, updatedTemplate);

        // Assert
        assertNotNull(result);
        assertEquals("3.1", result.getVersion());
        assertEquals("{\"new\":\"schema\"}", result.getSchemaJson());
        assertEquals(100L, result.getCreatedBy());
        verify(schemaValidator).validateSchema("{\"new\":\"schema\"}");
        verify(templateRepository).save(existingTemplate);
    }

    @Test
    void updateTemplate_withActiveTemplate_shouldThrowBusinessException() {
        // Arrange
        AppraisalTemplate activeTemplate = createTemplate(1L, "3.0", "{}", true);
        AppraisalTemplate updatedTemplate = createTemplate(null, "3.1", "{}", false);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(activeTemplate));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> templateService.updateTemplate(1L, updatedTemplate));
        assertEquals("Cannot update an active template. Deactivate it first or create a new version.",
                exception.getMessage());
        verify(templateRepository, never()).save(any());
    }

    @Test
    void updateTemplate_withDuplicateVersion_shouldThrowBusinessException() {
        // Arrange
        AppraisalTemplate existingTemplate = createTemplate(1L, "3.0", "{}", false);
        AppraisalTemplate conflictingTemplate = createTemplate(2L, "3.1", "{}", false);
        AppraisalTemplate updatedTemplate = createTemplate(null, "3.1", "{}", false);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(existingTemplate));
        when(templateRepository.findByVersion("3.1")).thenReturn(Optional.of(conflictingTemplate));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> templateService.updateTemplate(1L, updatedTemplate));
        assertEquals("Template with version '3.1' already exists", exception.getMessage());
        verify(templateRepository, never()).save(any());
    }

    @Test
    void updateTemplate_withSameVersion_shouldSucceed() {
        // Arrange
        AppraisalTemplate existingTemplate = createTemplate(1L, "3.0", "{}", false);
        AppraisalTemplate updatedTemplate = createTemplate(null, "3.0", "{\"updated\":\"schema\"}", false);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(existingTemplate));
        // Note: findByVersion is called but returns the same template, so no conflict
        when(templateRepository.save(any(AppraisalTemplate.class))).thenReturn(existingTemplate);

        // Act
        AppraisalTemplate result = templateService.updateTemplate(1L, updatedTemplate);

        // Assert
        assertNotNull(result);
        assertEquals("{\"updated\":\"schema\"}", result.getSchemaJson());
        verify(templateRepository).save(existingTemplate);
    }

    @Test
    void updateTemplate_withInvalidSchema_shouldThrowBusinessException() {
        // Arrange
        AppraisalTemplate existingTemplate = createTemplate(1L, "3.0", "{}", false);
        AppraisalTemplate updatedTemplate = createTemplate(null, "3.0", "invalid", false);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(existingTemplate));
        doThrow(new BusinessException("Invalid schema"))
                .when(schemaValidator).validateSchema("invalid");

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> templateService.updateTemplate(1L, updatedTemplate));
        assertEquals("Invalid schema", exception.getMessage());
        verify(templateRepository, never()).save(any());
    }

    @Test
    void updateTemplate_withNonExistentId_shouldThrowResourceNotFoundException() {
        // Arrange
        AppraisalTemplate updatedTemplate = createTemplate(null, "3.1", "{}", false);

        when(templateRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> templateService.updateTemplate(999L, updatedTemplate));
        verify(templateRepository, never()).save(any());
    }

    @Test
    void updateTemplate_withNullFields_shouldNotUpdateThoseFields() {
        // Arrange
        AppraisalTemplate existingTemplate = createTemplate(1L, "3.0", "{\"old\":\"schema\"}", false);
        existingTemplate.setCreatedBy(50L);
        AppraisalTemplate updatedTemplate = new AppraisalTemplate();
        // All fields are null

        when(templateRepository.findById(1L)).thenReturn(Optional.of(existingTemplate));
        when(templateRepository.save(any(AppraisalTemplate.class))).thenReturn(existingTemplate);

        // Act
        AppraisalTemplate result = templateService.updateTemplate(1L, updatedTemplate);

        // Assert
        assertNotNull(result);
        assertEquals("3.0", result.getVersion()); // Should remain unchanged
        assertEquals("{\"old\":\"schema\"}", result.getSchemaJson()); // Should remain unchanged
        assertEquals(50L, result.getCreatedBy()); // Should remain unchanged
        verify(schemaValidator, never()).validateSchema(any());
        verify(templateRepository).save(existingTemplate);
    }

    // ========== ACTIVATE/DEACTIVATE TEMPLATE TESTS ==========

    @Test
    void activateTemplate_withNoCurrentlyActiveTemplate_shouldSucceed() {
        // Arrange
        AppraisalTemplate template = createTemplate(1L, "3.0", "{}", false);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(templateRepository.findByIsActiveTrue()).thenReturn(Optional.empty());
        when(templateRepository.save(any(AppraisalTemplate.class))).thenReturn(template);

        // Act
        AppraisalTemplate result = templateService.activateTemplate(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.getIsActive());
        verify(templateRepository).save(template);
    }

    @Test
    void activateTemplate_withDifferentActiveTemplate_shouldDeactivateOldAndActivateNew() {
        // Arrange
        AppraisalTemplate currentlyActive = createTemplate(2L, "2.0", "{}", true);
        AppraisalTemplate toActivate = createTemplate(1L, "3.0", "{}", false);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(toActivate));
        when(templateRepository.findByIsActiveTrue()).thenReturn(Optional.of(currentlyActive));
        when(templateRepository.save(any(AppraisalTemplate.class))).thenReturn(toActivate);

        // Act
        AppraisalTemplate result = templateService.activateTemplate(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.getIsActive());
        assertFalse(currentlyActive.getIsActive());
        verify(templateRepository, times(2)).save(any(AppraisalTemplate.class));
    }

    @Test
    void activateTemplate_withSameTemplateAlreadyActive_shouldSucceed() {
        // Arrange
        AppraisalTemplate template = createTemplate(1L, "3.0", "{}", true);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(templateRepository.findByIsActiveTrue()).thenReturn(Optional.of(template));
        when(templateRepository.save(any(AppraisalTemplate.class))).thenReturn(template);

        // Act
        AppraisalTemplate result = templateService.activateTemplate(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.getIsActive());
        verify(templateRepository, times(1)).save(template); // Only saves once
    }

    @Test
    void activateTemplate_withNonExistentId_shouldThrowResourceNotFoundException() {
        // Arrange
        when(templateRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> templateService.activateTemplate(999L));
        verify(templateRepository, never()).save(any());
    }

    @Test
    void deactivateTemplate_withActiveTemplate_shouldSucceed() {
        // Arrange
        AppraisalTemplate template = createTemplate(1L, "3.0", "{}", true);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(templateRepository.save(any(AppraisalTemplate.class))).thenReturn(template);

        // Act
        AppraisalTemplate result = templateService.deactivateTemplate(1L);

        // Assert
        assertNotNull(result);
        assertFalse(result.getIsActive());
        verify(templateRepository).save(template);
    }

    @Test
    void deactivateTemplate_withInactiveTemplate_shouldSucceed() {
        // Arrange
        AppraisalTemplate template = createTemplate(1L, "3.0", "{}", false);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(templateRepository.save(any(AppraisalTemplate.class))).thenReturn(template);

        // Act
        AppraisalTemplate result = templateService.deactivateTemplate(1L);

        // Assert
        assertNotNull(result);
        assertFalse(result.getIsActive());
        verify(templateRepository).save(template);
    }

    @Test
    void deactivateTemplate_withNonExistentId_shouldThrowResourceNotFoundException() {
        // Arrange
        when(templateRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> templateService.deactivateTemplate(999L));
        verify(templateRepository, never()).save(any());
    }

    // ========== DELETE TEMPLATE TESTS ==========

    @Test
    void deleteTemplate_withInactiveTemplate_shouldSucceed() {
        // Arrange
        AppraisalTemplate template = createTemplate(1L, "3.0", "{}", false);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));

        // Act
        templateService.deleteTemplate(1L);

        // Assert
        verify(templateRepository).delete(template);
    }

    @Test
    void deleteTemplate_withActiveTemplate_shouldThrowBusinessException() {
        // Arrange
        AppraisalTemplate activeTemplate = createTemplate(1L, "3.0", "{}", true);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(activeTemplate));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> templateService.deleteTemplate(1L));
        assertEquals("Cannot delete an active template. Deactivate it first.", exception.getMessage());
        verify(templateRepository, never()).delete(any());
    }

    @Test
    void deleteTemplate_withNonExistentId_shouldThrowResourceNotFoundException() {
        // Arrange
        when(templateRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> templateService.deleteTemplate(999L));
        verify(templateRepository, never()).delete(any());
    }

    // ========== CREATE NEW VERSION TESTS ==========

    @Test
    void createNewVersion_withValidData_shouldSucceed() {
        // Arrange
        AppraisalTemplate sourceTemplate = createTemplate(1L, "3.0", "{\"source\":\"schema\"}", true);
        AppraisalTemplate newTemplate = createTemplate(2L, "3.1", "{\"source\":\"schema\"}", false);
        newTemplate.setCreatedBy(100L);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(sourceTemplate));
        when(templateRepository.findByVersion("3.1")).thenReturn(Optional.empty());
        when(templateRepository.save(any(AppraisalTemplate.class))).thenReturn(newTemplate);

        // Act
        AppraisalTemplate result = templateService.createNewVersion(1L, "3.1", 100L);

        // Assert
        assertNotNull(result);
        assertEquals("3.1", result.getVersion());
        assertEquals("{\"source\":\"schema\"}", result.getSchemaJson());
        assertFalse(result.getIsActive());
        assertEquals(100L, result.getCreatedBy());
        verify(templateRepository).save(any(AppraisalTemplate.class));
    }

    @Test
    void createNewVersion_withDuplicateVersion_shouldThrowBusinessException() {
        // Arrange
        AppraisalTemplate sourceTemplate = createTemplate(1L, "3.0", "{}", true);
        AppraisalTemplate existingTemplate = createTemplate(2L, "3.1", "{}", false);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(sourceTemplate));
        when(templateRepository.findByVersion("3.1")).thenReturn(Optional.of(existingTemplate));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> templateService.createNewVersion(1L, "3.1", 100L));
        assertEquals("Template with version '3.1' already exists", exception.getMessage());
        verify(templateRepository, never()).save(any());
    }

    @Test
    void createNewVersion_withNonExistentSourceId_shouldThrowResourceNotFoundException() {
        // Arrange
        when(templateRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> templateService.createNewVersion(999L, "3.1", 100L));
        verify(templateRepository, never()).save(any());
    }

    @Test
    void createNewVersion_shouldCopySchemaFromSource() {
        // Arrange
        String complexSchema = "{\"version\":\"3.0\",\"sections\":[{\"sectionType\":\"key_responsibilities\"}]}";
        AppraisalTemplate sourceTemplate = createTemplate(1L, "3.0", complexSchema, true);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(sourceTemplate));
        when(templateRepository.findByVersion("3.1")).thenReturn(Optional.empty());
        when(templateRepository.save(any(AppraisalTemplate.class))).thenAnswer(invocation -> {
            AppraisalTemplate saved = invocation.getArgument(0);
            assertEquals(complexSchema, saved.getSchemaJson(), "Schema should be copied from source");
            return saved;
        });

        // Act
        templateService.createNewVersion(1L, "3.1", 100L);

        // Assert
        verify(templateRepository).save(any(AppraisalTemplate.class));
    }

    @Test
    void createNewVersion_shouldCreateInactiveTemplate() {
        // Arrange
        AppraisalTemplate sourceTemplate = createTemplate(1L, "3.0", "{}", true);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(sourceTemplate));
        when(templateRepository.findByVersion("3.1")).thenReturn(Optional.empty());
        when(templateRepository.save(any(AppraisalTemplate.class))).thenAnswer(invocation -> {
            AppraisalTemplate saved = invocation.getArgument(0);
            assertFalse(saved.getIsActive(), "New version should be inactive");
            return saved;
        });

        // Act
        templateService.createNewVersion(1L, "3.1", 100L);

        // Assert
        verify(templateRepository).save(any(AppraisalTemplate.class));
    }

    // ========== HELPER METHODS ==========

    private AppraisalTemplate createTemplate(Long id, String version, String schemaJson, Boolean isActive) {
        AppraisalTemplate template = new AppraisalTemplate();
        template.setId(id);
        template.setVersion(version);
        template.setSchemaJson(schemaJson);
        template.setIsActive(isActive);
        return template;
    }
}
