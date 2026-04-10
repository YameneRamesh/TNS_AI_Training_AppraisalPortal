import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, ActivatedRoute } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { CycleTriggerComponent } from './cycle-trigger.component';
import { CycleService, TriggerCycleResult } from '../../../core/services/cycle.service';
import { UserService } from '../../../core/services/user.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { AppraisalCycle } from '../../../core/models/appraisal.model';
import { User } from '../../../core/models/user.model';

describe('CycleTriggerComponent', () => {
  let component: CycleTriggerComponent;
  let fixture: ComponentFixture<CycleTriggerComponent>;
  let cycleService: jasmine.SpyObj<CycleService>;
  let userService: jasmine.SpyObj<UserService>;
  let router: jasmine.SpyObj<Router>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;
  let dialog: jasmine.SpyObj<MatDialog>;

  const mockCycle: AppraisalCycle = {
    id: 1,
    name: 'Annual Appraisal 2025-26',
    startDate: '2025-04-01',
    endDate: '2026-03-31',
    templateId: 1,
    status: 'DRAFT',
    createdBy: 1,
    createdAt: '2025-03-01T10:00:00Z',
    updatedAt: '2025-03-01T10:00:00Z'
  };

  const mockUsers: User[] = [
    {
      id: 10,
      employeeId: 'EMP001',
      fullName: 'John Doe',
      email: 'john@example.com',
      designation: 'Software Engineer',
      department: 'Engineering',
      managerId: 5,
      managerName: 'Jane Manager',
      isActive: true,
      roles: [{ id: 1, name: 'EMPLOYEE' }],
      createdAt: '2024-01-01T10:00:00Z',
      updatedAt: '2024-01-01T10:00:00Z'
    },
    {
      id: 11,
      employeeId: 'EMP002',
      fullName: 'Alice Smith',
      email: 'alice@example.com',
      designation: 'Senior Engineer',
      department: 'Engineering',
      managerId: 5,
      managerName: 'Jane Manager',
      isActive: true,
      roles: [{ id: 1, name: 'EMPLOYEE' }],
      createdAt: '2024-01-01T10:00:00Z',
      updatedAt: '2024-01-01T10:00:00Z'
    },
    {
      id: 12,
      employeeId: 'EMP003',
      fullName: 'Bob Wilson',
      email: 'bob@example.com',
      designation: 'Sales Manager',
      department: 'Sales',
      managerId: 6,
      managerName: 'Tom Lead',
      isActive: true,
      roles: [{ id: 1, name: 'EMPLOYEE' }],
      createdAt: '2024-01-01T10:00:00Z',
      updatedAt: '2024-01-01T10:00:00Z'
    }
  ];

  beforeEach(async () => {
    const cycleServiceSpy = jasmine.createSpyObj('CycleService', ['getCycleById', 'triggerCycle']);
    const userServiceSpy = jasmine.createSpyObj('UserService', ['getUsers']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    const dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);

    await TestBed.configureTestingModule({
      imports: [CycleTriggerComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimations(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => key === 'id' ? '1' : null
              }
            }
          }
        },
        { provide: CycleService, useValue: cycleServiceSpy },
        { provide: UserService, useValue: userServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: MatDialog, useValue: dialogSpy }
      ]
    }).compileComponents();

    cycleService = TestBed.inject(CycleService) as jasmine.SpyObj<CycleService>;
    userService = TestBed.inject(UserService) as jasmine.SpyObj<UserService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    snackBar = TestBed.inject(MatSnackBar) as jasmine.SpyObj<MatSnackBar>;
    dialog = TestBed.inject(MatDialog) as jasmine.SpyObj<MatDialog>;

    fixture = TestBed.createComponent(CycleTriggerComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Component Initialization', () => {
    it('should load cycle and employees on init', () => {
      cycleService.getCycleById.and.returnValue(of({ data: mockCycle, message: 'Success' }));
      userService.getUsers.and.returnValue(of({ data: mockUsers, message: 'Success' }));

      fixture.detectChanges();

      expect(component.cycleId).toBe(1);
      expect(cycleService.getCycleById).toHaveBeenCalledWith(1);
      expect(userService.getUsers).toHaveBeenCalledWith({ isActive: true });
      expect(component.cycle).toEqual(mockCycle);
      expect(component.employees).toEqual(mockUsers);
      expect(component.filteredEmployees).toEqual(mockUsers);
      expect(component.loading).toBe(false);
    });

    it('should handle cycle loading error', () => {
      const error = { message: 'Network error' };
      cycleService.getCycleById.and.returnValue(throwError(() => error));
      userService.getUsers.and.returnValue(of({ data: mockUsers, message: 'Success' }));

      fixture.detectChanges();

      expect(snackBar.open).toHaveBeenCalledWith('Failed to load cycle details', 'Close', { duration: 3000 });
    });

    it('should handle employees loading error', () => {
      const error = { message: 'Network error' };
      cycleService.getCycleById.and.returnValue(of({ data: mockCycle, message: 'Success' }));
      userService.getUsers.and.returnValue(throwError(() => error));

      fixture.detectChanges();

      expect(component.loading).toBe(false);
      expect(snackBar.open).toHaveBeenCalledWith('Failed to load employees', 'Close', { duration: 3000 });
    });
  });

  describe('Employee Search and Filtering', () => {
    beforeEach(() => {
      cycleService.getCycleById.and.returnValue(of({ data: mockCycle, message: 'Success' }));
      userService.getUsers.and.returnValue(of({ data: mockUsers, message: 'Success' }));
      fixture.detectChanges();
    });

    it('should filter employees by name', () => {
      component.filterEmployees('john');
      expect(component.filteredEmployees.length).toBe(1);
      expect(component.filteredEmployees[0].fullName).toBe('John Doe');
    });

    it('should filter employees by employee ID', () => {
      component.filterEmployees('EMP001');
      expect(component.filteredEmployees.length).toBe(1);
      expect(component.filteredEmployees[0].employeeId).toBe('EMP001');
    });

    it('should filter employees by email', () => {
      component.filterEmployees('alice@');
      expect(component.filteredEmployees.length).toBe(1);
      expect(component.filteredEmployees[0].email).toBe('alice@example.com');
    });

    it('should filter employees by department', () => {
      component.filterEmployees('engineering');
      expect(component.filteredEmployees.length).toBe(2);
    });

    it('should filter employees by designation', () => {
      component.filterEmployees('senior');
      expect(component.filteredEmployees.length).toBe(1);
      expect(component.filteredEmployees[0].designation).toBe('Senior Engineer');
    });

    it('should return all employees when search is empty', () => {
      component.filterEmployees('');
      expect(component.filteredEmployees.length).toBe(3);
    });

    it('should be case insensitive', () => {
      component.filterEmployees('JOHN');
      expect(component.filteredEmployees.length).toBe(1);
    });
  });

  describe('Employee Selection', () => {
    beforeEach(() => {
      cycleService.getCycleById.and.returnValue(of({ data: mockCycle, message: 'Success' }));
      userService.getUsers.and.returnValue(of({ data: mockUsers, message: 'Success' }));
      fixture.detectChanges();
    });

    it('should select all employees', () => {
      component.toggleAllRows();
      expect(component.selection.selected.length).toBe(3);
      expect(component.isAllSelected()).toBe(true);
    });

    it('should deselect all employees', () => {
      component.selection.select(...mockUsers);
      component.toggleAllRows();
      expect(component.selection.selected.length).toBe(0);
      expect(component.isAllSelected()).toBe(false);
    });

    it('should detect indeterminate state', () => {
      component.selection.select(mockUsers[0]);
      expect(component.isIndeterminate()).toBe(true);
    });

    it('should not be indeterminate when all selected', () => {
      component.selection.select(...mockUsers);
      expect(component.isIndeterminate()).toBe(false);
    });

    it('should not be indeterminate when none selected', () => {
      expect(component.isIndeterminate()).toBe(false);
    });

    it('should return correct selection text', () => {
      expect(component.getSelectionText()).toBe('No employees selected');
      
      component.selection.select(mockUsers[0]);
      expect(component.getSelectionText()).toBe('1 employee(s) selected');
      
      component.selection.select(mockUsers[1]);
      expect(component.getSelectionText()).toBe('2 employee(s) selected');
    });
  });

  describe('Cycle Trigger', () => {
    beforeEach(() => {
      cycleService.getCycleById.and.returnValue(of({ data: mockCycle, message: 'Success' }));
      userService.getUsers.and.returnValue(of({ data: mockUsers, message: 'Success' }));
      fixture.detectChanges();
    });

    it('should show error when no employees selected', () => {
      component.onTrigger();
      expect(snackBar.open).toHaveBeenCalledWith('Please select at least one employee', 'Close', { duration: 3000 });
      expect(dialog.open).not.toHaveBeenCalled();
    });

    it('should open confirmation dialog when employees selected', () => {
      const dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['afterClosed']);
      dialogRefSpy.afterClosed.and.returnValue(of(false));
      dialog.open.and.returnValue(dialogRefSpy);

      component.selection.select(mockUsers[0]);
      component.onTrigger();

      expect(dialog.open).toHaveBeenCalled();
    });

    it('should trigger cycle successfully', () => {
      const dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['afterClosed']);
      dialogRefSpy.afterClosed.and.returnValue(of(true));
      dialog.open.and.returnValue(dialogRefSpy);

      const result: TriggerCycleResult = {
        successCount: 2,
        failureCount: 0,
        totalCount: 2,
        failures: []
      };
      cycleService.triggerCycle.and.returnValue(of({ data: result, message: 'Success' }));

      component.selection.select(mockUsers[0], mockUsers[1]);
      component.onTrigger();

      expect(cycleService.triggerCycle).toHaveBeenCalledWith(1, { employeeIds: [10, 11] });
      expect(snackBar.open).toHaveBeenCalledWith(
        'Cycle triggered successfully for 2 employee(s)',
        'Close',
        { duration: 5000 }
      );
      expect(router.navigate).toHaveBeenCalledWith(['/hr/cycles']);
    });

    it('should handle partial failure', (done) => {
      const dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['afterClosed']);
      dialogRefSpy.afterClosed.and.returnValue(of(true));
      dialog.open.and.returnValue(dialogRefSpy);

      const result: TriggerCycleResult = {
        successCount: 1,
        failureCount: 1,
        totalCount: 2,
        failures: [
          {
            employeeId: 11,
            employeeName: 'Alice Smith',
            reason: 'Already has active form'
          }
        ]
      };
      cycleService.triggerCycle.and.returnValue(of({ data: result, message: 'Success' }));

      component.selection.select(mockUsers[0], mockUsers[1]);
      component.onTrigger();

      setTimeout(() => {
        expect(snackBar.open).toHaveBeenCalled();
        expect(router.navigate).toHaveBeenCalledWith(['/hr/cycles']);
        done();
      }, 2100);
    });

    it('should handle trigger error', () => {
      const dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['afterClosed']);
      dialogRefSpy.afterClosed.and.returnValue(of(true));
      dialog.open.and.returnValue(dialogRefSpy);

      const error = { error: { message: 'Cycle already triggered' } };
      cycleService.triggerCycle.and.returnValue(throwError(() => error));

      component.selection.select(mockUsers[0]);
      component.onTrigger();

      expect(component.triggering).toBe(false);
      expect(snackBar.open).toHaveBeenCalledWith('Cycle already triggered', 'Close', { duration: 5000 });
    });

    it('should handle generic trigger error', () => {
      const dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['afterClosed']);
      dialogRefSpy.afterClosed.and.returnValue(of(true));
      dialog.open.and.returnValue(dialogRefSpy);

      const error = { message: 'Network error' };
      cycleService.triggerCycle.and.returnValue(throwError(() => error));

      component.selection.select(mockUsers[0]);
      component.onTrigger();

      expect(snackBar.open).toHaveBeenCalledWith('Failed to trigger cycle. Please try again.', 'Close', { duration: 5000 });
    });
  });

  describe('Navigation', () => {
    it('should navigate back on cancel', () => {
      component.onCancel();
      expect(router.navigate).toHaveBeenCalledWith(['/hr/cycles']);
    });
  });

  describe('Date Formatting', () => {
    it('should format date correctly', () => {
      const formatted = component.formatDate('2025-04-01');
      expect(formatted).toMatch(/Apr.*2025/);
    });
  });
});
