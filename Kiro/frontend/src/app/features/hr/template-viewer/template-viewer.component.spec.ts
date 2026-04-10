import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, ActivatedRoute } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { TemplateViewerComponent } from './template-viewer.component';
import { TemplateService } from '../../../core/services/template.service';
import { Router } from '@angular/router';
import { AppraisalTemplate } from '../../../core/models/appraisal.model';

describe('TemplateViewerComponent', () => {
  let component: TemplateViewerComponent;
  let fixture: ComponentFixture<TemplateViewerComponent>;
  let templateService: jasmine.SpyObj<TemplateService>;
  let router: jasmine.SpyObj<Router>;

  const mockTemplate: AppraisalTemplate = {
    id: 1,
    version: '3.0',
    schemaJson: JSON.stringify({
      version: '3.0',
      sections: [
        {
          sectionType: 'key_responsibilities',
          title: 'Key Responsibilities',
          items: [
            { id: 'kr_1', label: 'Essential Duty 1', ratingScale: 'competency' },
            { id: 'kr_2', label: 'Essential Duty 2', ratingScale: 'competency' }
          ]
        },
        {
          sectionType: 'idp',
          title: 'Individual Development Plan',
          items: [
            { id: 'idp_nextgen', label: 'NextGen Tech Skills', ratingScale: 'competency' }
          ]
        }
      ]
    }),
    isActive: true,
    createdBy: 1,
    createdAt: '2025-01-01T10:00:00Z'
  };

  const mockInvalidTemplate: AppraisalTemplate = {
    id: 2,
    version: '2.0',
    schemaJson: 'invalid json {',
    isActive: false,
    createdBy: 1,
    createdAt: '2024-01-01T10:00:00Z'
  };

  beforeEach(async () => {
    const templateServiceSpy = jasmine.createSpyObj('TemplateService', ['getTemplateById']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [TemplateViewerComponent],
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
        { provide: TemplateService, useValue: templateServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    templateService = TestBed.inject(TemplateService) as jasmine.SpyObj<TemplateService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;

    fixture = TestBed.createComponent(TemplateViewerComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Component Initialization', () => {
    it('should load template on init', () => {
      templateService.getTemplateById.and.returnValue(of({ data: mockTemplate, message: 'Success' }));

      fixture.detectChanges();

      expect(templateService.getTemplateById).toHaveBeenCalledWith(1);
      expect(component.template).toEqual(mockTemplate);
      expect(component.loading).toBe(false);
    });

    it('should parse JSON schema correctly', () => {
      templateService.getTemplateById.and.returnValue(of({ data: mockTemplate, message: 'Success' }));

      fixture.detectChanges();

      expect(component.parsedSchema).toBeTruthy();
      expect(component.parsedSchema.version).toBe('3.0');
      expect(component.parsedSchema.sections.length).toBe(2);
      expect(component.formattedJson).toContain('"version": "3.0"');
    });

    it('should handle invalid JSON schema', () => {
      templateService.getTemplateById.and.returnValue(of({ data: mockInvalidTemplate, message: 'Success' }));

      fixture.detectChanges();

      expect(component.parsedSchema).toBeNull();
      expect(component.formattedJson).toBe('invalid json {');
    });

    it('should handle null template data', () => {
      templateService.getTemplateById.and.returnValue(of({ data: null, message: 'Success' }));

      fixture.detectChanges();

      expect(component.template).toBeNull();
      expect(component.loading).toBe(false);
    });

    it('should handle template loading error', () => {
      const error = { message: 'Network error' };
      templateService.getTemplateById.and.returnValue(throwError(() => error));

      fixture.detectChanges();

      expect(component.error).toBe('Failed to load template. Please try again.');
      expect(component.loading).toBe(false);
    });

    it('should handle missing template ID', () => {
      const activatedRoute = TestBed.inject(ActivatedRoute);
      spyOn(activatedRoute.snapshot.paramMap, 'get').and.returnValue(null);

      const newFixture = TestBed.createComponent(TemplateViewerComponent);
      const newComponent = newFixture.componentInstance;

      newFixture.detectChanges();

      expect(newComponent.error).toBe('Invalid template ID');
    });
  });

  describe('JSON Schema Parsing', () => {
    it('should format JSON with proper indentation', () => {
      templateService.getTemplateById.and.returnValue(of({ data: mockTemplate, message: 'Success' }));

      fixture.detectChanges();

      expect(component.formattedJson).toContain('\n');
      expect(component.formattedJson).toContain('  ');
    });

    it('should parse sections correctly', () => {
      templateService.getTemplateById.and.returnValue(of({ data: mockTemplate, message: 'Success' }));

      fixture.detectChanges();

      expect(component.parsedSchema.sections[0].sectionType).toBe('key_responsibilities');
      expect(component.parsedSchema.sections[0].items.length).toBe(2);
      expect(component.parsedSchema.sections[1].sectionType).toBe('idp');
    });

    it('should handle empty schema JSON', () => {
      const emptyTemplate = { ...mockTemplate, schemaJson: '' };
      templateService.getTemplateById.and.returnValue(of({ data: emptyTemplate, message: 'Success' }));

      fixture.detectChanges();

      expect(component.formattedJson).toBe('');
      expect(component.parsedSchema).toBeNull();
    });
  });

  describe('Navigation', () => {
    it('should navigate back to template list', () => {
      component.goBack();
      expect(router.navigate).toHaveBeenCalledWith(['/hr/templates']);
    });
  });

  describe('Date Formatting', () => {
    it('should format date correctly with time', () => {
      const formatted = component.formatDate('2025-01-01T10:00:00Z');
      expect(formatted).toMatch(/January.*2025/);
      expect(formatted).toMatch(/10:00/);
    });
  });

  describe('Clipboard Operations', () => {
    beforeEach(() => {
      templateService.getTemplateById.and.returnValue(of({ data: mockTemplate, message: 'Success' }));
      fixture.detectChanges();
    });

    it('should copy JSON to clipboard', async () => {
      const clipboardSpy = spyOn(navigator.clipboard, 'writeText').and.returnValue(Promise.resolve());

      await component.copyToClipboard();

      expect(clipboardSpy).toHaveBeenCalledWith(component.formattedJson);
    });

    it('should handle clipboard copy error', async () => {
      const clipboardSpy = spyOn(navigator.clipboard, 'writeText').and.returnValue(
        Promise.reject(new Error('Clipboard error'))
      );
      const consoleSpy = spyOn(console, 'error');

      await component.copyToClipboard();

      expect(clipboardSpy).toHaveBeenCalled();
      expect(consoleSpy).toHaveBeenCalled();
    });

    it('should not copy when formattedJson is empty', async () => {
      component.formattedJson = '';
      const clipboardSpy = spyOn(navigator.clipboard, 'writeText');

      await component.copyToClipboard();

      expect(clipboardSpy).not.toHaveBeenCalled();
    });
  });

  describe('Data Binding', () => {
    it('should display template metadata', () => {
      templateService.getTemplateById.and.returnValue(of({ data: mockTemplate, message: 'Success' }));

      fixture.detectChanges();

      expect(component.template?.version).toBe('3.0');
      expect(component.template?.isActive).toBe(true);
      expect(component.template?.id).toBe(1);
    });

    it('should display parsed schema structure', () => {
      templateService.getTemplateById.and.returnValue(of({ data: mockTemplate, message: 'Success' }));

      fixture.detectChanges();

      expect(component.parsedSchema).toBeTruthy();
      expect(component.parsedSchema.sections).toBeDefined();
      expect(Array.isArray(component.parsedSchema.sections)).toBe(true);
    });
  });

  describe('Loading States', () => {
    it('should show loading state initially', () => {
      templateService.getTemplateById.and.returnValue(of({ data: mockTemplate, message: 'Success' }));

      expect(component.loading).toBe(false);
      
      fixture.detectChanges();

      expect(component.loading).toBe(false);
    });

    it('should clear loading state after successful load', () => {
      templateService.getTemplateById.and.returnValue(of({ data: mockTemplate, message: 'Success' }));

      fixture.detectChanges();

      expect(component.loading).toBe(false);
      expect(component.template).toBeTruthy();
    });

    it('should clear loading state after error', () => {
      const error = { message: 'Network error' };
      templateService.getTemplateById.and.returnValue(throwError(() => error));

      fixture.detectChanges();

      expect(component.loading).toBe(false);
      expect(component.error).toBeTruthy();
    });
  });
});
