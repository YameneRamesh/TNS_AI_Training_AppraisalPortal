package com.tns.appraisal.form;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter that serializes/deserializes {@link FormData} to/from
 * a JSON string for storage in the NVARCHAR(MAX) form_data column.
 */
@Converter
public class FormDataConverter implements AttributeConverter<FormData, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    @Override
    public String convertToDatabaseColumn(FormData formData) {
        if (formData == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(formData);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize FormData to JSON", e);
        }
    }

    @Override
    public FormData convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, FormData.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to deserialize FormData from JSON", e);
        }
    }
}
