package com.tns.appraisal.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReportingHierarchyRepository.
 */
@DataJpaTest
class ReportingHierarchyRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ReportingHierarchyRepository reportingHierarchyRepository;

    private User manager;
    private User employee1;
    private User employee2;

    @BeforeEach
    void setUp() {
        // Create test users
        manager = new User("MGR001", "John Manager", "john.manager@tns.com", "hash123");
        manager.setDesignation("Senior Manager");
        manager.setDepartment("Engineering");
        manager.setIsActive(true);
        entityManager.persist(manager);

        employee1 = new User("EMP001", "Alice Employee", "alice@tns.com", "hash123");
        employee1.setDesignation("Software Engineer");
        employee1.setDepartment("Engineering");
        employee1.setManager(manager);
        employee1.setIsActive(true);
        entityManager.persist(employee1);

        employee2 = new User("EMP002", "Bob Employee", "bob@tns.com", "hash123");
        employee2.setDesignation("Software Engineer");
        employee2.setDepartment("Engineering");
        employee2.setManager(manager);
        employee2.setIsActive(true);
        entityManager.persist(employee2);

        entityManager.flush();
    }

    @Test
    void testCreateReportingHierarchy() {
        // Given
        ReportingHierarchy hierarchy = new ReportingHierarchy(
            employee1,
            manager,
            LocalDate.now()
        );

        // When
        ReportingHierarchy saved = reportingHierarchyRepository.save(hierarchy);
        entityManager.flush();

        // Then
        assertNotNull(saved.getId());
        assertEquals(employee1.getId(), saved.getEmployee().getId());
        assertEquals(manager.getId(), saved.getManager().getId());
        assertNotNull(saved.getEffectiveDate());
        assertNull(saved.getEndDate());
        assertTrue(saved.isActive());
    }

    @Test
    void testFindCurrentByEmployee() {
        // Given
        ReportingHierarchy hierarchy = new ReportingHierarchy(
            employee1,
            manager,
            LocalDate.now().minusMonths(6)
        );
        reportingHierarchyRepository.save(hierarchy);
        entityManager.flush();

        // When
        Optional<ReportingHierarchy> found = reportingHierarchyRepository.findCurrentByEmployee(employee1);

        // Then
        assertTrue(found.isPresent());
        assertEquals(employee1.getId(), found.get().getEmployee().getId());
        assertEquals(manager.getId(), found.get().getManager().getId());
    }

    @Test
    void testFindCurrentByManager() {
        // Given
        ReportingHierarchy hierarchy1 = new ReportingHierarchy(
            employee1,
            manager,
            LocalDate.now().minusMonths(6)
        );
        ReportingHierarchy hierarchy2 = new ReportingHierarchy(
            employee2,
            manager,
            LocalDate.now().minusMonths(3)
        );
        reportingHierarchyRepository.save(hierarchy1);
        reportingHierarchyRepository.save(hierarchy2);
        entityManager.flush();

        // When
        List<ReportingHierarchy> directReports = reportingHierarchyRepository.findCurrentByManager(manager);

        // Then
        assertEquals(2, directReports.size());
        assertTrue(directReports.stream()
            .allMatch(rh -> rh.getManager().getId().equals(manager.getId())));
    }

    @Test
    void testFindByEmployeeAndDate() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 6, 30);
        
        ReportingHierarchy hierarchy = new ReportingHierarchy(
            employee1,
            manager,
            startDate
        );
        hierarchy.setEndDate(endDate);
        reportingHierarchyRepository.save(hierarchy);
        entityManager.flush();

        // When - query within the active period
        Optional<ReportingHierarchy> found = reportingHierarchyRepository.findByEmployeeAndDate(
            employee1,
            LocalDate.of(2024, 3, 15)
        );

        // Then
        assertTrue(found.isPresent());
        assertEquals(employee1.getId(), found.get().getEmployee().getId());

        // When - query after the end date
        Optional<ReportingHierarchy> notFound = reportingHierarchyRepository.findByEmployeeAndDate(
            employee1,
            LocalDate.of(2024, 7, 1)
        );

        // Then
        assertFalse(notFound.isPresent());
    }

    @Test
    void testExistsCurrentRelationship() {
        // Given
        ReportingHierarchy hierarchy = new ReportingHierarchy(
            employee1,
            manager,
            LocalDate.now().minusMonths(6)
        );
        reportingHierarchyRepository.save(hierarchy);
        entityManager.flush();

        // When
        boolean exists = reportingHierarchyRepository.existsCurrentRelationship(employee1, manager);

        // Then
        assertTrue(exists);
    }

    @Test
    void testFindAllActive() {
        // Given
        ReportingHierarchy active1 = new ReportingHierarchy(
            employee1,
            manager,
            LocalDate.now().minusMonths(6)
        );
        
        ReportingHierarchy active2 = new ReportingHierarchy(
            employee2,
            manager,
            LocalDate.now().minusMonths(3)
        );
        
        reportingHierarchyRepository.save(active1);
        reportingHierarchyRepository.save(active2);
        entityManager.flush();

        // When
        List<ReportingHierarchy> activeHierarchies = reportingHierarchyRepository.findAllActive();

        // Then
        assertEquals(2, activeHierarchies.size());
        assertTrue(activeHierarchies.stream().allMatch(ReportingHierarchy::isActive));
    }

    @Test
    void testIsActiveOn() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);
        
        ReportingHierarchy hierarchy = new ReportingHierarchy(
            employee1,
            manager,
            startDate
        );
        hierarchy.setEndDate(endDate);

        // When/Then
        assertTrue(hierarchy.isActiveOn(LocalDate.of(2024, 6, 15)));
        assertTrue(hierarchy.isActiveOn(startDate));
        assertTrue(hierarchy.isActiveOn(endDate));
        assertFalse(hierarchy.isActiveOn(LocalDate.of(2023, 12, 31)));
        assertFalse(hierarchy.isActiveOn(LocalDate.of(2025, 1, 1)));
    }

    @Test
    void testHistoricalTracking() {
        // Given - employee had a previous manager
        User previousManager = new User("MGR002", "Jane Previous", "jane.previous@tns.com", "hash123");
        previousManager.setDesignation("Manager");
        previousManager.setDepartment("Engineering");
        previousManager.setIsActive(true);
        entityManager.persist(previousManager);

        ReportingHierarchy oldHierarchy = new ReportingHierarchy(
            employee1,
            previousManager,
            LocalDate.of(2023, 1, 1)
        );
        oldHierarchy.setEndDate(LocalDate.of(2023, 12, 31));
        
        ReportingHierarchy currentHierarchy = new ReportingHierarchy(
            employee1,
            manager,
            LocalDate.of(2024, 1, 1)
        );
        
        reportingHierarchyRepository.save(oldHierarchy);
        reportingHierarchyRepository.save(currentHierarchy);
        entityManager.flush();

        // When
        List<ReportingHierarchy> allHistory = reportingHierarchyRepository.findAllByEmployee(employee1);

        // Then
        assertEquals(2, allHistory.size());
        
        // Verify current relationship
        Optional<ReportingHierarchy> current = reportingHierarchyRepository.findCurrentByEmployee(employee1);
        assertTrue(current.isPresent());
        assertEquals(manager.getId(), current.get().getManager().getId());
        
        // Verify historical relationship
        Optional<ReportingHierarchy> historical = reportingHierarchyRepository.findByEmployeeAndDate(
            employee1,
            LocalDate.of(2023, 6, 15)
        );
        assertTrue(historical.isPresent());
        assertEquals(previousManager.getId(), historical.get().getManager().getId());
    }
}
