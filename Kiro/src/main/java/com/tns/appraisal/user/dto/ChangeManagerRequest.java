package com.tns.appraisal.user.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Request DTO for changing an employee's manager.
 */
public class ChangeManagerRequest {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "New manager ID is required")
    private Long newManagerId;

    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;

    // Constructors
    public ChangeManagerRequest() {
    }

    public ChangeManagerRequest(Long employeeId, Long newManagerId, LocalDate effectiveDate) {
        this.employeeId = employeeId;
        this.newManagerId = newManagerId;
        this.effectiveDate = effectiveDate;
    }

    // Getters and Setters
    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Long getNewManagerId() {
        return newManagerId;
    }

    public void setNewManagerId(Long newManagerId) {
        this.newManagerId = newManagerId;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }
}
