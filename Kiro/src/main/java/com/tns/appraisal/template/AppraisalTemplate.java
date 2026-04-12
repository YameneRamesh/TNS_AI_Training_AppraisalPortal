package com.tns.appraisal.template;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entity representing an appraisal template that defines the structure of appraisal forms.
 * Templates are versioned and can be activated/deactivated.
 * Note: appraisal_templates table only has created_at (no updated_at), so this entity
 * does not extend BaseEntity.
 */
@Entity
@Table(name = "appraisal_templates")
public class AppraisalTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version", nullable = false, length = 50)
    private String version;

    @Column(name = "schema_json", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String schemaJson;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "datetime2")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public AppraisalTemplate() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSchemaJson() {
        return schemaJson;
    }

    public void setSchemaJson(String schemaJson) {
        this.schemaJson = schemaJson;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
