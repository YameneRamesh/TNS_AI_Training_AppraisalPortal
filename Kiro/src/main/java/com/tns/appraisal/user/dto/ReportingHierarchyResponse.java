package com.tns.appraisal.user.dto;

import com.tns.appraisal.user.ReportingHierarchy;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Response DTO for reporting hierarchy information.
 */
public class ReportingHierarchyResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeEmail;
    private Long managerId;
    private String managerName;
    private String managerEmail;
    private LocalDate effectiveDate;
    private LocalDate endDate;
    private Boolean isActive;
    private Instant createdAt;

    // Constructors
    public ReportingHierarchyResponse() {
    }

    public ReportingHierarchyResponse(ReportingHierarchy hierarchy) {
        this.id = hierarchy.getId();
        this.employeeId = hierarchy.getEmployee().getId();
        this.employeeName = hierarchy.getEmployee().getFullName();
        this.employeeEmail = hierarchy.getEmployee().getEmail();
        this.managerId = hierarchy.getManager().getId();
        this.managerName = hierarchy.getManager().getFullName();
        this.managerEmail = hierarchy.getManager().getEmail();
        this.effectiveDate = hierarchy.getEffectiveDate();
        this.endDate = hierarchy.getEndDate();
        this.isActive = hierarchy.isActive();
        this.createdAt = hierarchy.getCreatedAt();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getEmployeeEmail() {
        return employeeEmail;
    }

    public void setEmployeeEmail(String employeeEmail) {
        this.employeeEmail = employeeEmail;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public String getManagerEmail() {
        return managerEmail;
    }

    public void setManagerEmail(String managerEmail) {
        this.managerEmail = managerEmail;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
