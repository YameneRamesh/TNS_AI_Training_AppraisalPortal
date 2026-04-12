package com.tns.appraisal.form;

import java.time.Instant;

/**
 * DTO for AppraisalForm data returned to the frontend.
 * Flattens entity relationships into simple fields.
 */
public class AppraisalFormDto {

    private Long id;
    private Long cycleId;
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private Long managerId;
    private String managerName;
    private Long backupReviewerId;
    private String backupReviewerName;
    private Long templateId;
    private String status;
    private Instant submittedAt;
    private Instant reviewStartedAt;
    private Instant reviewedAt;
    private String pdfStoragePath;
    private Instant createdAt;
    private Instant updatedAt;

    public AppraisalFormDto() {
    }

    public static AppraisalFormDto fromEntity(AppraisalForm form) {
        AppraisalFormDto dto = new AppraisalFormDto();
        dto.setId(form.getId());
        dto.setCycleId(form.getCycle().getId());
        dto.setEmployeeId(form.getEmployee().getId());
        dto.setEmployeeName(form.getEmployee().getFullName());
        dto.setEmployeeCode(form.getEmployee().getEmployeeId());
        dto.setManagerId(form.getManager().getId());
        dto.setManagerName(form.getManager().getFullName());

        if (form.getBackupReviewer() != null) {
            dto.setBackupReviewerId(form.getBackupReviewer().getId());
            dto.setBackupReviewerName(form.getBackupReviewer().getFullName());
        }

        dto.setTemplateId(form.getTemplate().getId());
        dto.setStatus(form.getStatus());
        dto.setSubmittedAt(form.getSubmittedAt());
        dto.setReviewStartedAt(form.getReviewStartedAt());
        dto.setReviewedAt(form.getReviewedAt());
        dto.setPdfStoragePath(form.getPdfStoragePath());
        dto.setCreatedAt(form.getCreatedAt());
        dto.setUpdatedAt(form.getUpdatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCycleId() { return cycleId; }
    public void setCycleId(Long cycleId) { this.cycleId = cycleId; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public Long getManagerId() { return managerId; }
    public void setManagerId(Long managerId) { this.managerId = managerId; }

    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }

    public Long getBackupReviewerId() { return backupReviewerId; }
    public void setBackupReviewerId(Long backupReviewerId) { this.backupReviewerId = backupReviewerId; }

    public String getBackupReviewerName() { return backupReviewerName; }
    public void setBackupReviewerName(String backupReviewerName) { this.backupReviewerName = backupReviewerName; }

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

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
