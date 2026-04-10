package com.tns.appraisal.user;

import com.tns.appraisal.audit.AuditLogService;
import com.tns.appraisal.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing reporting hierarchy relationships.
 * Supports creating, updating, and querying manager-employee relationships
 * with historical tracking.
 * 
 * Implements Requirement 15: Reporting Hierarchy and Backup Assignment.
 */
@Service
public class ReportingHierarchyService {

    private static final Logger logger = LoggerFactory.getLogger(ReportingHierarchyService.class);

    private final ReportingHierarchyRepository reportingHierarchyRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public ReportingHierarchyService(ReportingHierarchyRepository reportingHierarchyRepository,
                                    UserRepository userRepository,
                                    AuditLogService auditLogService) {
        this.reportingHierarchyRepository = reportingHierarchyRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Assign a manager to an employee, creating a new reporting relationship.
     * If the employee already has an active manager, this will fail.
     * Use changeManager() to update an existing relationship.
     *
     * @param employeeId employee's user ID
     * @param managerId manager's user ID
     * @param effectiveDate date when the relationship becomes effective
     * @param performedByUserId ID of the user performing this action
     * @return created reporting hierarchy record
     * @throws BusinessException if validation fails or active relationship exists
     */
    @Transactional
    public ReportingHierarchy assignManager(Long employeeId, Long managerId, 
                                           LocalDate effectiveDate, Long performedByUserId) {
        logger.debug("Assigning manager {} to employee {} effective {}", managerId, employeeId, effectiveDate);

        // Validate inputs
        validateManagerAssignment(employeeId, managerId, effectiveDate);

        User employee = userRepository.findById(employeeId)
            .orElseThrow(() -> new BusinessException("Employee with ID " + employeeId + " not found"));
        User manager = userRepository.findById(managerId)
            .orElseThrow(() -> new BusinessException("Manager with ID " + managerId + " not found"));

        // Check if employee already has an active manager
        reportingHierarchyRepository.findCurrentByEmployee(employee).ifPresent(existing -> {
            throw new BusinessException("Employee already has an active manager. Use changeManager() to update.");
        });

        // Create new reporting relationship
        ReportingHierarchy hierarchy = new ReportingHierarchy(employee, manager, effectiveDate);
        ReportingHierarchy saved = reportingHierarchyRepository.save(hierarchy);

        // Sync with User.manager field
        employee.setManager(manager);
        userRepository.save(employee);

        logger.info("Manager assigned: employee {} now reports to manager {} effective {}", 
            employeeId, managerId, effectiveDate);

        // Audit log
        auditLogService.logAsync(performedByUserId, "MANAGER_ASSIGNED", "ReportingHierarchy", saved.getId(),
            Map.of("employeeId", employeeId, "managerId", managerId, "effectiveDate", effectiveDate.toString()),
            null);

        return saved;
    }

    /**
     * Change an employee's manager, ending the current relationship and creating a new one.
     * This maintains historical records by setting an end date on the current relationship.
     *
     * @param employeeId employee's user ID
     * @param newManagerId new manager's user ID
     * @param effectiveDate date when the new relationship becomes effective
     * @param performedByUserId ID of the user performing this action
     * @return newly created reporting hierarchy record
     * @throws BusinessException if validation fails or no current manager exists
     */
    @Transactional
    public ReportingHierarchy changeManager(Long employeeId, Long newManagerId, 
                                           LocalDate effectiveDate, Long performedByUserId) {
        logger.debug("Changing manager for employee {} to {} effective {}", 
            employeeId, newManagerId, effectiveDate);

        // Validate inputs
        validateManagerAssignment(employeeId, newManagerId, effectiveDate);

        User employee = userRepository.findById(employeeId)
            .orElseThrow(() -> new BusinessException("Employee with ID " + employeeId + " not found"));
        User newManager = userRepository.findById(newManagerId)
            .orElseThrow(() -> new BusinessException("Manager with ID " + newManagerId + " not found"));

        // Find current active relationship
        ReportingHierarchy currentHierarchy = reportingHierarchyRepository.findCurrentByEmployee(employee)
            .orElseThrow(() -> new BusinessException("Employee does not have an active manager to change"));

        Long oldManagerId = currentHierarchy.getManager().getId();

        // End current relationship (set end date to day before new effective date)
        LocalDate endDate = effectiveDate.minusDays(1);
        currentHierarchy.setEndDate(endDate);
        reportingHierarchyRepository.save(currentHierarchy);

        // Create new relationship
        ReportingHierarchy newHierarchy = new ReportingHierarchy(employee, newManager, effectiveDate);
        ReportingHierarchy saved = reportingHierarchyRepository.save(newHierarchy);

        // Sync with User.manager field
        employee.setManager(newManager);
        userRepository.save(employee);

        logger.info("Manager changed: employee {} now reports to manager {} (was {}) effective {}", 
            employeeId, newManagerId, oldManagerId, effectiveDate);

        // Audit log
        auditLogService.logAsync(performedByUserId, "MANAGER_CHANGED", "ReportingHierarchy", saved.getId(),
            Map.of("employeeId", employeeId, "oldManagerId", oldManagerId, 
                   "newManagerId", newManagerId, "effectiveDate", effectiveDate.toString()),
            null);

        return saved;
    }

    /**
     * Get the current active manager for an employee.
     *
     * @param employeeId employee's user ID
     * @return current reporting hierarchy record, or null if none exists
     */
    @Transactional(readOnly = true)
    public ReportingHierarchy getCurrentManager(Long employeeId) {
        logger.debug("Fetching current manager for employee {}", employeeId);

        User employee = userRepository.findById(employeeId)
            .orElseThrow(() -> new BusinessException("Employee with ID " + employeeId + " not found"));

        return reportingHierarchyRepository.findCurrentByEmployee(employee).orElse(null);
    }

    /**
     * Get all current direct reports for a manager.
     *
     * @param managerId manager's user ID
     * @return list of current active reporting relationships
     */
    @Transactional(readOnly = true)
    public List<ReportingHierarchy> getDirectReports(Long managerId) {
        logger.debug("Fetching direct reports for manager {}", managerId);

        User manager = userRepository.findById(managerId)
            .orElseThrow(() -> new BusinessException("Manager with ID " + managerId + " not found"));

        return reportingHierarchyRepository.findCurrentByManager(manager);
    }

    /**
     * Get the complete reporting history for an employee.
     * Returns all past and current manager relationships ordered by effective date.
     *
     * @param employeeId employee's user ID
     * @return list of all reporting relationships for the employee
     */
    @Transactional(readOnly = true)
    public List<ReportingHierarchy> getReportingHistory(Long employeeId) {
        logger.debug("Fetching reporting history for employee {}", employeeId);

        User employee = userRepository.findById(employeeId)
            .orElseThrow(() -> new BusinessException("Employee with ID " + employeeId + " not found"));

        return reportingHierarchyRepository.findAllByEmployee(employee);
    }

    /**
     * Get the manager for an employee on a specific date (historical query).
     *
     * @param employeeId employee's user ID
     * @param date date to check
     * @return reporting hierarchy record active on that date, or null if none
     */
    @Transactional(readOnly = true)
    public ReportingHierarchy getManagerOnDate(Long employeeId, LocalDate date) {
        logger.debug("Fetching manager for employee {} on date {}", employeeId, date);

        User employee = userRepository.findById(employeeId)
            .orElseThrow(() -> new BusinessException("Employee with ID " + employeeId + " not found"));

        return reportingHierarchyRepository.findByEmployeeAndDate(employee, date).orElse(null);
    }

    /**
     * Get direct reports for a manager on a specific date (historical query).
     *
     * @param managerId manager's user ID
     * @param date date to check
     * @return list of reporting relationships active on that date
     */
    @Transactional(readOnly = true)
    public List<ReportingHierarchy> getDirectReportsOnDate(Long managerId, LocalDate date) {
        logger.debug("Fetching direct reports for manager {} on date {}", managerId, date);

        User manager = userRepository.findById(managerId)
            .orElseThrow(() -> new BusinessException("Manager with ID " + managerId + " not found"));

        return reportingHierarchyRepository.findByManagerAndDate(manager, date);
    }

    /**
     * Get all currently active reporting relationships in the organization.
     *
     * @return list of all active reporting relationships
     */
    @Transactional(readOnly = true)
    public List<ReportingHierarchy> getAllActiveRelationships() {
        logger.debug("Fetching all active reporting relationships");
        return reportingHierarchyRepository.findAllActive();
    }

    /**
     * End a reporting relationship by setting an end date.
     * This is used when an employee leaves or a manager is removed.
     *
     * @param employeeId employee's user ID
     * @param endDate date when the relationship ends
     * @param performedByUserId ID of the user performing this action
     * @throws BusinessException if no active relationship exists
     */
    @Transactional
    public void endReportingRelationship(Long employeeId, LocalDate endDate, Long performedByUserId) {
        logger.debug("Ending reporting relationship for employee {} on {}", employeeId, endDate);

        User employee = userRepository.findById(employeeId)
            .orElseThrow(() -> new BusinessException("Employee with ID " + employeeId + " not found"));

        ReportingHierarchy currentHierarchy = reportingHierarchyRepository.findCurrentByEmployee(employee)
            .orElseThrow(() -> new BusinessException("Employee does not have an active manager"));

        Long managerId = currentHierarchy.getManager().getId();

        currentHierarchy.setEndDate(endDate);
        reportingHierarchyRepository.save(currentHierarchy);

        // Clear User.manager field if ending today or in the past
        if (!endDate.isAfter(LocalDate.now())) {
            employee.setManager(null);
            userRepository.save(employee);
        }

        logger.info("Reporting relationship ended: employee {} no longer reports to manager {} as of {}", 
            employeeId, managerId, endDate);

        // Audit log
        auditLogService.logAsync(performedByUserId, "REPORTING_RELATIONSHIP_ENDED", 
            "ReportingHierarchy", currentHierarchy.getId(),
            Map.of("employeeId", employeeId, "managerId", managerId, "endDate", endDate.toString()),
            null);
    }

    // Private validation methods

    private void validateManagerAssignment(Long employeeId, Long managerId, LocalDate effectiveDate) {
        if (employeeId == null) {
            throw new BusinessException("Employee ID is required");
        }
        if (managerId == null) {
            throw new BusinessException("Manager ID is required");
        }
        if (effectiveDate == null) {
            throw new BusinessException("Effective date is required");
        }
        if (employeeId.equals(managerId)) {
            throw new BusinessException("Employee cannot be their own manager");
        }
    }
}
