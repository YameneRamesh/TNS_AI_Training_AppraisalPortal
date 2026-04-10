package com.tns.appraisal.user;

import com.tns.appraisal.auth.AuthService;
import com.tns.appraisal.common.dto.ApiResponse;
import com.tns.appraisal.user.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for managing reporting hierarchy relationships.
 * Provides endpoints for HR to manage organizational structure and
 * for users to query reporting relationships.
 * 
 * Implements Requirement 15: Reporting Hierarchy and Backup Assignment.
 */
@RestController
@RequestMapping("/api/reporting-hierarchy")
public class ReportingHierarchyController {

    private static final Logger logger = LoggerFactory.getLogger(ReportingHierarchyController.class);

    private final ReportingHierarchyService reportingHierarchyService;
    private final AuthService authService;

    public ReportingHierarchyController(ReportingHierarchyService reportingHierarchyService,
                                       AuthService authService) {
        this.reportingHierarchyService = reportingHierarchyService;
        this.authService = authService;
    }

    /**
     * Assign a manager to an employee.
     * Creates a new reporting relationship.
     * 
     * POST /api/reporting-hierarchy/assign
     * 
     * @param request assign manager request
     * @param httpRequest HTTP request for getting current user
     * @return created reporting hierarchy
     */
    @PostMapping("/assign")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReportingHierarchyResponse>> assignManager(
            @Valid @RequestBody AssignManagerRequest request,
            HttpServletRequest httpRequest) {
        
        logger.info("Assign manager request - employee: {}, manager: {}, effective: {}", 
            request.getEmployeeId(), request.getManagerId(), request.getEffectiveDate());

        User currentUser = authService.getCurrentUser(httpRequest);

        ReportingHierarchy hierarchy = reportingHierarchyService.assignManager(
            request.getEmployeeId(),
            request.getManagerId(),
            request.getEffectiveDate(),
            currentUser.getId()
        );

        ReportingHierarchyResponse response = new ReportingHierarchyResponse(hierarchy);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Manager assigned successfully", response));
    }

    /**
     * Change an employee's manager.
     * Ends the current relationship and creates a new one.
     * 
     * POST /api/reporting-hierarchy/change
     * 
     * @param request change manager request
     * @param httpRequest HTTP request for getting current user
     * @return new reporting hierarchy
     */
    @PostMapping("/change")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReportingHierarchyResponse>> changeManager(
            @Valid @RequestBody ChangeManagerRequest request,
            HttpServletRequest httpRequest) {
        
        logger.info("Change manager request - employee: {}, new manager: {}, effective: {}", 
            request.getEmployeeId(), request.getNewManagerId(), request.getEffectiveDate());

        User currentUser = authService.getCurrentUser(httpRequest);

        ReportingHierarchy hierarchy = reportingHierarchyService.changeManager(
            request.getEmployeeId(),
            request.getNewManagerId(),
            request.getEffectiveDate(),
            currentUser.getId()
        );

        ReportingHierarchyResponse response = new ReportingHierarchyResponse(hierarchy);
        return ResponseEntity.ok(ApiResponse.success("Manager changed successfully", response));
    }

    /**
     * End a reporting relationship.
     * Sets an end date on the current active relationship.
     * 
     * POST /api/reporting-hierarchy/end
     * 
     * @param request end relationship request
     * @param httpRequest HTTP request for getting current user
     * @return success message
     */
    @PostMapping("/end")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> endRelationship(
            @Valid @RequestBody EndRelationshipRequest request,
            HttpServletRequest httpRequest) {
        
        logger.info("End relationship request - employee: {}, end date: {}", 
            request.getEmployeeId(), request.getEndDate());

        User currentUser = authService.getCurrentUser(httpRequest);

        reportingHierarchyService.endReportingRelationship(
            request.getEmployeeId(),
            request.getEndDate(),
            currentUser.getId()
        );

        return ResponseEntity.ok(ApiResponse.success("Reporting relationship ended successfully", null));
    }

    /**
     * Get the current manager for an employee.
     * 
     * GET /api/reporting-hierarchy/employee/{employeeId}/current-manager
     * 
     * @param employeeId employee's user ID
     * @return current reporting hierarchy, or null if none
     */
    @GetMapping("/employee/{employeeId}/current-manager")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ReportingHierarchyResponse>> getCurrentManager(
            @PathVariable Long employeeId) {
        
        logger.debug("Get current manager request for employee: {}", employeeId);

        ReportingHierarchy hierarchy = reportingHierarchyService.getCurrentManager(employeeId);
        
        if (hierarchy == null) {
            return ResponseEntity.ok(ApiResponse.success("No current manager found", null));
        }

        ReportingHierarchyResponse response = new ReportingHierarchyResponse(hierarchy);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all direct reports for a manager.
     * 
     * GET /api/reporting-hierarchy/manager/{managerId}/direct-reports
     * 
     * @param managerId manager's user ID
     * @return list of current direct reports
     */
    @GetMapping("/manager/{managerId}/direct-reports")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<ReportingHierarchyResponse>>> getDirectReports(
            @PathVariable Long managerId) {
        
        logger.debug("Get direct reports request for manager: {}", managerId);

        List<ReportingHierarchy> hierarchies = reportingHierarchyService.getDirectReports(managerId);
        List<ReportingHierarchyResponse> response = hierarchies.stream()
            .map(ReportingHierarchyResponse::new)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get the complete reporting history for an employee.
     * Returns all past and current manager relationships.
     * 
     * GET /api/reporting-hierarchy/employee/{employeeId}/history
     * 
     * @param employeeId employee's user ID
     * @return list of all reporting relationships
     */
    @GetMapping("/employee/{employeeId}/history")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ReportingHierarchyResponse>>> getReportingHistory(
            @PathVariable Long employeeId) {
        
        logger.debug("Get reporting history request for employee: {}", employeeId);

        List<ReportingHierarchy> hierarchies = reportingHierarchyService.getReportingHistory(employeeId);
        List<ReportingHierarchyResponse> response = hierarchies.stream()
            .map(ReportingHierarchyResponse::new)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get the manager for an employee on a specific date (historical query).
     * 
     * GET /api/reporting-hierarchy/employee/{employeeId}/manager-on-date
     * 
     * @param employeeId employee's user ID
     * @param date date to check (format: yyyy-MM-dd)
     * @return reporting hierarchy active on that date, or null if none
     */
    @GetMapping("/employee/{employeeId}/manager-on-date")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReportingHierarchyResponse>> getManagerOnDate(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        logger.debug("Get manager on date request - employee: {}, date: {}", employeeId, date);

        ReportingHierarchy hierarchy = reportingHierarchyService.getManagerOnDate(employeeId, date);
        
        if (hierarchy == null) {
            return ResponseEntity.ok(ApiResponse.success("No manager found on specified date", null));
        }

        ReportingHierarchyResponse response = new ReportingHierarchyResponse(hierarchy);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get direct reports for a manager on a specific date (historical query).
     * 
     * GET /api/reporting-hierarchy/manager/{managerId}/direct-reports-on-date
     * 
     * @param managerId manager's user ID
     * @param date date to check (format: yyyy-MM-dd)
     * @return list of reporting relationships active on that date
     */
    @GetMapping("/manager/{managerId}/direct-reports-on-date")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ReportingHierarchyResponse>>> getDirectReportsOnDate(
            @PathVariable Long managerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        logger.debug("Get direct reports on date request - manager: {}, date: {}", managerId, date);

        List<ReportingHierarchy> hierarchies = reportingHierarchyService.getDirectReportsOnDate(managerId, date);
        List<ReportingHierarchyResponse> response = hierarchies.stream()
            .map(ReportingHierarchyResponse::new)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all currently active reporting relationships in the organization.
     * 
     * GET /api/reporting-hierarchy/active
     * 
     * @return list of all active reporting relationships
     */
    @GetMapping("/active")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ReportingHierarchyResponse>>> getAllActiveRelationships() {
        logger.debug("Get all active relationships request");

        List<ReportingHierarchy> hierarchies = reportingHierarchyService.getAllActiveRelationships();
        List<ReportingHierarchyResponse> response = hierarchies.stream()
            .map(ReportingHierarchyResponse::new)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
