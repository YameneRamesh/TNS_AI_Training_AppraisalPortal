package com.tns.appraisal.form;

import com.tns.appraisal.common.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;


/**
 * Entity representing an employee's appraisal form within a cycle.
 * Each form is unique per (cycle, employee) pair.
 */
@Entity
@Table(
    name = "appraisal_forms",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_form_cycle_employee",
        columnNames = {"cycle_id", "employee_id"}
    )
)
public class AppraisalForm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO: Replace with @ManyToOne to AppraisalCycle once that entity is created (task 2.2.1)
    @Column(name = "cycle_id", nullable = false)
    private Long cycleId;

    // TODO: Replace with @ManyToOne to User once that entity is created (task 1.1.1)
    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    // TODO: Replace with @ManyToOne to User once that entity is created (task 1.1.1)
    @Column(name = "manager_id", nullable = false)
    private Long managerId;

    // TODO: Replace with @ManyToOne to User once that entity is created (task 1.1.1)
    @Column(name = "backup_reviewer_id")
    private Long backupReviewerId;

    // TODO: Replace with @ManyToOne to AppraisalTemplate once that entity is created (task 2.1.1)
    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private FormStatus status = FormStatus.NOT_STARTED;

    @Convert(converter = FormDataConverter.class)
    @Column(name = "form_data", columnDefinition = "NVARCHAR(MAX)")
    private FormData formData;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "review_started_at")
    private Instant reviewStartedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "pdf_storage_path", columnDefinition = "NVARCHAR(500)")
    private String pdfStoragePath;

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

    public FormData getFormData() { return formData; }
    public void setFormData(FormData formData) { this.formData = formData; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public Instant getReviewStartedAt() { return reviewStartedAt; }
    public void setReviewStartedAt(Instant reviewStartedAt) { this.reviewStartedAt = reviewStartedAt; }

    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getPdfStoragePath() { return pdfStoragePath; }
    public void setPdfStoragePath(String pdfStoragePath) { this.pdfStoragePath = pdfStoragePath; }
}
