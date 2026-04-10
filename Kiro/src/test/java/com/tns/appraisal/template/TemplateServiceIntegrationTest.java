package com.tns.appraisal.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tns.appraisal.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for TemplateService with schema validation.
 */
@ExtendWith(MockitoExtension.class)
class TemplateServiceIntegrationTest {

    @Mock
    private AppraisalTemplateRepository templateRepository;

    private TemplateSchemaValidator schemaValidator;
    private TemplateService templateService;

    @BeforeEach
    void setUp() {
        schemaValidator = new TemplateSchemaValidator(new ObjectMapper());
        templateService = new TemplateService(templateRepository, schemaValidator);
    }

    @Test
    void createTemplate_withValidSchema_shouldSucceed() {
        String validSchema = """
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

        AppraisalTemplate template = new AppraisalTemplate();
        template.setVersion("3.0");
        template.setSchemaJson(validSchema);
        template.setCreatedBy(1L);

        when(templateRepository.findByVersion("3.0")).thenReturn(Optional.empty());
        when(templateRepository.save(any(AppraisalTemplate.class))).thenReturn(template);

        AppraisalTemplate result = templateService.createTemplate(template);

        assertNotNull(result);
        verify(templateRepository).save(any(AppraisalTemplate.class));
    }

    @Test
    void createTemplate_withInvalidSchema_shouldThrowException() {
        String invalidSchema = """
            {
              "version": "3.0",
              "sections": [
                {
                  "sectionType": "invalid_type",
                  "title": "Invalid Section"
                }
              ]
            }
            """;

        AppraisalTemplate template = new AppraisalTemplate();
        template.setVersion("3.0");
        template.setSchemaJson(invalidSchema);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> templateService.createTemplate(template)
        );

        assertTrue(exception.getMessage().contains("Invalid section type"));
        verify(templateRepository, never()).save(any(AppraisalTemplate.class));
    }

    @Test
    void createTemplate_withMissingVersion_shouldThrowException() {
        String schemaWithoutVersion = """
            {
              "sections": [
                {
                  "sectionType": "key_responsibilities",
                  "title": "Key Responsibilities"
                }
              ]
            }
            """;

        AppraisalTemplate template = new AppraisalTemplate();
        template.setVersion("3.0");
        template.setSchemaJson(schemaWithoutVersion);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> templateService.createTemplate(template)
        );

        assertEquals("Template schema must have a 'version' field", exception.getMessage());
        verify(templateRepository, never()).save(any(AppraisalTemplate.class));
    }

    @Test
    void updateTemplate_withInvalidSchema_shouldThrowException() {
        String invalidSchema = """
            {
              "version": "3.0",
              "sections": []
            }
            """;

        AppraisalTemplate existingTemplate = new AppraisalTemplate();
        existingTemplate.setId(1L);
        existingTemplate.setVersion("3.0");
        existingTemplate.setIsActive(false);

        AppraisalTemplate updatedTemplate = new AppraisalTemplate();
        updatedTemplate.setSchemaJson(invalidSchema);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(existingTemplate));

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> templateService.updateTemplate(1L, updatedTemplate)
        );

        assertEquals("Template must have at least one section", exception.getMessage());
        verify(templateRepository, never()).save(any(AppraisalTemplate.class));
    }

    @Test
    void createTemplate_withDuplicateItemIds_shouldThrowException() {
        String schemaWithDuplicateIds = """
            {
              "version": "3.0",
              "sections": [
                {
                  "sectionType": "key_responsibilities",
                  "title": "Key Responsibilities",
                  "items": [
                    {
                      "id": "kr_1",
                      "label": "Duty 1",
                      "ratingScale": "competency"
                    },
                    {
                      "id": "kr_1",
                      "label": "Duty 2",
                      "ratingScale": "competency"
                    }
                  ]
                }
              ]
            }
            """;

        AppraisalTemplate template = new AppraisalTemplate();
        template.setVersion("3.0");
        template.setSchemaJson(schemaWithDuplicateIds);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> templateService.createTemplate(template)
        );

        assertTrue(exception.getMessage().contains("Duplicate item id"));
        verify(templateRepository, never()).save(any(AppraisalTemplate.class));
    }

    @Test
    void createTemplate_withInvalidRatingScale_shouldThrowException() {
        String schemaWithInvalidRatingScale = """
            {
              "version": "3.0",
              "sections": [
                {
                  "sectionType": "key_responsibilities",
                  "title": "Key Responsibilities",
                  "items": [
                    {
                      "id": "kr_1",
                      "label": "Duty 1",
                      "ratingScale": "invalid_scale"
                    }
                  ]
                }
              ]
            }
            """;

        AppraisalTemplate template = new AppraisalTemplate();
        template.setVersion("3.0");
        template.setSchemaJson(schemaWithInvalidRatingScale);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> templateService.createTemplate(template)
        );

        assertTrue(exception.getMessage().contains("Invalid rating scale"));
        verify(templateRepository, never()).save(any(AppraisalTemplate.class));
    }

    @Test
    void createTemplate_withComplexValidSchema_shouldSucceed() {
        String complexValidSchema = """
            {
              "version": "3.0",
              "sections": [
                {
                  "sectionType": "header",
                  "title": "Header"
                },
                {
                  "sectionType": "key_responsibilities",
                  "title": "Key Responsibilities",
                  "items": [
                    {
                      "id": "kr_1",
                      "label": "Essential Duty 1",
                      "ratingScale": "competency"
                    },
                    {
                      "id": "kr_2",
                      "label": "Essential Duty 2",
                      "ratingScale": "competency"
                    }
                  ]
                },
                {
                  "sectionType": "idp",
                  "title": "Individual Development Plan",
                  "items": [
                    {
                      "id": "idp_nextgen",
                      "label": "NextGen Tech Skills",
                      "ratingScale": "competency"
                    }
                  ]
                },
                {
                  "sectionType": "policy_adherence",
                  "title": "Policy Adherence",
                  "items": [
                    {
                      "id": "policy_hr",
                      "label": "Follow HR Policy",
                      "ratingScale": "policy_1_10"
                    }
                  ]
                }
              ]
            }
            """;

        AppraisalTemplate template = new AppraisalTemplate();
        template.setVersion("3.0");
        template.setSchemaJson(complexValidSchema);
        template.setCreatedBy(1L);

        when(templateRepository.findByVersion("3.0")).thenReturn(Optional.empty());
        when(templateRepository.save(any(AppraisalTemplate.class))).thenReturn(template);

        AppraisalTemplate result = templateService.createTemplate(template);

        assertNotNull(result);
        verify(templateRepository).save(any(AppraisalTemplate.class));
    }
}
