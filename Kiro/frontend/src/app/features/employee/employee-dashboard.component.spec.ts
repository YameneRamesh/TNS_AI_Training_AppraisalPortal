import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { EmployeeDashboardComponent } from './employee-dashboard.component';
import { DashboardService } from '../../core/services/dashboard.service';
import { AuthService } from '../../core/services/auth.service';
import { EmployeeDashboard } from '../../core/models/dashboard.model';
import { User } from '../../core/models/user.model';
import { provideZonelessChangeDetection } from '@angular/core';

describe('EmployeeDashboardComponent', () => {
  let component: EmployeeDashboardComponent;
  let fixture: ComponentFixture<EmployeeDashboardComponent>;
  let mockDashboardService: { getEmployeeDashboard: ReturnType<typeof vi.fn> };
  let mockAuthService: { currentUserValue: User };
  let mockRouter: { navigate: ReturnType<typeof vi.fn> };

  const mockUser: User = {
    id: 1,
    employeeId: 'EMP001',
    fullName: 'John Doe',
    email: 'john@example.com',
    designation: 'Developer',
    department: 'Engineering',
    managerId: 2,
    managerName: 'Jane Manager',
    roles: [{ id: 1, name: 'EMPLOYEE' }],
    isActive: true,
    createdAt: '',
    updatedAt: ''
  };

  const mockDashboard: EmployeeDashboard = {
    currentForm: {
      id: 1,
      cycleName: '2025-26',
      status: 'NOT_STARTED',
      pdfAvailable: false
    },
    historicalForms: [
      {
        id: 2,
        cycleName: '2024-25',
        status: 'REVIEWED_AND_COMPLETED',
        submittedAt: '2024-04-15T10:00:00Z',
        reviewedAt: '2024-05-01T14:30:00Z',
        pdfAvailable: true
      }
    ]
  };

  beforeEach(async () => {
    mockDashboardService = { getEmployeeDashboard: vi.fn() };
    mockAuthService = { currentUserValue: mockUser };
    mockRouter = { navigate: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [EmployeeDashboardComponent],
      providers: [
        provideZonelessChangeDetection(),
        { provide: DashboardService, useValue: mockDashboardService },
        { provide: AuthService, useValue: mockAuthService },
        { provide: Router, useValue: mockRouter }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(EmployeeDashboardComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load dashboard data on init', () => {
    mockDashboardService.getEmployeeDashboard.mockReturnValue(of(mockDashboard));

    fixture.detectChanges();

    expect(mockDashboardService.getEmployeeDashboard).toHaveBeenCalled();
    expect(component.dashboard).toEqual(mockDashboard);
    expect(component.loading).toBe(false);
  });

  it('should handle dashboard load error', () => {
    mockDashboardService.getEmployeeDashboard.mockReturnValue(
      throwError(() => new Error('API Error'))
    );

    fixture.detectChanges();

    expect(component.error).toBeTruthy();
    expect(component.loading).toBe(false);
  });

  it('should determine if form can be edited', () => {
    expect(component.canEditForm('NOT_STARTED')).toBe(true);
    expect(component.canEditForm('DRAFT_SAVED')).toBe(true);
    expect(component.canEditForm('SUBMITTED')).toBe(false);
    expect(component.canEditForm('REVIEWED_AND_COMPLETED')).toBe(false);
  });

  it('should navigate to form when opening', () => {
    component.openForm(1);
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/employee/appraisal', 1]);
  });

  it('should navigate to readonly form when viewing', () => {
    component.viewForm(2);
    expect(mockRouter.navigate).toHaveBeenCalledWith(
      ['/employee/appraisal', 2],
      { queryParams: { readonly: true } }
    );
  });
});
