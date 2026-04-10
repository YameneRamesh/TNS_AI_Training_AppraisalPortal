package com.tns.appraisal.form;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Competency rating scale used for key responsibilities, IDP, and goals sections.
 */
public enum Rating {
    EXCELS("Excels"),
    EXCEEDS("Exceeds"),
    MEETS("Meets"),
    DEVELOPING("Developing");

    private final String displayName;

    Rating(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static Rating fromValue(String value) {
        if (value == null) return null;
        for (Rating r : values()) {
            if (r.displayName.equalsIgnoreCase(value) || r.name().equalsIgnoreCase(value)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown rating: " + value);
    }
}
