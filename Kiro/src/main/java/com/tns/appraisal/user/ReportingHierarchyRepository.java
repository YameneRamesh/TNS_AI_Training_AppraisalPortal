package com.tns.appraisal.user;

import com.tns.appraisal.common.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing ReportingHierarchy entities.
 * Provides query methods for accessing current and historical reporting relationships.
 */
@Repository
public interface ReportingHierarchyRepository extends BaseRepository<ReportingHierarchy, Long> {

    /**
     * Find the current active reporting relationship for an employee.
     * 
     * @param employee the employee
     * @return the current active reporting relationship, if any
     */
    @Query("SELECT rh FROM ReportingHierarchy rh WHERE rh.employee = :employee " +
           "AND (rh.endDate IS NULL OR rh.endDate > CURRENT_DATE) " +
           "ORDER BY rh.effectiveDate DESC")
    Optional<ReportingHierarchy> findCurrentByEmployee(@Param("employee") User employee);

    /**
     * Find the reporting relationship for an employee on a specific date.
     * 
     * @param employee the employee
     * @param date the date to check
     * @return the reporting relationship active on the given date, if any
     */
    @Query("SELECT rh FROM ReportingHierarchy rh WHERE rh.employee = :employee " +
           "AND rh.effectiveDate <= :date " +
           "AND (rh.endDate IS NULL OR rh.endDate >= :date) " +
           "ORDER BY rh.effectiveDate DESC")
    Optional<ReportingHierarchy> findByEmployeeAndDate(@Param("employee") User employee, 
                                                        @Param("date") LocalDate date);

    /**
     * Find all current direct reports for a manager.
     * 
     * @param manager the manager
     * @return list of current active reporting relationships where the user is the manager
     */
    @Query("SELECT rh FROM ReportingHierarchy rh WHERE rh.manager = :manager " +
           "AND (rh.endDate IS NULL OR rh.endDate > CURRENT_DATE)")
    List<ReportingHierarchy> findCurrentByManager(@Param("manager") User manager);

    /**
     * Find all direct reports for a manager on a specific date.
     * 
     * @param manager the manager
     * @param date the date to check
     * @return list of reporting relationships active on the given date
     */
    @Query("SELECT rh FROM ReportingHierarchy rh WHERE rh.manager = :manager " +
           "AND rh.effectiveDate <= :date " +
           "AND (rh.endDate IS NULL OR rh.endDate >= :date)")
    List<ReportingHierarchy> findByManagerAndDate(@Param("manager") User manager, 
                                                   @Param("date") LocalDate date);

    /**
     * Find all reporting relationships for an employee (historical and current).
     * 
     * @param employee the employee
     * @return list of all reporting relationships for the employee, ordered by effective date descending
     */
    @Query("SELECT rh FROM ReportingHierarchy rh WHERE rh.employee = :employee " +
           "ORDER BY rh.effectiveDate DESC")
    List<ReportingHierarchy> findAllByEmployee(@Param("employee") User employee);

    /**
     * Find all reporting relationships managed by a manager (historical and current).
     * 
     * @param manager the manager
     * @return list of all reporting relationships managed by the manager, ordered by effective date descending
     */
    @Query("SELECT rh FROM ReportingHierarchy rh WHERE rh.manager = :manager " +
           "ORDER BY rh.effectiveDate DESC")
    List<ReportingHierarchy> findAllByManager(@Param("manager") User manager);

    /**
     * Check if an employee reports to a specific manager currently.
     * 
     * @param employee the employee
     * @param manager the manager
     * @return true if the employee currently reports to the manager
     */
    @Query("SELECT COUNT(rh) > 0 FROM ReportingHierarchy rh WHERE rh.employee = :employee " +
           "AND rh.manager = :manager " +
           "AND (rh.endDate IS NULL OR rh.endDate > CURRENT_DATE)")
    boolean existsCurrentRelationship(@Param("employee") User employee, 
                                      @Param("manager") User manager);

    /**
     * Find all active reporting relationships (no end date or end date in the future).
     * 
     * @return list of all currently active reporting relationships
     */
    @Query("SELECT rh FROM ReportingHierarchy rh WHERE rh.endDate IS NULL OR rh.endDate > CURRENT_DATE " +
           "ORDER BY rh.employee.fullName")
    List<ReportingHierarchy> findAllActive();
}
