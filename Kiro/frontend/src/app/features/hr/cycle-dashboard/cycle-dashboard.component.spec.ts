import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { CycleDashboardComponent } from './cycle-dashboard.component';
import { DashboardService } from '../../../core/services/dashboard.service';
import { CycleService } from '../../../core/services/cycle.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { HRDashboard } from '../../../core/models/dashboard.model';
import { AppraisalCycle } from '../../../core/models/appraisal.model';

describe('CycleDashboardComponent', () => {
  let component: CycleDashboardComponent;
  let fixture: ComponentFixture<CycleDashboardComponent>;
  let dashboardService: jasmine.SpyObj<DashboardService>;
  let cycleService: jasmine.SpyObj<CycleService>;
  let router: jasmine.SpyObj<Router>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;

  const mockHRDashboard: HRDashboard = {
    activeCycle: {
      id: 1,
      name: 'Annual Appraisal 2025-26',
      startDate: '2025-04-01',
      endDate: '2026-03-31',
      status: 'ACTIVE'
    },
    eligibleEmployees: 100,
    pendingSubmissions: 30,
    pendingReviews: 20,
    completedAppraisals: 50,
    departmentProgress: [
      {
        department: 'Engineering',
        totalEmployees: 50,
        completedAppraisals: 30,
        completionPercentage: 60
      },
      {
        department: 'Sales',
        totalEmployees: 30,
        completedAppraisals: 15,
        completionPercentage: 50
      }
    ]
  };

  const mockCycles: AppraisalCycle[] = [
    {
      id: 1,
      name: 'Annual Appraisal 2025-26',
      startDate: '2025-04-01',
      endDate: '2026-03-31',
      templateId: 1,
      status: 'ACTIVE',
      createdBy: 1,
      createdAt: '2025-03-01T10:00:00Z',
      updatedAt: '2025-03-01T10:00:00Z'
    },
    {
      id: 2,
      name: 'Annual Appraisal 2024-25',
      startDate: '2024-04-01',
      endDate: '2025-03-31',
      templateId: 1,
      status: 'CLOSED',
      createdBy: 1,
      createdAt: '2024-03-01T10:00:00Z',
      updatedAt: '2025-04-01T10:00:00Z'
    }
  ];

  beforeEach(async () => {
    const dashboardServiceSpy = jasmine.createSpyObj('DashboardService', ['getHRDashboard']);
    const cycleServiceSpy = jasmine.createSpyObj('CycleService', ['getCycles']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      imports: [CycleDashboardComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimations(),
        { provide: DashboardService, useValue: dashboardServiceSpy },
        { provide: CycleService, useValue: cycleServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: MatSnackBar, useValue: snackBarSpy }
      ]
    }).compileComponents();

    dashboardService = TestBed.inject(DashboardService) as jasmine.SpyObj<DashboardService>;
    cycleService = TestBed.inject(CycleService) as jasmine.SpyObj<CycleService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    snackBar = TestBed.inject(MatSnackBar) as jasmine.SpyObj<MatSnackBar>;

    fixture = TestBed.createComponent(CycleDashboardComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Component Initialization', () => {
    it('should load dashboard data and cycles on init', () => {
      dashboardService.getHRDashboard.and.returnValue(of({ data: mockHRDashboard, message: 'Success' }));
      cycleService.getCycles.and.returnValue(of({ data: mockCycles, message: 'Success' }));

      fixture.detectChanges();

      expect(dashboardService.getHRDashboard).toHaveBeenCalled();
      expect(cycleService.getCycles).toHaveBeenCalled();
      expect(component.dashboardData).toEqual(mockHRDashboard);
      expect(component.cycles).toEqual(mockCycles);
      expect(component.loading).toBe(false);
    });

    it('should handle null dashboard data', () => {
      dashboardService.getHRDashboard.and.returnValue(of({ data: null, message: 'Success' }));
      cycleService.getCycles.and.returnValue(of({ data: mockCycles, message: 'Success' }));

      fixture.detectChanges();

      expect(component.dashboardData).toBeNull();
      expect(component.loading).toBe(false);
    });
  });

  describe('Error Handling', () => {
    it('should handle dashboard loading error', () => {
      const error = { message: 'Network error' };
      dashboardService.getHRDashboard.and.returnValue(throwError(() => error));
      cycleService.getCycles.and.returnValue(of({ data: mockCycles, message: 'Success' }));

      fixture.detectChanges();

      expect(component.error).toBe('Failed to load dashboard data. Please try again.');
      expect(component.loading).toBe(false);
    });

    it('should handle cycles loading error', () => {
      const error = { message: 'Network error' };
      dashboardService.getHRDashboard.and.returnValue(of({ data: mockHRDashboard, message: 'Success' }));
      cycleService.getCycles.and.returnValue(throwError(() => error));

      fixture.detectChanges();

      expect(snackBar.open).toHaveBeenCalledWith('Failed to load cycles', 'Close', { duration: 3000 });
    });
  });

  describe('Navigation', () => {
    beforeEach(() => {
      dashboardService.getHRDashboard.and.returnValue(of({ data: mockHRDashboard, message: 'Success' }));
      cycleService.getCycles.and.returnValue(of({ data: mockCycles, message: 'Success' }));
      fixture.detectChanges();
    });

    it('should navigate to create cycle page', () => {
      component.createCycle();
      expect(router.navigate).toHaveBeenCalledWith(['/hr/cycles/create']);
    });

    it('should navigate to cycle details', () => {
      const cycle = mockCycles[0];
      component.viewCycle(cycle);
      expect(router.navigate).toHaveBeenCalledWith(['/hr/cycles', cycle.id]);
    });

    it('should navigate to trigger cycle page', () => {
      const cycle = mockCycles[0];
      component.triggerCycle(cycle);
      expect(router.navigate).toHaveBeenCalledWith(['/hr/cycles', cycle.id, 'trigger']);
    });
  });

  describe('Status Chip Colors', () => {
    it('should return primary color for ACTIVE status', () => {
      expect(component.getStatusColor('ACTIVE')).toBe('primary');
    });

    it('should return accent color for CLOSED status', () => {
      expect(component.getStatusColor('CLOSED')).toBe('accent');
    });

    it('should return warn color for DRAFT status', () => {
      expect(component.getStatusColor('DRAFT')).toBe('warn');
    });

    it('should return warn color for unknown status', () => {
      expect(component.getStatusColor('UNKNOWN')).toBe('warn');
    });
  });

  describe('Completion Colors', () => {
    it('should return primary color for completion >= 80%', () => {
      expect(component.getCompletionColor(85)).toBe('primary');
      expect(component.getCompletionColor(100)).toBe('primary');
    });

    it('should return accent color for completion >= 50% and < 80%', () => {
      expect(component.getCompletionColor(50)).toBe('accent');
      expect(component.getCompletionColor(75)).toBe('accent');
    });

    it('should return warn color for completion < 50%', () => {
      expect(component.getCompletionColor(30)).toBe('warn');
      expect(component.getCompletionColor(0)).toBe('warn');
    });
  });

  describe('Date Formatting', () => {
    it('should format date correctly', () => {
      const formatted = component.formatDate('2025-04-01');
      expect(formatted).toMatch(/Apr.*2025/);
    });
  });

  describe('Data Binding', () => {
    it('should display department progress data', () => {
      dashboardService.getHRDashboard.and.returnValue(of({ data: mockHRDashboard, message: 'Success' }));
      cycleService.getCycles.and.returnValue(of({ data: mockCycles, message: 'Success' }));

      fixture.detectChanges();

      expect(component.dashboardData?.departmentProgress.length).toBe(2);
      expect(component.dashboardData?.departmentProgress[0].department).toBe('Engineering');
    });

    it('should display cycle list data', () => {
      dashboardService.getHRDashboard.and.returnValue(of({ data: mockHRDashboard, message: 'Success' }));
      cycleService.getCycles.and.returnValue(of({ data: mockCycles, message: 'Success' }));

      fixture.detectChanges();

      expect(component.cycles.length).toBe(2);
      expect(component.cycles[0].name).toBe('Annual Appraisal 2025-26');
    });
  });
});
