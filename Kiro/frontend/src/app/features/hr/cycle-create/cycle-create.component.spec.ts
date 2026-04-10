import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { CycleCreateComponent } from './cycle-create.component';
import { CycleService } from '../../../core/services/cycle.service';
import { TemplateService } from '../../../core/services/template.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { AppraisalTemplate } from '../../../core/models/appraisal.model';

describe('CycleCreateComponent', () => {
  let component: CycleCreateComponent;
  let fixture: ComponentFixture<CycleCreateComponent>;
  let cycleService: jasmine.SpyObj<CycleService>;
  let templateService: jasmine.SpyObj<TemplateService>;
  let router: jasmine.SpyObj<Router>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;

  const mockTemplates: AppraisalTemplate[] = [
    {
      id: 1,
      version: '3.0',
      schemaJson: '{"version":"3.0","sections":[]}',
      isActive: true,
      createdBy: 1,
      createdAt: '2025-01-01T10:00:00Z'
    },
    {
      id: 2,
      version: '2.0',
      schemaJson: '{"version":"2.0","sections":[]}',
      isActive: false,
      createdBy: 1,
      createdAt: '2024-01-01T10:00:00Z'
    }
  ];

  beforeEach(async () => {
    const cycleServiceSpy = jasmine.createSpyObj('CycleService', ['createCycle']);
    const templateServiceSpy = jasmine.createSpyObj('TemplateService', ['getTemplates']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      imports: [CycleCreateComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimations(),
        { provide: CycleService, useValue: cycleServiceSpy },
        { provide: TemplateService, useValue: templateServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: MatSnackBar, useValue: snackBarSpy }
      ]
    }).compileComponents();

    cycleService = TestBed.inject(CycleService) as jasmine.SpyObj<CycleService>;
    templateService = TestBed.inject(TemplateService) as jasmine.SpyObj<TemplateService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    snackBar = TestBed.inject(MatSnackBar) as jasmine.SpyObj<MatSnackBar>;

    fixture = TestBed.createComponent(CycleCreateComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Component Initialization', () => {
    it('should load templates on init', () => {
      templateService.getTemplates.and.returnValue(of({ data: mockTemplates, message: 'Success' }));

      fixture.detectChanges();

      expect(templateService.getTemplates).toHaveBeenCalled();
      expect(component.templates).toEqual(mockTemplates);
      expect(component.loading).toBe(false);
    });

    it('should auto-select active template', () => {
      templateService.getTemplates.and.returnValue(of({ data: mockTemplates, message: 'Success' }));

      fixture.detectChanges();

      expect(component.cycleForm.get('templateId')?.value).toBe(1);
    });

    it('should handle empty template list', () => {
      templateService.getTemplates.and.returnValue(of({ data: [], message: 'Success' }));

      fixture.detectChanges();

      expect(component.templates).toEqual([]);
      expect(component.cycleForm.get('templateId')?.value).toBe('');
    });

    it('should handle template loading error', () => {
      const error = { message: 'Network error' };
      templateService.getTemplates.and.returnValue(throwError(() => error));

      fixture.detectChanges();

      expect(component.loading).toBe(false);
      expect(snackBar.open).toHaveBeenCalledWith('Failed to load templates', 'Close', { duration: 3000 });
    });
  });

  describe('Form Validation', () => {
    beforeEach(() => {
      templateService.getTemplates.and.returnValue(of({ data: mockTemplates, message: 'Success' }));
      fixture.detectChanges();
    });

    it('should initialize form with empty values', () => {
      expect(component.cycleForm.get('name')?.value).toBe('');
      expect(component.cycleForm.get('startDate')?.value).toBe('');
      expect(component.cycleForm.get('endDate')?.value).toBe('');
    });

    it('should require all fields', () => {
      expect(component.cycleForm.valid).toBe(false);
      
      component.cycleForm.patchValue({
        name: 'Test Cycle',
        startDate: new Date('2025-04-01'),
        endDate: new Date('2026-03-31'),
        templateId: 1
      });

      expect(component.cycleForm.valid).toBe(true);
    });

    it('should validate name max length', () => {
      const longName = 'a'.repeat(201);
      component.cycleForm.patchValue({ name: longName });
      
      expect(component.cycleForm.get('name')?.hasError('maxlength')).toBe(true);
    });

    it('should validate end date is after start date', () => {
      component.cycleForm.patchValue({
        name: 'Test Cycle',
        startDate: new Date('2025-04-01'),
        endDate: new Date('2025-03-31'),
        templateId: 1
      });

      expect(component.cycleForm.hasError('dateRangeInvalid')).toBe(true);
    });

    it('should accept valid date range', () => {
      component.cycleForm.patchValue({
        name: 'Test Cycle',
        startDate: new Date('2025-04-01'),
        endDate: new Date('2026-03-31'),
        templateId: 1
      });

      expect(component.cycleForm.hasError('dateRangeInvalid')).toBe(false);
    });
  });

  describe('Form Submission', () => {
    beforeEach(() => {
      templateService.getTemplates.and.returnValue(of({ data: mockTemplates, message: 'Success' }));
      fixture.detectChanges();
    });

    it('should not submit invalid form', () => {
      component.onSubmit();

      expect(cycleService.createCycle).not.toHaveBeenCalled();
    });

    it('should submit valid form successfully', () => {
      const mockResponse = {
        data: {
          id: 1,
          name: 'Test Cycle',
          startDate: '2025-04-01',
          endDate: '2026-03-31',
          templateId: 1,
          status: 'DRAFT' as const,
          createdBy: 1,
          createdAt: '2025-03-01T10:00:00Z',
          updatedAt: '2025-03-01T10:00:00Z'
        },
        message: 'Success'
      };

      cycleService.createCycle.and.returnValue(of(mockResponse));

      component.cycleForm.patchValue({
        name: 'Test Cycle',
        startDate: new Date('2025-04-01'),
        endDate: new Date('2026-03-31'),
        templateId: 1
      });

      component.onSubmit();

      expect(cycleService.createCycle).toHaveBeenCalledWith({
        name: 'Test Cycle',
        startDate: '2025-04-01',
        endDate: '2026-03-31',
        templateId: 1,
        status: 'DRAFT'
      });
      expect(snackBar.open).toHaveBeenCalledWith('Cycle created successfully', 'Close', { duration: 3000 });
      expect(router.navigate).toHaveBeenCalledWith(['/hr/cycles']);
    });

    it('should handle submission error', () => {
      const error = { error: { message: 'Cycle name already exists' } };
      cycleService.createCycle.and.returnValue(throwError(() => error));

      component.cycleForm.patchValue({
        name: 'Test Cycle',
        startDate: new Date('2025-04-01'),
        endDate: new Date('2026-03-31'),
        templateId: 1
      });

      component.onSubmit();

      expect(component.submitting).toBe(false);
      expect(snackBar.open).toHaveBeenCalledWith('Cycle name already exists', 'Close', { duration: 5000 });
      expect(router.navigate).not.toHaveBeenCalled();
    });

    it('should handle generic submission error', () => {
      const error = { message: 'Network error' };
      cycleService.createCycle.and.returnValue(throwError(() => error));

      component.cycleForm.patchValue({
        name: 'Test Cycle',
        startDate: new Date('2025-04-01'),
        endDate: new Date('2026-03-31'),
        templateId: 1
      });

      component.onSubmit();

      expect(snackBar.open).toHaveBeenCalledWith('Failed to create cycle. Please try again.', 'Close', { duration: 5000 });
    });
  });

  describe('Navigation', () => {
    it('should navigate back on cancel', () => {
      component.onCancel();
      expect(router.navigate).toHaveBeenCalledWith(['/hr/cycles']);
    });
  });

  describe('Error Messages', () => {
    beforeEach(() => {
      templateService.getTemplates.and.returnValue(of({ data: mockTemplates, message: 'Success' }));
      fixture.detectChanges();
    });

    it('should return required error message', () => {
      component.cycleForm.get('name')?.markAsTouched();
      expect(component.getErrorMessage('name')).toBe('Cycle name is required');
    });

    it('should return max length error message', () => {
      const longName = 'a'.repeat(201);
      component.cycleForm.patchValue({ name: longName });
      component.cycleForm.get('name')?.markAsTouched();
      
      expect(component.getErrorMessage('name')).toBe('Cycle name cannot exceed 200 characters');
    });

    it('should return date range error message', () => {
      component.cycleForm.patchValue({
        startDate: new Date('2025-04-01'),
        endDate: new Date('2025-03-31')
      });
      component.cycleForm.get('endDate')?.markAsTouched();
      
      expect(component.getErrorMessage('endDate')).toBe('End date must be after start date');
    });
  });

  describe('Minimum End Date', () => {
    beforeEach(() => {
      templateService.getTemplates.and.returnValue(of({ data: mockTemplates, message: 'Success' }));
      fixture.detectChanges();
    });

    it('should return minStartDate when no start date selected', () => {
      expect(component.minEndDate).toEqual(component.minStartDate);
    });

    it('should return day after start date when start date selected', () => {
      const startDate = new Date('2025-04-01');
      component.cycleForm.patchValue({ startDate });

      const minEnd = component.minEndDate;
      expect(minEnd).toBeTruthy();
      expect(minEnd!.getDate()).toBe(2);
    });
  });
});
