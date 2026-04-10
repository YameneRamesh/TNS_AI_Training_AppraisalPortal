package com.tns.appraisal.user.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Request DTO for assigning a manager to an employee.
 */
public class AssignManagerRequest {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Manager ID is required")
    private Long managerId;

    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;

    // Constructors
    public AssignManagerRequest() {
    }

    public AssignManagerRequest(Long employeeId, Long managerId, LocalDate effectiveDate) {
        this.employeeId = employeeId;
        this.managerId = managerId;
        this.effectiveDate = effectiveDate;
    }

    // Getters and Setters
    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }
}
