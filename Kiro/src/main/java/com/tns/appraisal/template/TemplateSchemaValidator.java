package com.tns.appraisal.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tns.appraisal.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validator for appraisal template JSON schema structure.
 * Ensures templates conform to the expected structure defined in the design document.
 */
@Component
public class TemplateSchemaValidator {

    private static final Set<String> VALID_SECTION_TYPES = Set.of(
        "header", "rating_key", "key_responsibilities", "idp", 
        "policy_adherence", "goals", "next_year_goals", 
        "overall_evaluation", "signature"
    );

    private static final Set<String> VALID_RATING_SCALES = Set.of(
        "competency", "policy_1_10"
    );

    private final ObjectMapper objectMapper;

    public TemplateSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Validate the template schema JSON structure.
     * 
     * @param schemaJson the JSON schema string to validate
     * @throws BusinessException if validation fails
     */
    public void validateSchema(String schemaJson) {
        if (schemaJson == null || schemaJson.trim().isEmpty()) {
            throw new BusinessException("Template schema JSON cannot be null or empty");
        }

        try {
            JsonNode rootNode = objectMapper.readTree(schemaJson);
            
            // Validate root structure
            validateRootStructure(rootNode);
            
            // Validate version field
            validateVersion(rootNode);
            
            // Validate sections array
            validateSections(rootNode);
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Invalid JSON format: " + e.getMessage());
        }
    }

    private void validateRootStructure(JsonNode rootNode) {
        if (!rootNode.isObject()) {
            throw new BusinessException("Template schema must be a JSON object");
        }

        if (!rootNode.has("version")) {
            throw new BusinessException("Template schema must have a 'version' field");
        }

        if (!rootNode.has("sections")) {
            throw new BusinessException("Template schema must have a 'sections' field");
        }
    }

    private void validateVersion(JsonNode rootNode) {
        JsonNode versionNode = rootNode.get("version");
        
        if (!versionNode.isTextual()) {
            throw new BusinessException("Template 'version' field must be a string");
        }

        String version = versionNode.asText();
        if (version.trim().isEmpty()) {
            throw new BusinessException("Template 'version' field cannot be empty");
        }
    }

    private void validateSections(JsonNode rootNode) {
        JsonNode sectionsNode = rootNode.get("sections");
        
        if (!sectionsNode.isArray()) {
            throw new BusinessException("Template 'sections' field must be an array");
        }

        if (sectionsNode.size() == 0) {
            throw new BusinessException("Template must have at least one section");
        }

        Set<String> seenSectionTypes = new HashSet<>();
        
        for (int i = 0; i < sectionsNode.size(); i++) {
            JsonNode section = sectionsNode.get(i);
            validateSection(section, i, seenSectionTypes);
        }
    }

    private void validateSection(JsonNode section, int index, Set<String> seenSectionTypes) {
        if (!section.isObject()) {
            throw new BusinessException("Section at index " + index + " must be a JSON object");
        }

        // Validate sectionType
        if (!section.has("sectionType")) {
            throw new BusinessException("Section at index " + index + " must have a 'sectionType' field");
        }

        JsonNode sectionTypeNode = section.get("sectionType");
        if (!sectionTypeNode.isTextual()) {
            throw new BusinessException("Section 'sectionType' at index " + index + " must be a string");
        }

        String sectionType = sectionTypeNode.asText();
        if (!VALID_SECTION_TYPES.contains(sectionType)) {
            throw new BusinessException(
                "Invalid section type '" + sectionType + "' at index " + index + 
                ". Valid types are: " + VALID_SECTION_TYPES
            );
        }

        // Check for duplicate section types
        if (seenSectionTypes.contains(sectionType)) {
            throw new BusinessException("Duplicate section type '" + sectionType + "' found");
        }
        seenSectionTypes.add(sectionType);

        // Validate title
        if (!section.has("title")) {
            throw new BusinessException("Section at index " + index + " must have a 'title' field");
        }

        JsonNode titleNode = section.get("title");
        if (!titleNode.isTextual()) {
            throw new BusinessException("Section 'title' at index " + index + " must be a string");
        }

        // Validate items if present
        if (section.has("items")) {
            validateItems(section.get("items"), sectionType, index);
        }
    }

    private void validateItems(JsonNode itemsNode, String sectionType, int sectionIndex) {
        if (!itemsNode.isArray()) {
            throw new BusinessException(
                "Section 'items' at index " + sectionIndex + " must be an array"
            );
        }

        Set<String> seenItemIds = new HashSet<>();
        
        for (int i = 0; i < itemsNode.size(); i++) {
            JsonNode item = itemsNode.get(i);
            validateItem(item, sectionType, sectionIndex, i, seenItemIds);
        }
    }

    private void validateItem(JsonNode item, String sectionType, int sectionIndex, 
                              int itemIndex, Set<String> seenItemIds) {
        if (!item.isObject()) {
            throw new BusinessException(
                "Item at index " + itemIndex + " in section " + sectionIndex + " must be a JSON object"
            );
        }

        // Validate id
        if (!item.has("id")) {
            throw new BusinessException(
                "Item at index " + itemIndex + " in section " + sectionIndex + " must have an 'id' field"
            );
        }

        JsonNode idNode = item.get("id");
        if (!idNode.isTextual()) {
            throw new BusinessException(
                "Item 'id' at index " + itemIndex + " in section " + sectionIndex + " must be a string"
            );
        }

        String itemId = idNode.asText();
        if (itemId.trim().isEmpty()) {
            throw new BusinessException(
                "Item 'id' at index " + itemIndex + " in section " + sectionIndex + " cannot be empty"
            );
        }

        // Check for duplicate item IDs
        if (seenItemIds.contains(itemId)) {
            throw new BusinessException(
                "Duplicate item id '" + itemId + "' found in section " + sectionIndex
            );
        }
        seenItemIds.add(itemId);

        // Validate label
        if (!item.has("label")) {
            throw new BusinessException(
                "Item at index " + itemIndex + " in section " + sectionIndex + " must have a 'label' field"
            );
        }

        JsonNode labelNode = item.get("label");
        if (!labelNode.isTextual()) {
            throw new BusinessException(
                "Item 'label' at index " + itemIndex + " in section " + sectionIndex + " must be a string"
            );
        }

        // Validate ratingScale
        if (!item.has("ratingScale")) {
            throw new BusinessException(
                "Item at index " + itemIndex + " in section " + sectionIndex + " must have a 'ratingScale' field"
            );
        }

        JsonNode ratingScaleNode = item.get("ratingScale");
        if (!ratingScaleNode.isTextual()) {
            throw new BusinessException(
                "Item 'ratingScale' at index " + itemIndex + " in section " + sectionIndex + " must be a string"
            );
        }

        String ratingScale = ratingScaleNode.asText();
        if (!VALID_RATING_SCALES.contains(ratingScale)) {
            throw new BusinessException(
                "Invalid rating scale '" + ratingScale + "' at item " + itemIndex + 
                " in section " + sectionIndex + ". Valid scales are: " + VALID_RATING_SCALES
            );
        }
    }

    /**
     * Get a list of validation errors without throwing an exception.
     * Useful for providing detailed feedback to users.
     * 
     * @param schemaJson the JSON schema string to validate
     * @return list of validation error messages (empty if valid)
     */
    public List<String> getValidationErrors(String schemaJson) {
        List<String> errors = new ArrayList<>();

        if (schemaJson == null || schemaJson.trim().isEmpty()) {
            errors.add("Template schema JSON cannot be null or empty");
            return errors;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(schemaJson);
            
            // Collect all validation errors
            collectRootStructureErrors(rootNode, errors);
            collectVersionErrors(rootNode, errors);
            collectSectionsErrors(rootNode, errors);
            
        } catch (Exception e) {
            errors.add("Invalid JSON format: " + e.getMessage());
        }

        return errors;
    }

    private void collectRootStructureErrors(JsonNode rootNode, List<String> errors) {
        if (!rootNode.isObject()) {
            errors.add("Template schema must be a JSON object");
            return;
        }

        if (!rootNode.has("version")) {
            errors.add("Template schema must have a 'version' field");
        }

        if (!rootNode.has("sections")) {
            errors.add("Template schema must have a 'sections' field");
        }
    }

    private void collectVersionErrors(JsonNode rootNode, List<String> errors) {
        if (!rootNode.has("version")) {
            return; // Already reported in root structure
        }

        JsonNode versionNode = rootNode.get("version");
        
        if (!versionNode.isTextual()) {
            errors.add("Template 'version' field must be a string");
            return;
        }

        String version = versionNode.asText();
        if (version.trim().isEmpty()) {
            errors.add("Template 'version' field cannot be empty");
        }
    }

    private void collectSectionsErrors(JsonNode rootNode, List<String> errors) {
        if (!rootNode.has("sections")) {
            return; // Already reported in root structure
        }

        JsonNode sectionsNode = rootNode.get("sections");
        
        if (!sectionsNode.isArray()) {
            errors.add("Template 'sections' field must be an array");
            return;
        }

        if (sectionsNode.size() == 0) {
            errors.add("Template must have at least one section");
            return;
        }

        Set<String> seenSectionTypes = new HashSet<>();
        
        for (int i = 0; i < sectionsNode.size(); i++) {
            JsonNode section = sectionsNode.get(i);
            collectSectionErrors(section, i, seenSectionTypes, errors);
        }
    }

    private void collectSectionErrors(JsonNode section, int index, 
                                      Set<String> seenSectionTypes, List<String> errors) {
        if (!section.isObject()) {
            errors.add("Section at index " + index + " must be a JSON object");
            return;
        }

        if (!section.has("sectionType")) {
            errors.add("Section at index " + index + " must have a 'sectionType' field");
        } else {
            JsonNode sectionTypeNode = section.get("sectionType");
            if (!sectionTypeNode.isTextual()) {
                errors.add("Section 'sectionType' at index " + index + " must be a string");
            } else {
                String sectionType = sectionTypeNode.asText();
                if (!VALID_SECTION_TYPES.contains(sectionType)) {
                    errors.add(
                        "Invalid section type '" + sectionType + "' at index " + index + 
                        ". Valid types are: " + VALID_SECTION_TYPES
                    );
                }
                if (seenSectionTypes.contains(sectionType)) {
                    errors.add("Duplicate section type '" + sectionType + "' found");
                }
                seenSectionTypes.add(sectionType);
            }
        }

        if (!section.has("title")) {
            errors.add("Section at index " + index + " must have a 'title' field");
        } else {
            JsonNode titleNode = section.get("title");
            if (!titleNode.isTextual()) {
                errors.add("Section 'title' at index " + index + " must be a string");
            }
        }

        if (section.has("items")) {
            collectItemsErrors(section.get("items"), index, errors);
        }
    }

    private void collectItemsErrors(JsonNode itemsNode, int sectionIndex, List<String> errors) {
        if (!itemsNode.isArray()) {
            errors.add("Section 'items' at index " + sectionIndex + " must be an array");
            return;
        }

        Set<String> seenItemIds = new HashSet<>();
        
        for (int i = 0; i < itemsNode.size(); i++) {
            JsonNode item = itemsNode.get(i);
            collectItemErrors(item, sectionIndex, i, seenItemIds, errors);
        }
    }

    private void collectItemErrors(JsonNode item, int sectionIndex, int itemIndex, 
                                   Set<String> seenItemIds, List<String> errors) {
        if (!item.isObject()) {
            errors.add("Item at index " + itemIndex + " in section " + sectionIndex + " must be a JSON object");
            return;
        }

        if (!item.has("id")) {
            errors.add("Item at index " + itemIndex + " in section " + sectionIndex + " must have an 'id' field");
        } else {
            JsonNode idNode = item.get("id");
            if (!idNode.isTextual()) {
                errors.add("Item 'id' at index " + itemIndex + " in section " + sectionIndex + " must be a string");
            } else {
                String itemId = idNode.asText();
                if (itemId.trim().isEmpty()) {
                    errors.add("Item 'id' at index " + itemIndex + " in section " + sectionIndex + " cannot be empty");
                }
                if (seenItemIds.contains(itemId)) {
                    errors.add("Duplicate item id '" + itemId + "' found in section " + sectionIndex);
                }
                seenItemIds.add(itemId);
            }
        }

        if (!item.has("label")) {
            errors.add("Item at index " + itemIndex + " in section " + sectionIndex + " must have a 'label' field");
        } else {
            JsonNode labelNode = item.get("label");
            if (!labelNode.isTextual()) {
                errors.add("Item 'label' at index " + itemIndex + " in section " + sectionIndex + " must be a string");
            }
        }

        if (!item.has("ratingScale")) {
            errors.add("Item at index " + itemIndex + " in section " + sectionIndex + " must have a 'ratingScale' field");
        } else {
            JsonNode ratingScaleNode = item.get("ratingScale");
            if (!ratingScaleNode.isTextual()) {
                errors.add("Item 'ratingScale' at index " + itemIndex + " in section " + sectionIndex + " must be a string");
            } else {
                String ratingScale = ratingScaleNode.asText();
                if (!VALID_RATING_SCALES.contains(ratingScale)) {
                    errors.add(
                        "Invalid rating scale '" + ratingScale + "' at item " + itemIndex + 
                        " in section " + sectionIndex + ". Valid scales are: " + VALID_RATING_SCALES
                    );
                }
            }
        }
    }
}
