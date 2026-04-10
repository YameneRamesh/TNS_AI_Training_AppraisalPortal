package com.tns.appraisal.form.dto;

import com.tns.appraisal.form.FormStatus;
import java.time.Instant;

/**
 * Full DTO for a single appraisal form including parsed form data.
 */
public class FormDetailDto {

    private Long id;
    private Long cycleId;
    private Long employeeId;
    private Long managerId;
    private Long backupReviewerId;
    private Long templateId;
    private FormStatus status;
    private FormDataDto formData;
    private Instant submittedAt;
    private Instant reviewStartedAt;
    private Instant reviewedAt;
    private String pdfStoragePath;
    private Instant createdAt;
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCycleId() { return cycleId; }
    public void setCycleId(Long cycleId) { this.cycleId = cycleId; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public Long getManagerId() { return managerId; }
    public void setManagerId(Long managerId) { this.managerId = managerId; }

    public Long getBackupReviewerId() { return backupReviewerId; }
    public void setBackupReviewerId(Long backupReviewerId) { this.backupReviewerId = backupReviewerId; }

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }

    public FormStatus getStatus() { return status; }
    public void setStatus(FormStatus status) { this.status = status; }

    public FormDataDto getFormData() { return formData; }
    public void setFormData(FormDataDto formData) { this.formData = formData; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public Instant getReviewStartedAt() { return reviewStartedAt; }
    public void setReviewStartedAt(Instant reviewStartedAt) { this.reviewStartedAt = reviewStartedAt; }

    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getPdfStoragePath() { return pdfStoragePath; }
    public void setPdfStoragePath(String pdfStoragePath) { this.pdfStoragePath = pdfStoragePath; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
