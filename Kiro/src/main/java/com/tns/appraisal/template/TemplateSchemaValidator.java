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
 * Supports both flat item format (id/label/ratingScale on items)
 * and nested format (items with fields arrays, as used in TnS V3.0).
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

    public void validateSchema(String schemaJson) {
        if (schemaJson == null || schemaJson.trim().isEmpty()) {
            throw new BusinessException("Template schema JSON cannot be null or empty");
        }

        try {
            JsonNode rootNode = objectMapper.readTree(schemaJson);
            validateRootStructure(rootNode);
            validateVersion(rootNode);
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
        if (versionNode.asText().trim().isEmpty()) {
            throw new BusinessException("Template 'version' field cannot be empty");
        }
    }

    private void validateSections(JsonNode rootNode) {
        JsonNode sectionsNode = rootNode.get("sections");
        if (!sectionsNode.isArray()) {
            throw new BusinessException("Template 'sections' field must be an array");
        }
        if (sectionsNode.isEmpty()) {
            throw new BusinessException("Template must have at least one section");
        }

        Set<String> seenSectionTypes = new HashSet<>();
        for (int i = 0; i < sectionsNode.size(); i++) {
            validateSection(sectionsNode.get(i), i, seenSectionTypes);
        }
    }

    private void validateSection(JsonNode section, int index, Set<String> seenSectionTypes) {
        if (!section.isObject()) {
            throw new BusinessException("Section at index " + index + " must be a JSON object");
        }

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

        if (seenSectionTypes.contains(sectionType)) {
            throw new BusinessException("Duplicate section type '" + sectionType + "' found");
        }
        seenSectionTypes.add(sectionType);

        if (!section.has("title")) {
            throw new BusinessException("Section at index " + index + " must have a 'title' field");
        }

        JsonNode titleNode = section.get("title");
        if (!titleNode.isTextual()) {
            throw new BusinessException("Section 'title' at index " + index + " must be a string");
        }

        if (section.has("items")) {
            validateItems(section.get("items"), sectionType, index);
        }

        if (section.has("fields")) {
            validateFields(section.get("fields"), index);
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
            validateItem(itemsNode.get(i), sectionType, sectionIndex, i, seenItemIds);
        }
    }

    /**
     * Items can be either:
     *  - Flat format: { id, label, ratingScale, ... } (simple items)
     *  - Nested format: { id, label, fields: [...] } (items with nested field definitions)
     * Both formats are valid.
     */
    private void validateItem(JsonNode item, String sectionType, int sectionIndex,
                              int itemIndex, Set<String> seenItemIds) {
        if (!item.isObject()) {
            throw new BusinessException(
                "Item at index " + itemIndex + " in section " + sectionIndex + " must be a JSON object"
            );
        }

        if (item.has("id")) {
            JsonNode idNode = item.get("id");
            if (idNode.isTextual()) {
                String itemId = idNode.asText();
                if (!itemId.trim().isEmpty()) {
                    if (seenItemIds.contains(itemId)) {
                        throw new BusinessException(
                            "Duplicate item id '" + itemId + "' found in section " + sectionIndex
                        );
                    }
                    seenItemIds.add(itemId);
                }
            }
        }

        if (item.has("label")) {
            JsonNode labelNode = item.get("label");
            if (!labelNode.isTextual()) {
                throw new BusinessException(
                    "Item 'label' at index " + itemIndex + " in section " + sectionIndex + " must be a string"
                );
            }
        }

        if (item.has("ratingScale")) {
            JsonNode ratingScaleNode = item.get("ratingScale");
            if (ratingScaleNode.isTextual()) {
                String ratingScale = ratingScaleNode.asText();
                if (!VALID_RATING_SCALES.contains(ratingScale)) {
                    throw new BusinessException(
                        "Invalid rating scale '" + ratingScale + "' at item " + itemIndex +
                        " in section " + sectionIndex + ". Valid scales are: " + VALID_RATING_SCALES
                    );
                }
            }
        }

        if (item.has("fields")) {
            validateFields(item.get("fields"), sectionIndex);
        }

        if (item.has("ratings")) {
            validateRatings(item.get("ratings"), sectionIndex, itemIndex);
        }
    }

    private void validateFields(JsonNode fieldsNode, int sectionIndex) {
        if (!fieldsNode.isArray()) {
            throw new BusinessException(
                "Fields at section index " + sectionIndex + " must be an array"
            );
        }

        for (int i = 0; i < fieldsNode.size(); i++) {
            JsonNode field = fieldsNode.get(i);
            if (!field.isObject()) {
                throw new BusinessException(
                    "Field at index " + i + " in section " + sectionIndex + " must be a JSON object"
                );
            }

            if (field.has("ratingScale")) {
                JsonNode ratingScaleNode = field.get("ratingScale");
                if (ratingScaleNode.isTextual()) {
                    String ratingScale = ratingScaleNode.asText();
                    if (!VALID_RATING_SCALES.contains(ratingScale)) {
                        throw new BusinessException(
                            "Invalid rating scale '" + ratingScale + "' at field " + i +
                            " in section " + sectionIndex + ". Valid scales are: " + VALID_RATING_SCALES
                        );
                    }
                }
            }
        }
    }

    private void validateRatings(JsonNode ratingsNode, int sectionIndex, int itemIndex) {
        if (!ratingsNode.isArray()) {
            throw new BusinessException(
                "Ratings at item " + itemIndex + " in section " + sectionIndex + " must be an array"
            );
        }
    }

    /**
     * Collect all validation errors without throwing.
     */
    public List<String> getValidationErrors(String schemaJson) {
        List<String> errors = new ArrayList<>();

        if (schemaJson == null || schemaJson.trim().isEmpty()) {
            errors.add("Template schema JSON cannot be null or empty");
            return errors;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(schemaJson);

            if (!rootNode.isObject()) {
                errors.add("Template schema must be a JSON object");
                return errors;
            }

            if (!rootNode.has("version")) {
                errors.add("Template schema must have a 'version' field");
            } else {
                JsonNode vn = rootNode.get("version");
                if (!vn.isTextual()) errors.add("Template 'version' field must be a string");
                else if (vn.asText().trim().isEmpty()) errors.add("Template 'version' field cannot be empty");
            }

            if (!rootNode.has("sections")) {
                errors.add("Template schema must have a 'sections' field");
            } else {
                JsonNode sn = rootNode.get("sections");
                if (!sn.isArray()) errors.add("Template 'sections' field must be an array");
                else if (sn.isEmpty()) errors.add("Template must have at least one section");
                else {
                    Set<String> seenTypes = new HashSet<>();
                    for (int i = 0; i < sn.size(); i++) {
                        collectSectionErrors(sn.get(i), i, seenTypes, errors);
                    }
                }
            }
        } catch (Exception e) {
            errors.add("Invalid JSON format: " + e.getMessage());
        }

        return errors;
    }

    private void collectSectionErrors(JsonNode section, int index,
                                      Set<String> seenTypes, List<String> errors) {
        if (!section.isObject()) {
            errors.add("Section at index " + index + " must be a JSON object");
            return;
        }

        if (!section.has("sectionType")) {
            errors.add("Section at index " + index + " must have a 'sectionType' field");
        } else {
            JsonNode st = section.get("sectionType");
            if (!st.isTextual()) {
                errors.add("Section 'sectionType' at index " + index + " must be a string");
            } else {
                String type = st.asText();
                if (!VALID_SECTION_TYPES.contains(type)) {
                    errors.add("Invalid section type '" + type + "' at index " + index);
                }
                if (seenTypes.contains(type)) {
                    errors.add("Duplicate section type '" + type + "' found");
                }
                seenTypes.add(type);
            }
        }

        if (!section.has("title")) {
            errors.add("Section at index " + index + " must have a 'title' field");
        } else if (!section.get("title").isTextual()) {
            errors.add("Section 'title' at index " + index + " must be a string");
        }
    }
}
