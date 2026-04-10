package com.tns.appraisal.user;

import com.tns.appraisal.audit.AuditLogService;
import com.tns.appraisal.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReportingHierarchyService.
 */
@ExtendWith(MockitoExtension.class)
class ReportingHierarchyServiceTest {

    @Mock
    private ReportingHierarchyRepository reportingHierarchyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ReportingHierarchyService reportingHierarchyService;

    private User employee;
    private User manager;
    private User newManager;
    private ReportingHierarchy hierarchy;

    @BeforeEach
    void setUp() {
        employee = new User("EMP001", "John Doe", "john@example.com", "hash");
        employee.setId(1L);

        manager = new User("MGR001", "Jane Manager", "jane@example.com", "hash");
        manager.setId(2L);

        newManager = new User("MGR002", "Bob NewManager", "bob@example.com", "hash");
        newManager.setId(3L);

        hierarchy = new ReportingHierarchy(employee, manager, LocalDate.now());
        hierarchy.setId(100L);
    }

    @Test
    void assignManager_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(reportingHierarchyRepository.findCurrentByEmployee(employee)).thenReturn(Optional.empty());
        when(reportingHierarchyRepository.save(any(ReportingHierarchy.class))).thenReturn(hierarchy);
        when(userRepository.save(any(User.class))).thenReturn(employee);

        // Act
        ReportingHierarchy result = reportingHierarchyService.assignManager(1L, 2L, LocalDate.now(), 999L);

        // Assert
        assertNotNull(result);
        assertEquals(100L, result.getId());
        verify(reportingHierarchyRepository).save(any(ReportingHierarchy.class));
        verify(userRepository).save(employee);
        verify(auditLogService).logAsync(eq(999L), eq("MANAGER_ASSIGNED"), eq("ReportingHierarchy"), 
            eq(100L), any(), eq(null));
    }

    @Test
    void assignManager_EmployeeAlreadyHasManager_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(reportingHierarchyRepository.findCurrentByEmployee(employee)).thenReturn(Optional.of(hierarchy));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            reportingHierarchyService.assignManager(1L, 2L, LocalDate.now(), 999L);
        });

        assertTrue(exception.getMessage().contains("already has an active manager"));
        verify(reportingHierarchyRepository, never()).save(any());
    }

    @Test
    void assignManager_EmployeeNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            reportingHierarchyService.assignManager(1L, 2L, LocalDate.now(), 999L);
        });

        assertTrue(exception.getMessage().contains("Employee with ID 1 not found"));
    }

    @Test
    void assignManager_SameEmployeeAndManager_ThrowsException() {
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            reportingHierarchyService.assignManager(1L, 1L, LocalDate.now(), 999L);
        });

        assertTrue(exception.getMessage().contains("cannot be their own manager"));
    }

    @Test
    void changeManager_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.findById(3L)).thenReturn(Optional.of(newManager));
        when(reportingHierarchyRepository.findCurrentByEmployee(employee)).thenReturn(Optional.of(hierarchy));
        
        ReportingHierarchy newHierarchy = new ReportingHierarchy(employee, newManager, LocalDate.now());
        newHierarchy.setId(101L);
        when(reportingHierarchyRepository.save(any(ReportingHierarchy.class))).thenReturn(newHierarchy);
        when(userRepository.save(any(User.class))).thenReturn(employee);

        // Act
        ReportingHierarchy result = reportingHierarchyService.changeManager(1L, 3L, LocalDate.now(), 999L);

        // Assert
        assertNotNull(result);
        assertNotNull(hierarchy.getEndDate());
        verify(reportingHierarchyRepository, times(2)).save(any(ReportingHierarchy.class));
        verify(userRepository).save(employee);
        verify(auditLogService).logAsync(eq(999L), eq("MANAGER_CHANGED"), eq("ReportingHierarchy"), 
            eq(101L), any(), eq(null));
    }

    @Test
    void changeManager_NoCurrentManager_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.findById(3L)).thenReturn(Optional.of(newManager));
        when(reportingHierarchyRepository.findCurrentByEmployee(employee)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            reportingHierarchyService.changeManager(1L, 3L, LocalDate.now(), 999L);
        });

        assertTrue(exception.getMessage().contains("does not have an active manager"));
    }

    @Test
    void getCurrentManager_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(reportingHierarchyRepository.findCurrentByEmployee(employee)).thenReturn(Optional.of(hierarchy));

        // Act
        ReportingHierarchy result = reportingHierarchyService.getCurrentManager(1L);

        // Assert
        assertNotNull(result);
        assertEquals(hierarchy.getId(), result.getId());
    }

    @Test
    void getCurrentManager_NoManager_ReturnsNull() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(reportingHierarchyRepository.findCurrentByEmployee(employee)).thenReturn(Optional.empty());

        // Act
        ReportingHierarchy result = reportingHierarchyService.getCurrentManager(1L);

        // Assert
        assertNull(result);
    }

    @Test
    void getDirectReports_Success() {
        // Arrange
        ReportingHierarchy hierarchy2 = new ReportingHierarchy(employee, manager, LocalDate.now());
        List<ReportingHierarchy> hierarchies = Arrays.asList(hierarchy, hierarchy2);
        
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(reportingHierarchyRepository.findCurrentByManager(manager)).thenReturn(hierarchies);

        // Act
        List<ReportingHierarchy> result = reportingHierarchyService.getDirectReports(2L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void getReportingHistory_Success() {
        // Arrange
        ReportingHierarchy oldHierarchy = new ReportingHierarchy(employee, manager, LocalDate.now().minusYears(1));
        oldHierarchy.setEndDate(LocalDate.now().minusMonths(1));
        List<ReportingHierarchy> hierarchies = Arrays.asList(hierarchy, oldHierarchy);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(reportingHierarchyRepository.findAllByEmployee(employee)).thenReturn(hierarchies);

        // Act
        List<ReportingHierarchy> result = reportingHierarchyService.getReportingHistory(1L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void endReportingRelationship_Success() {
        // Arrange
        LocalDate endDate = LocalDate.now();
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(reportingHierarchyRepository.findCurrentByEmployee(employee)).thenReturn(Optional.of(hierarchy));
        when(reportingHierarchyRepository.save(any(ReportingHierarchy.class))).thenReturn(hierarchy);
        when(userRepository.save(any(User.class))).thenReturn(employee);

        // Act
        reportingHierarchyService.endReportingRelationship(1L, endDate, 999L);

        // Assert
        assertEquals(endDate, hierarchy.getEndDate());
        verify(reportingHierarchyRepository).save(hierarchy);
        verify(userRepository).save(employee);
        verify(auditLogService).logAsync(eq(999L), eq("REPORTING_RELATIONSHIP_ENDED"), 
            eq("ReportingHierarchy"), eq(100L), any(), eq(null));
    }

    @Test
    void endReportingRelationship_NoActiveRelationship_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(reportingHierarchyRepository.findCurrentByEmployee(employee)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            reportingHierarchyService.endReportingRelationship(1L, LocalDate.now(), 999L);
        });

        assertTrue(exception.getMessage().contains("does not have an active manager"));
    }

    @Test
    void getAllActiveRelationships_Success() {
        // Arrange
        List<ReportingHierarchy> hierarchies = Arrays.asList(hierarchy);
        when(reportingHierarchyRepository.findAllActive()).thenReturn(hierarchies);

        // Act
        List<ReportingHierarchy> result = reportingHierarchyService.getAllActiveRelationships();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
