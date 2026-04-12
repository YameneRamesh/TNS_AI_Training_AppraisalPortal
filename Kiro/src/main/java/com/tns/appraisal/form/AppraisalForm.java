package com.tns.appraisal.form;

import com.tns.appraisal.common.BaseEntity;
import com.tns.appraisal.cycle.AppraisalCycle;
import com.tns.appraisal.template.AppraisalTemplate;
import com.tns.appraisal.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "appraisal_forms",
       uniqueConstraints = @UniqueConstraint(columnNames = {"cycle_id", "employee_id"}))
public class AppraisalForm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    private AppraisalCycle cycle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    private User manager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "backup_reviewer_id")
    private User backupReviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private AppraisalTemplate template;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "NOT_STARTED";

    @Column(name = "form_data", columnDefinition = "NVARCHAR(MAX)")
    private String formData;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "review_started_at")
    private Instant reviewStartedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "pdf_storage_path", length = 500)
    private String pdfStoragePath;

    public AppraisalForm() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppraisalCycle getCycle() {
        return cycle;
    }

    public void setCycle(AppraisalCycle cycle) {
        this.cycle = cycle;
    }

    public User getEmployee() {
        return employee;
    }

    public void setEmployee(User employee) {
        this.employee = employee;
    }

    public User getManager() {
        return manager;
    }

    public void setManager(User manager) {
        this.manager = manager;
    }

    public User getBackupReviewer() {
        return backupReviewer;
    }

    public void setBackupReviewer(User backupReviewer) {
        this.backupReviewer = backupReviewer;
    }

    public AppraisalTemplate getTemplate() {
        return template;
    }

    public void setTemplate(AppraisalTemplate template) {
        this.template = template;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFormData() {
        return formData;
    }

    public void setFormData(String formData) {
        this.formData = formData;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Instant getReviewStartedAt() {
        return reviewStartedAt;
    }

    public void setReviewStartedAt(Instant reviewStartedAt) {
        this.reviewStartedAt = reviewStartedAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getPdfStoragePath() {
        return pdfStoragePath;
    }

    public void setPdfStoragePath(String pdfStoragePath) {
        this.pdfStoragePath = pdfStoragePath;
    }
}
