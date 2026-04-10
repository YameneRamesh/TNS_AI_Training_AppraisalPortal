import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { provideZonelessChangeDetection } from '@angular/core';

import { SelfAppraisalFormComponent } from './self-appraisal-form.component';
import { FormRendererService } from '../../form-renderer/form-renderer.service';
import { AuthService } from '../../core/services/auth.service';
import { DialogService } from '../../shared/services/dialog.service';
import { AppraisalForm, AppraisalTemplate } from '../../core/models/appraisal.model';

describe('SelfAppraisalFormComponent', () => {
  let component: SelfAppraisalFormComponent;
  let fixture: ComponentFixture<SelfAppraisalFormComponent>;
  let httpMock: HttpTestingController;
  let router: Router;
  let authService: { currentUserValue: any };
  let dialogService: { confirm: ReturnType<typeof vi.fn> };

  const mockAppraisalForm: AppraisalForm = {
    id: 1, cycleId: 1, cycleName: '2025-26 Annual Review',
    employeeId: 100, employeeName: 'John Doe',
    managerId: 200, managerName: 'Jane Manager',
    templateId: 1, status: 'NOT_STARTED', formData: {},
    createdAt: '2025-04-01T00:00:00Z', updatedAt: '2025-04-01T00:00:00Z'
  };

  const mockTemplate: AppraisalTemplate = {
    id: 1, version: '3.0',
    schemaJson: JSON.stringify({
      version: '3.0',
      sections: [
        { sectionType: 'key_responsibilities', title: 'Key Responsibilities', items: [{ id: 'kr_1', label: 'Responsibility 1', ratingScale: 'competency' }] },
        { sectionType: 'idp', title: 'Individual Development Plan', items: [{ id: 'idp_1', label: 'NextGen Tech Skills', ratingScale: 'competency' }] },
        { sectionType: 'goals', title: 'Goals', items: [{ id: 'goal_1', label: 'Goal 1', ratingScale: 'competency' }] }
      ]
    }),
    isActive: true, createdBy: 1, createdAt: '2025-01-01T00:00:00Z'
  };

  beforeEach(async () => {
    authService = { currentUserValue: { id: 100, fullName: 'John Doe', email: 'john@example.com', roles: ['EMPLOYEE'] } };
    dialogService = { confirm: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [SelfAppraisalFormComponent, HttpClientTestingModule, MatSnackBarModule, NoopAnimationsModule],
      providers: [
        provideZonelessChangeDetection(),
        { provide: ActivatedRoute, useValue: { params: of({ id: '1' }), queryParams: of({}) } },
        { provide: AuthService, useValue: authService },
        { provide: DialogService, useValue: dialogService },
        FormRendererService
      ]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);

    fixture = TestBed.createComponent(SelfAppraisalFormComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load form and template on init', () => {
    fixture.detectChanges();

    const formReq = httpMock.expectOne('/api/forms/1');
    expect(formReq.request.method).toBe('GET');
    formReq.flush(mockAppraisalForm);

    const templateReq = httpMock.expectOne('/api/templates/1');
    expect(templateReq.request.method).toBe('GET');
    templateReq.flush(mockTemplate);

    expect(component.appraisalForm).toEqual(mockAppraisalForm);
    expect(component.template).toEqual(mockTemplate);
    expect(component.loading).toBe(false);
  });

  it('should handle form load error', () => {
    fixture.detectChanges();

    const formReq = httpMock.expectOne('/api/forms/1');
    formReq.error(new ProgressEvent('error'));

    expect(component.error).toBe('Failed to load appraisal form. Please try again.');
    expect(component.loading).toBe(false);
  });

  it('should set readonly mode from query params', async () => {
    await TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [SelfAppraisalFormComponent, HttpClientTestingModule, MatSnackBarModule, NoopAnimationsModule],
      providers: [
        provideZonelessChangeDetection(),
        { provide: ActivatedRoute, useValue: { params: of({ id: '1' }), queryParams: of({ readonly: 'true' }) } },
        { provide: AuthService, useValue: authService },
        { provide: DialogService, useValue: dialogService },
        FormRendererService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SelfAppraisalFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.readonly).toBe(true);
  });

  it('should allow editing when status is NOT_STARTED', () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/1').flush(mockAppraisalForm);
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    expect(component.canEditSelfFields).toBe(true);
    expect(component.canSaveDraft).toBe(true);
    expect(component.canSubmit).toBe(true);
  });

  it('should prevent editing when status is SUBMITTED', () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/1').flush({ ...mockAppraisalForm, status: 'SUBMITTED' });
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    expect(component.canEditSelfFields).toBe(false);
    expect(component.canSaveDraft).toBe(false);
    expect(component.canSubmit).toBe(false);
  });

  it('should save draft successfully', async () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/1').flush(mockAppraisalForm);
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    component.onSaveDraft();

    const saveReq = httpMock.expectOne('/api/forms/1/draft');
    expect(saveReq.request.method).toBe('PUT');
    saveReq.flush({});

    await fixture.whenStable();

    expect(component.saving).toBe(false);
  });

  it('should submit form after confirmation', async () => {
    dialogService.confirm.mockResolvedValue(true);

    fixture.detectChanges();

    httpMock.expectOne('/api/forms/1').flush(mockAppraisalForm);
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    vi.spyOn(router, 'navigate').mockImplementation(() => Promise.resolve(true));

    await component.onSubmit();

    const draftReq = httpMock.expectOne('/api/forms/1/draft');
    expect(draftReq.request.method).toBe('PUT');
    draftReq.flush({});

    const submitReq = httpMock.expectOne('/api/forms/1/submit');
    expect(submitReq.request.method).toBe('POST');
    submitReq.flush({});

    await fixture.whenStable();

    expect(router.navigate).toHaveBeenCalledWith(['/employee/dashboard']);
  });

  it('should not submit form if confirmation is cancelled', async () => {
    dialogService.confirm.mockResolvedValue(false);

    fixture.detectChanges();

    httpMock.expectOne('/api/forms/1').flush(mockAppraisalForm);
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    await component.onSubmit();

    httpMock.expectNone('/api/forms/1/submit');
  });

  it('should get correct status labels', () => {
    expect(component.getStatusLabel('NOT_STARTED')).toBe('Not Started');
    expect(component.getStatusLabel('DRAFT_SAVED')).toBe('Draft Saved');
    expect(component.getStatusLabel('SUBMITTED')).toBe('Submitted');
    expect(component.getStatusLabel('REVIEWED_AND_COMPLETED')).toBe('Completed');
  });

  it('should extract key responsibilities items from template', () => {
    component.template = mockTemplate;
    const items = component.getKeyResponsibilitiesItems();
    expect(items.length).toBe(1);
    expect(items[0].id).toBe('kr_1');
  });

  it('should extract IDP items from template', () => {
    component.template = mockTemplate;
    const items = component.getIdpItems();
    expect(items.length).toBe(1);
    expect(items[0].id).toBe('idp_1');
  });

  it('should extract goals items from template', () => {
    component.template = mockTemplate;
    const items = component.getGoalsItems();
    expect(items.length).toBe(1);
    expect(items[0].id).toBe('goal_1');
  });

  it('should handle cancel with unsaved changes', async () => {
    dialogService.confirm.mockResolvedValue(true);
    vi.spyOn(router, 'navigate').mockImplementation(() => Promise.resolve(true));

    fixture.detectChanges();

    httpMock.expectOne('/api/forms/1').flush(mockAppraisalForm);
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    component.formGroup.markAsDirty();
    component.onCancel();

    await fixture.whenStable();

    expect(dialogService.confirm).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/employee/dashboard']);
  });
});
