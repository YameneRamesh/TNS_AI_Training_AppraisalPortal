package com.tns.appraisal.user.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Request DTO for ending a reporting relationship.
 */
public class EndRelationshipRequest {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    // Constructors
    public EndRelationshipRequest() {
    }

    public EndRelationshipRequest(Long employeeId, LocalDate endDate) {
        this.employeeId = employeeId;
        this.endDate = endDate;
    }

    // Getters and Setters
    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
