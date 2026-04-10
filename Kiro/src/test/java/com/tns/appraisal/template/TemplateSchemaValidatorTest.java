package com.tns.appraisal.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tns.appraisal.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TemplateSchemaValidator.
 */
class TemplateSchemaValidatorTest {

    private TemplateSchemaValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TemplateSchemaValidator(new ObjectMapper());
    }

    @Test
    void validateSchema_withValidSchema_shouldPass() {
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

        assertDoesNotThrow(() -> validator.validateSchema(validSchema));
    }

    @Test
    void validateSchema_withNullSchema_shouldThrowException() {
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> validator.validateSchema(null)
        );
        assertEquals("Template schema JSON cannot be null or empty", exception.getMessage());
    }

    @Test
    void validateSchema_withEmptySchema_shouldThrowException() {
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> validator.validateSchema("")
        );
        assertEquals("Template schema JSON cannot be null or empty", exception.getMessage());
    }

    @Test
    void validateSchema_withInvalidJson_shouldThrowException() {
        String invalidJson = "{ invalid json }";

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> validator.validateSchema(invalidJson)
        );
        assertTrue(exception.getMessage().contains("Invalid JSON format"));
    }

    @Test
    void validateSchema_withMissingVersion_shouldThrowException() {
        String schemaWithoutVersion = """
            {
              "sections": []
            }
            """;

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> validator.validateSchema(schemaWithoutVersion)
        );
        assertEquals("Template schema must have a 'version' field", exception.getMessage());
    }

    @Test
    void validateSchema_withMissingSections_shouldThrowException() {
        String schemaWithoutSections = """
            {
              "version": "3.0"
            }
            """;

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> validator.validateSchema(schemaWithoutSections)
        );
        assertEquals("Template schema must have a 'sections' field", exception.getMessage());
    }

    @Test
    void validateSchema_withEmptySections_shouldThrowException() {
        String schemaWithEmptySections = """
            {
              "version": "3.0",
              "sections": []
            }
            """;

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> validator.validateSchema(schemaWithEmptySections)
        );
        assertEquals("Template must have at least one section", exception.getMessage());
    }

    @Test
    void validateSchema_withInvalidSectionType_shouldThrowException() {
        String schemaWithInvalidSectionType = """
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

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> validator.validateSchema(schemaWithInvalidSectionType)
        );
        assertTrue(exception.getMessage().contains("Invalid section type 'invalid_type'"));
    }

    @Test
    void validateSchema_withDuplicateSectionType_shouldThrowException() {
        String schemaWithDuplicateSectionType = """
            {
              "version": "3.0",
              "sections": [
                {
                  "sectionType": "key_responsibilities",
                  "title": "Key Responsibilities 1"
                },
                {
                  "sectionType": "key_responsibilities",
                  "title": "Key Responsibilities 2"
                }
              ]
            }
            """;

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> validator.validateSchema(schemaWithDuplicateSectionType)
        );
        assertEquals("Duplicate section type 'key_responsibilities' found", exception.getMessage());
    }

    @Test
    void validateSchema_withMissingSectionTitle_shouldThrowException() {
        String schemaWithoutTitle = """
            {
              "version": "3.0",
              "sections": [
                {
                  "sectionType": "key_responsibilities"
                }
              ]
            }
            """;

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> validator.validateSchema(schemaWithoutTitle)
        );
        assertEquals("Section at index 0 must have a 'title' field", exception.getMessage());
    }

    @Test
    void validateSchema_withMissingItemId_shouldThrowException() {
        String schemaWithoutItemId = """
            {
              "version": "3.0",
              "sections": [
                {
                  "sectionType": "key_responsibilities",
                  "title": "Key Responsibilities",
                  "items": [
                    {
                      "label": "Essential Duty 1",
                      "ratingScale": "competency"
                    }
                  ]
                }
              ]
            }
            """;

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> validator.validateSchema(schemaWithoutItemId)
        );
        assertEquals("Item at index 0 in section 0 must have an 'id' field", exception.getMessage());
    }

    @Test
    void validateSchema_withDuplicateItemId_shouldThrowException() {
        String schemaWithDuplicateItemId = """
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
                    },
                    {
                      "id": "kr_1",
                      "label": "Essential Duty 2",
                      "ratingScale": "competency"
                    }
                  ]
                }
              ]
            }
            """;

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> validator.validateSchema(schemaWithDuplicateItemId)
        );
        assertEquals("Duplicate item id 'kr_1' found in section 0", exception.getMessage());
    }

    @Test
    void validateSchema_withInvalidRatingScale_shouldThrowException() {
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
                      "label": "Essential Duty 1",
                      "ratingScale": "invalid_scale"
                    }
                  ]
                }
              ]
            }
            """;

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> validator.validateSchema(schemaWithInvalidRatingScale)
        );
        assertTrue(exception.getMessage().contains("Invalid rating scale 'invalid_scale'"));
    }

    @Test
    void validateSchema_withMultipleSections_shouldPass() {
        String validMultiSectionSchema = """
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
                },
                {
                  "sectionType": "idp",
                  "title": "Individual Development Plan",
                  "items": [
                    {
                      "id": "idp_1",
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
                      "id": "policy_1",
                      "label": "Follow HR Policy",
                      "ratingScale": "policy_1_10"
                    }
                  ]
                }
              ]
            }
            """;

        assertDoesNotThrow(() -> validator.validateSchema(validMultiSectionSchema));
    }

    @Test
    void getValidationErrors_withValidSchema_shouldReturnEmptyList() {
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

        List<String> errors = validator.getValidationErrors(validSchema);
        assertTrue(errors.isEmpty());
    }

    @Test
    void getValidationErrors_withInvalidSchema_shouldReturnErrors() {
        String invalidSchema = """
            {
              "sections": [
                {
                  "sectionType": "invalid_type",
                  "items": []
                }
              ]
            }
            """;

        List<String> errors = validator.getValidationErrors(invalidSchema);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("version")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("title")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("invalid_type")));
    }

    @Test
    void validateSchema_withAllValidSectionTypes_shouldPass() {
        String schemaWithAllSectionTypes = """
            {
              "version": "3.0",
              "sections": [
                {"sectionType": "header", "title": "Header"},
                {"sectionType": "rating_key", "title": "Rating Key"},
                {"sectionType": "key_responsibilities", "title": "Key Responsibilities"},
                {"sectionType": "idp", "title": "IDP"},
                {"sectionType": "policy_adherence", "title": "Policy Adherence"},
                {"sectionType": "goals", "title": "Goals"},
                {"sectionType": "next_year_goals", "title": "Next Year Goals"},
                {"sectionType": "overall_evaluation", "title": "Overall Evaluation"},
                {"sectionType": "signature", "title": "Signature"}
              ]
            }
            """;

        assertDoesNotThrow(() -> validator.validateSchema(schemaWithAllSectionTypes));
    }
}
