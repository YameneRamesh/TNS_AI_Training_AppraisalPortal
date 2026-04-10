import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { TemplateListComponent } from './template-list.component';
import { TemplateService } from '../../../core/services/template.service';
import { DialogService } from '../../../shared/services/dialog.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { AppraisalTemplate } from '../../../core/models/appraisal.model';

describe('TemplateListComponent', () => {
  let component: TemplateListComponent;
  let fixture: ComponentFixture<TemplateListComponent>;
  let templateService: jasmine.SpyObj<TemplateService>;
  let dialogService: jasmine.SpyObj<DialogService>;
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
    },
    {
      id: 3,
      version: '1.0',
      schemaJson: '{"version":"1.0","sections":[]}',
      isActive: false,
      createdBy: 1,
      createdAt: '2023-01-01T10:00:00Z'
    }
  ];

  beforeEach(async () => {
    const templateServiceSpy = jasmine.createSpyObj('TemplateService', [
      'getTemplates',
      'activateTemplate',
      'deactivateTemplate'
    ]);
    const dialogServiceSpy = jasmine.createSpyObj('DialogService', ['confirmAction']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      imports: [TemplateListComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimations(),
        { provide: TemplateService, useValue: templateServiceSpy },
        { provide: DialogService, useValue: dialogServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: MatSnackBar, useValue: snackBarSpy }
      ]
    }).compileComponents();

    templateService = TestBed.inject(TemplateService) as jasmine.SpyObj<TemplateService>;
    dialogService = TestBed.inject(DialogService) as jasmine.SpyObj<DialogService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    snackBar = TestBed.inject(MatSnackBar) as jasmine.SpyObj<MatSnackBar>;

    fixture = TestBed.createComponent(TemplateListComponent);
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

    it('should handle empty template list', () => {
      templateService.getTemplates.and.returnValue(of({ data: [], message: 'Success' }));

      fixture.detectChanges();

      expect(component.templates).toEqual([]);
      expect(component.loading).toBe(false);
    });

    it('should handle null template data', () => {
      templateService.getTemplates.and.returnValue(of({ data: null, message: 'Success' }));

      fixture.detectChanges();

      expect(component.templates).toEqual([]);
      expect(component.loading).toBe(false);
    });

    it('should handle template loading error', () => {
      const error = { message: 'Network error' };
      templateService.getTemplates.and.returnValue(throwError(() => error));

      fixture.detectChanges();

      expect(component.error).toBe('Failed to load templates. Please try again.');
      expect(component.loading).toBe(false);
    });
  });

  describe('Navigation', () => {
    beforeEach(() => {
      templateService.getTemplates.and.returnValue(of({ data: mockTemplates, message: 'Success' }));
      fixture.detectChanges();
    });

    it('should navigate to template viewer', () => {
      const template = mockTemplates[0];
      component.viewTemplate(template);
      expect(router.navigate).toHaveBeenCalledWith(['/hr/templates', template.id]);
    });
  });

  describe('Template Activation', () => {
    beforeEach(() => {
      templateService.getTemplates.and.returnValue(of({ data: mockTemplates, message: 'Success' }));
      fixture.detectChanges();
    });

    it('should show confirmation dialog before activation', () => {
      dialogService.confirmAction.and.returnValue(of(false));

      component.activateTemplate(mockTemplates[1]);

      expect(dialogService.confirmAction).toHaveBeenCalledWith(
        'Are you sure you want to activate template version 2.0? This will deactivate all other templates.',
        'Activate Template'
      );
      expect(templateService.activateTemplate).not.toHaveBeenCalled();
    });

    it('should activate template when confirmed', () => {
      dialogService.confirmAction.and.returnValue(of(true));
      templateService.activateTemplate.and.returnValue(of({ data: mockTemplates[1], message: 'Success' }));

      component.activateTemplate(mockTemplates[1]);

      expect(templateService.activateTemplate).toHaveBeenCalledWith(2);
      expect(snackBar.open).toHaveBeenCalledWith('Template activated successfully', 'Close', { duration: 3000 });
      expect(templateService.getTemplates).toHaveBeenCalledTimes(2); // Initial load + reload
    });

    it('should handle activation error', () => {
      dialogService.confirmAction.and.returnValue(of(true));
      const error = { message: 'Activation failed' };
      templateService.activateTemplate.and.returnValue(throwError(() => error));

      component.activateTemplate(mockTemplates[1]);

      expect(snackBar.open).toHaveBeenCalledWith('Failed to activate template. Please try again.', 'Close', { duration: 5000 });
    });
  });

  describe('Template Deactivation', () => {
    beforeEach(() => {
      templateService.getTemplates.and.returnValue(of({ data: mockTemplates, message: 'Success' }));
      fixture.detectChanges();
    });

    it('should show confirmation dialog before deactivation', () => {
      dialogService.confirmAction.and.returnValue(of(false));

      component.deactivateTemplate(mockTemplates[0]);

      expect(dialogService.confirmAction).toHaveBeenCalledWith(
        'Are you sure you want to deactivate template version 3.0?',
        'Deactivate Template'
      );
      expect(templateService.deactivateTemplate).not.toHaveBeenCalled();
    });

    it('should deactivate template when confirmed', () => {
      dialogService.confirmAction.and.returnValue(of(true));
      templateService.deactivateTemplate.and.returnValue(of({ data: mockTemplates[0], message: 'Success' }));

      component.deactivateTemplate(mockTemplates[0]);

      expect(templateService.deactivateTemplate).toHaveBeenCalledWith(1);
      expect(snackBar.open).toHaveBeenCalledWith('Template deactivated successfully', 'Close', { duration: 3000 });
      expect(templateService.getTemplates).toHaveBeenCalledTimes(2); // Initial load + reload
    });

    it('should handle deactivation error', () => {
      dialogService.confirmAction.and.returnValue(of(true));
      const error = { message: 'Deactivation failed' };
      templateService.deactivateTemplate.and.returnValue(throwError(() => error));

      component.deactivateTemplate(mockTemplates[0]);

      expect(snackBar.open).toHaveBeenCalledWith('Failed to deactivate template. Please try again.', 'Close', { duration: 5000 });
    });
  });

  describe('Date Formatting', () => {
    it('should format date correctly', () => {
      const formatted = component.formatDate('2025-01-01T10:00:00Z');
      expect(formatted).toMatch(/Jan.*2025/);
    });
  });

  describe('Data Binding', () => {
    it('should display all templates', () => {
      templateService.getTemplates.and.returnValue(of({ data: mockTemplates, message: 'Success' }));

      fixture.detectChanges();

      expect(component.templates.length).toBe(3);
      expect(component.templates[0].version).toBe('3.0');
      expect(component.templates[0].isActive).toBe(true);
    });

    it('should identify active template', () => {
      templateService.getTemplates.and.returnValue(of({ data: mockTemplates, message: 'Success' }));

      fixture.detectChanges();

      const activeTemplates = component.templates.filter(t => t.isActive);
      expect(activeTemplates.length).toBe(1);
      expect(activeTemplates[0].version).toBe('3.0');
    });
  });
});
