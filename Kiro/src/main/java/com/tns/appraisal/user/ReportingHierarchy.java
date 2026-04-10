package com.tns.appraisal.user;

import com.tns.appraisal.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Entity representing the reporting hierarchy between employees and managers.
 * Maintains historical records of manager-employee relationships with effective dates.
 * 
 * This entity supports Requirement 15: Reporting Hierarchy and Backup Assignment.
 */
@Entity
@Table(name = "reporting_hierarchy", indexes = {
    @Index(name = "idx_employee_effective", columnList = "employee_id, effective_dt"),
    @Index(name = "idx_manager", columnList = "manager_id")
})
public class ReportingHierarchy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_id", nullable = false)
    private User manager;

    @Column(name = "effective_dt", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "end_dt")
    private LocalDate endDate;

    // Constructors
    public ReportingHierarchy() {
    }

    public ReportingHierarchy(User employee, User manager, LocalDate effectiveDate) {
        this.employee = employee;
        this.manager = manager;
        this.effectiveDate = effectiveDate;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    /**
     * Checks if this reporting relationship is currently active.
     * 
     * @return true if the relationship has no end date or the end date is in the future
     */
    public boolean isActive() {
        return endDate == null || endDate.isAfter(LocalDate.now());
    }

    /**
     * Checks if this reporting relationship was active on a specific date.
     * 
     * @param date the date to check
     * @return true if the relationship was active on the given date
     */
    public boolean isActiveOn(LocalDate date) {
        boolean afterStart = !effectiveDate.isAfter(date);
        boolean beforeEnd = endDate == null || !endDate.isBefore(date);
        return afterStart && beforeEnd;
    }
}
