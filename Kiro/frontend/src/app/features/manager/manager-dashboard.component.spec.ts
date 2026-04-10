import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { ManagerDashboardComponent } from './manager-dashboard.component';
import { DashboardService } from '../../core/services/dashboard.service';
import { AuthService } from '../../core/services/auth.service';
import { ManagerDashboard } from '../../core/models/dashboard.model';
import { User } from '../../core/models/user.model';
import { provideZonelessChangeDetection } from '@angular/core';

describe('ManagerDashboardComponent', () => {
  let component: ManagerDashboardComponent;
  let fixture: ComponentFixture<ManagerDashboardComponent>;
  let mockDashboardService: { getManagerDashboard: ReturnType<typeof vi.fn> };
  let mockAuthService: { currentUserValue: User };
  let mockRouter: { navigate: ReturnType<typeof vi.fn> };

  const mockUser: User = {
    id: 2,
    employeeId: 'MGR001',
    fullName: 'Jane Manager',
    email: 'jane@example.com',
    designation: 'Engineering Manager',
    department: 'Engineering',
    roles: [{ id: 1, name: 'EMPLOYEE' }, { id: 2, name: 'MANAGER' }],
    isActive: true,
    createdAt: '2020-01-01T00:00:00Z',
    updatedAt: '2020-01-01T00:00:00Z'
  };

  const mockDashboard: ManagerDashboard = {
    ownForm: {
      id: 10,
      cycleName: '2025-26',
      status: 'NOT_STARTED',
      pdfAvailable: false
    },
    teamForms: [
      {
        formId: 1,
        employeeId: 101,
        employeeName: 'Alice Smith',
        designation: 'Developer',
        status: 'SUBMITTED',
        submittedAt: '2025-04-10T09:00:00Z'
      },
      {
        formId: 2,
        employeeId: 102,
        employeeName: 'Bob Jones',
        designation: 'QA Engineer',
        status: 'NOT_STARTED'
      }
    ],
    pendingReviews: 1,
    completedReviews: 0,
    completionPercentage: 0,
    historicalForms: [
      {
        id: 5,
        cycleName: '2024-25',
        status: 'REVIEWED_AND_COMPLETED',
        reviewedAt: '2024-05-01T14:00:00Z',
        pdfAvailable: true
      }
    ]
  };

  beforeEach(async () => {
    mockDashboardService = { getManagerDashboard: vi.fn() };
    mockAuthService = { currentUserValue: mockUser };
    mockRouter = { navigate: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [ManagerDashboardComponent],
      providers: [
        provideZonelessChangeDetection(),
        { provide: DashboardService, useValue: mockDashboardService },
        { provide: AuthService, useValue: mockAuthService },
        { provide: Router, useValue: mockRouter }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ManagerDashboardComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load dashboard data on init', () => {
    mockDashboardService.getManagerDashboard.mockReturnValue(of(mockDashboard));

    fixture.detectChanges();

    expect(mockDashboardService.getManagerDashboard).toHaveBeenCalled();
    expect(component.dashboard).toEqual(mockDashboard);
    expect(component.loading).toBe(false);
    expect(component.error).toBeNull();
  });

  it('should handle dashboard load error', () => {
    mockDashboardService.getManagerDashboard.mockReturnValue(
      throwError(() => new Error('API Error'))
    );

    fixture.detectChanges();

    expect(component.error).toBeTruthy();
    expect(component.loading).toBe(false);
  });

  // Req 8.3 — pending and completed review counts
  it('should expose pending and completed review counts', () => {
    mockDashboardService.getManagerDashboard.mockReturnValue(of(mockDashboard));
    fixture.detectChanges();

    expect(component.dashboard!.pendingReviews).toBe(1);
    expect(component.dashboard!.completedReviews).toBe(0);
  });

  // Req 8.4 — completion percentage
  it('should expose team completion percentage', () => {
    mockDashboardService.getManagerDashboard.mockReturnValue(of(mockDashboard));
    fixture.detectChanges();

    expect(component.dashboard!.completionPercentage).toBe(0);
  });

  // Req 8.2 — team forms list
  it('should expose team member forms', () => {
    mockDashboardService.getManagerDashboard.mockReturnValue(of(mockDashboard));
    fixture.detectChanges();

    expect(component.dashboard!.teamForms.length).toBe(2);
    expect(component.dashboard!.teamForms[0].employeeName).toBe('Alice Smith');
  });

  // Req 8.1 — own form
  it('should expose manager own form', () => {
    mockDashboardService.getManagerDashboard.mockReturnValue(of(mockDashboard));
    fixture.detectChanges();

    expect(component.dashboard!.ownForm).toBeDefined();
    expect(component.dashboard!.ownForm!.cycleName).toBe('2025-26');
  });

  // Req 8.6 — historical forms
  it('should expose historical appraisal forms', () => {
    mockDashboardService.getManagerDashboard.mockReturnValue(of(mockDashboard));
    fixture.detectChanges();

    expect(component.dashboard!.historicalForms.length).toBe(1);
    expect(component.dashboard!.historicalForms[0].cycleName).toBe('2024-25');
  });

  it('should correctly identify forms that can be reviewed', () => {
    expect(component.canReviewForm('SUBMITTED')).toBe(true);
    expect(component.canReviewForm('UNDER_REVIEW')).toBe(true);
    expect(component.canReviewForm('REVIEW_DRAFT_SAVED')).toBe(true);
    expect(component.canReviewForm('NOT_STARTED')).toBe(false);
    expect(component.canReviewForm('REVIEWED_AND_COMPLETED')).toBe(false);
  });

  it('should correctly identify own forms that can be edited', () => {
    expect(component.canEditOwnForm('NOT_STARTED')).toBe(true);
    expect(component.canEditOwnForm('DRAFT_SAVED')).toBe(true);
    expect(component.canEditOwnForm('SUBMITTED')).toBe(false);
  });

  // Req 8.5 — navigate to team member form
  it('should navigate to review form for team member', () => {
    component.reviewForm(1);
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/manager/review', 1]);
  });

  it('should navigate to team member form in readonly mode', () => {
    component.viewTeamMemberForm(2);
    expect(mockRouter.navigate).toHaveBeenCalledWith(
      ['/manager/review', 2],
      { queryParams: { readonly: true } }
    );
  });

  it('should navigate to own form for editing', () => {
    component.openOwnForm(10);
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/manager/appraisal', 10]);
  });

  it('should navigate to own form in readonly mode', () => {
    component.viewOwnForm(10);
    expect(mockRouter.navigate).toHaveBeenCalledWith(
      ['/manager/appraisal', 10],
      { queryParams: { readonly: true } }
    );
  });

  it('should return correct status labels', () => {
    expect(component.getStatusLabel('NOT_STARTED')).toBe('Not Started');
    expect(component.getStatusLabel('SUBMITTED')).toBe('Submitted');
    expect(component.getStatusLabel('REVIEWED_AND_COMPLETED')).toBe('Completed');
  });

  it('should return correct status CSS classes', () => {
    expect(component.getStatusClass('NOT_STARTED')).toBe('status-not-started');
    expect(component.getStatusClass('SUBMITTED')).toBe('status-submitted');
    expect(component.getStatusClass('REVIEWED_AND_COMPLETED')).toBe('status-completed');
  });
});
