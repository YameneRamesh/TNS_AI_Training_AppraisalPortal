import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, ActivatedRoute } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { CycleDetailsComponent } from './cycle-details.component';
import { CycleService } from '../../../core/services/cycle.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { AppraisalCycle, AppraisalForm } from '../../../core/models/appraisal.model';

describe('CycleDetailsComponent', () => {
  let component: CycleDetailsComponent;
  let fixture: ComponentFixture<CycleDetailsComponent>;
  let cycleService: jasmine.SpyObj<CycleService>;
  let router: jasmine.SpyObj<Router>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;
  let dialog: jasmine.SpyObj<MatDialog>;

  const mockCycle: AppraisalCycle = {
    id: 1,
    name: 'Annual Appraisal 2025-26',
    startDate: '2025-04-01',
    endDate: '2026-03-31',
    templateId: 1,
    status: 'ACTIVE',
    createdBy: 1,
    createdAt: '2025-03-01T10:00:00Z',
    updatedAt: '2025-03-01T10:00:00Z'
  };

  const mockForms: AppraisalForm[] = [
    {
      id: 1,
      cycleId: 1,
      employeeId: 10,
      employeeName: 'John Doe',
      managerId: 5,
      managerName: 'Jane Manager',
      templateId: 1,
      status: 'SUBMITTED',
      submittedAt: '2025-04-15T10:00:00Z',
      createdAt: '2025-04-01T10:00:00Z',
      updatedAt: '2025-04-15T10:00:00Z'
    },
    {
      id: 2,
      cycleId: 1,
      employeeId: 11,
      employeeName: 'Alice Smith',
      managerId: 5,
      managerName: 'Jane Manager',
      backupReviewerId: 6,
      backupReviewerName: 'Bob Backup',
      templateId: 1,
      status: 'REVIEWED_AND_COMPLETED',
      submittedAt: '2025-04-10T10:00:00Z',
      reviewedAt: '2025-04-20T10:00:00Z',
      createdAt: '2025-04-01T10:00:00Z',
      updatedAt: '2025-04-20T10:00:00Z'
    }
  ];

  beforeEach(async () => {
    const cycleServiceSpy = jasmine.createSpyObj('CycleService', [
      'getCycleById',
      'reopenForm',
      'assignBackupReviewer'
    ]);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    const dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);

    await TestBed.configureTestingModule({
      imports: [CycleDetailsComponent],
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
        { provide: Router, useValue: routerSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: MatDialog, useValue: dialogSpy }
      ]
    }).compileComponents();

    cycleService = TestBed.inject(CycleService) as jasmine.SpyObj<CycleService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    snackBar = TestBed.inject(MatSnackBar) as jasmine.SpyObj<MatSnackBar>;
    dialog = TestBed.inject(MatDialog) as jasmine.SpyObj<MatDialog>;

    fixture = TestBed.createComponent(CycleDetailsComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Component Initialization', () => {
    it('should load cycle on init', () => {
      cycleService.getCycleById.and.returnValue(of({ data: mockCycle, message: 'Success' }));

      fixture.detectChanges();

      expect(component.cycleId).toBe(1);
      expect(cycleService.getCycleById).toHaveBeenCalledWith(1);
      expect(component.cycle).toEqual(mockCycle);
    });

    it('should handle cycle loading error', () => {
      const error = { message: 'Network error' };
      cycleService.getCycleById.and.returnValue(throwError(() => error));

      fixture.detectChanges();

      expect(snackBar.open).toHaveBeenCalledWith('Failed to load cycle details', 'Close', { duration: 3000 });
    });
  });

  describe('Search Filtering', () => {
    beforeEach(() => {
      cycleService.getCycleById.and.returnValue(of({ data: mockCycle, message: 'Success' }));
      component.forms = mockForms;
      component.filteredForms = [...mockForms];
      fixture.detectChanges();
    });

    it('should filter forms by employee name', () => {
      component.filterForms('john');
      expect(component.filteredForms.length).toBe(1);
      expect(component.filteredForms[0].employeeName).toBe('John Doe');
    });

    it('should filter forms by manager name', () => {
      component.filterForms('jane');
      expect(component.filteredForms.length).toBe(2);
    });

    it('should filter forms by status', () => {
      component.filterForms('submitted');
      expect(component.filteredForms.length).toBe(1);
      expect(component.filteredForms[0].status).toBe('SUBMITTED');
    });

    it('should return all forms when search is empty', () => {
      component.filterForms('');
      expect(component.filteredForms.length).toBe(2);
    });

    it('should return empty array when no match', () => {
      component.filterForms('nonexistent');
      expect(component.filteredForms.length).toBe(0);
    });
  });

  describe('Form Reopen', () => {
    beforeEach(() => {
      cycleService.getCycleById.and.returnValue(of({ data: mockCycle, message: 'Success' }));
      fixture.detectChanges();
    });

    it('should open reopen dialog', () => {
      const dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['afterClosed']);
      dialogRefSpy.afterClosed.and.returnValue(of(false));
      dialog.open.and.returnValue(dialogRefSpy);

      component.reopenForm(mockForms[0]);

      expect(dialog.open).toHaveBeenCalled();
    });

    it('should reopen form when confirmed', () => {
      const dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['afterClosed']);
      dialogRefSpy.afterClosed.and.returnValue(of(true));
      dialog.open.and.returnValue(dialogRefSpy);
      cycleService.reopenForm.and.returnValue(of({ data: undefined, message: 'Success' }));

      component.reopenForm(mockForms[0]);

      expect(cycleService.reopenForm).toHaveBeenCalledWith(1, 1);
      expect(snackBar.open).toHaveBeenCalledWith('Form reopened successfully', 'Close', { duration: 3000 });
    });

    it('should handle reopen error', () => {
      const dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['afterClosed']);
      dialogRefSpy.afterClosed.and.returnValue(of(true));
      dialog.open.and.returnValue(dialogRefSpy);
      
      const error = { error: { message: 'Cannot reopen form' } };
      cycleService.reopenForm.and.returnValue(throwError(() => error));

      component.reopenForm(mockForms[0]);

      expect(snackBar.open).toHaveBeenCalledWith('Cannot reopen form', 'Close', { duration: 5000 });
    });
  });

  describe('Backup Reviewer Assignment', () => {
    beforeEach(() => {
      cycleService.getCycleById.and.returnValue(of({ data: mockCycle, message: 'Success' }));
      fixture.detectChanges();
    });

    it('should open assign backup reviewer dialog', () => {
      const dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['afterClosed']);
      dialogRefSpy.afterClosed.and.returnValue(of(null));
      dialog.open.and.returnValue(dialogRefSpy);

      component.assignBackupReviewer(mockForms[0]);

      expect(dialog.open).toHaveBeenCalled();
    });

    it('should assign backup reviewer when confirmed', () => {
      const dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['afterClosed']);
      dialogRefSpy.afterClosed.and.returnValue(of(6));
      dialog.open.and.returnValue(dialogRefSpy);
      cycleService.assignBackupReviewer.and.returnValue(of({ data: undefined, message: 'Success' }));

      component.assignBackupReviewer(mockForms[0]);

      expect(cycleService.assignBackupReviewer).toHaveBeenCalledWith(1, 1, 6);
      expect(snackBar.open).toHaveBeenCalledWith('Backup reviewer assigned successfully', 'Close', { duration: 3000 });
    });

    it('should handle assignment error', () => {
      const dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['afterClosed']);
      dialogRefSpy.afterClosed.and.returnValue(of(6));
      dialog.open.and.returnValue(dialogRefSpy);
      
      const error = { error: { message: 'Invalid reviewer' } };
      cycleService.assignBackupReviewer.and.returnValue(throwError(() => error));

      component.assignBackupReviewer(mockForms[0]);

      expect(snackBar.open).toHaveBeenCalledWith('Invalid reviewer', 'Close', { duration: 5000 });
    });
  });

  describe('Form Status Checks', () => {
    it('should allow reopen for SUBMITTED status', () => {
      const form: AppraisalForm = { ...mockForms[0], status: 'SUBMITTED' };
      expect(component.canReopen(form)).toBe(true);
    });

    it('should allow reopen for UNDER_REVIEW status', () => {
      const form: AppraisalForm = { ...mockForms[0], status: 'UNDER_REVIEW' };
      expect(component.canReopen(form)).toBe(true);
    });

    it('should allow reopen for REVIEW_DRAFT_SAVED status', () => {
      const form: AppraisalForm = { ...mockForms[0], status: 'REVIEW_DRAFT_SAVED' };
      expect(component.canReopen(form)).toBe(true);
    });

    it('should allow reopen for REVIEWED_AND_COMPLETED status', () => {
      const form: AppraisalForm = { ...mockForms[0], status: 'REVIEWED_AND_COMPLETED' };
      expect(component.canReopen(form)).toBe(true);
    });

    it('should not allow reopen for NOT_STARTED status', () => {
      const form: AppraisalForm = { ...mockForms[0], status: 'NOT_STARTED' };
      expect(component.canReopen(form)).toBe(false);
    });

    it('should not allow reopen for DRAFT_SAVED status', () => {
      const form: AppraisalForm = { ...mockForms[0], status: 'DRAFT_SAVED' };
      expect(component.canReopen(form)).toBe(false);
    });
  });

  describe('Status Display', () => {
    it('should return correct color for each status', () => {
      expect(component.getStatusColor('NOT_STARTED')).toBe('default');
      expect(component.getStatusColor('DRAFT_SAVED')).toBe('accent');
      expect(component.getStatusColor('SUBMITTED')).toBe('primary');
      expect(component.getStatusColor('UNDER_REVIEW')).toBe('primary');
      expect(component.getStatusColor('REVIEWED_AND_COMPLETED')).toBe('primary');
    });

    it('should format status for display', () => {
      expect(component.formatStatus('NOT_STARTED')).toBe('Not Started');
      expect(component.formatStatus('DRAFT_SAVED')).toBe('Draft Saved');
      expect(component.formatStatus('REVIEWED_AND_COMPLETED')).toBe('Reviewed And Completed');
    });
  });

  describe('Date Formatting', () => {
    it('should format date correctly', () => {
      const formatted = component.formatDate('2025-04-15T10:00:00Z');
      expect(formatted).toMatch(/Apr.*2025/);
    });

    it('should return dash for undefined date', () => {
      expect(component.formatDate(undefined)).toBe('-');
    });
  });

  describe('Navigation', () => {
    it('should navigate back to cycles list', () => {
      component.goBack();
      expect(router.navigate).toHaveBeenCalledWith(['/hr/cycles']);
    });
  });
});
