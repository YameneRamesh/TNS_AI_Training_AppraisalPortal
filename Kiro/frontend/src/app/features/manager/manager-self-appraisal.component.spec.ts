import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { provideZonelessChangeDetection } from '@angular/core';

import { ManagerSelfAppraisalComponent } from './manager-self-appraisal.component';
import { FormRendererService } from '../../form-renderer/form-renderer.service';
import { AuthService } from '../../core/services/auth.service';
import { DialogService } from '../../shared/services/dialog.service';
import { AppraisalForm, AppraisalTemplate } from '../../core/models/appraisal.model';

const mockManagerUser = { id: 200, fullName: 'Jane Manager', email: 'jane@example.com', roles: ['MANAGER'] };

const mockAppraisalForm: AppraisalForm = {
  id: 10, cycleId: 1, cycleName: '2025-26 Annual Review',
  employeeId: 200, employeeName: 'Jane Manager',
  managerId: 300, managerName: 'Senior Manager',
  templateId: 1, status: 'NOT_STARTED', formData: {},
  createdAt: '2025-04-01T00:00:00Z', updatedAt: '2025-04-01T00:00:00Z'
};

const mockTemplate: AppraisalTemplate = {
  id: 1, version: '3.0',
  schemaJson: JSON.stringify({
    version: '3.0',
    sections: [
      { sectionType: 'key_responsibilities', title: 'Key Responsibilities', items: [{ id: 'kr_1', label: 'Responsibility 1', ratingScale: 'competency' }] },
      { sectionType: 'idp', title: 'IDP', items: [{ id: 'idp_1', label: 'NextGen Tech Skills', ratingScale: 'competency' }] },
      { sectionType: 'goals', title: 'Goals', items: [{ id: 'goal_1', label: 'Goal 1', ratingScale: 'competency' }] }
    ]
  }),
  isActive: true, createdBy: 1, createdAt: '2025-01-01T00:00:00Z'
};

describe('ManagerSelfAppraisalComponent', () => {
  let component: ManagerSelfAppraisalComponent;
  let fixture: ComponentFixture<ManagerSelfAppraisalComponent>;
  let httpMock: HttpTestingController;
  let router: Router;
  let mockAuthService: { currentUserValue: any };
  let mockDialogService: { confirm: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    mockAuthService = { currentUserValue: mockManagerUser };
    mockDialogService = { confirm: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [ManagerSelfAppraisalComponent, HttpClientTestingModule, MatSnackBarModule, NoopAnimationsModule],
      providers: [
        provideZonelessChangeDetection(),
        { provide: ActivatedRoute, useValue: { params: of({ id: '10' }), queryParams: of({}) } },
        { provide: AuthService, useValue: mockAuthService },
        { provide: DialogService, useValue: mockDialogService },
        FormRendererService
      ]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockImplementation(() => Promise.resolve(true));

    fixture = TestBed.createComponent(ManagerSelfAppraisalComponent);
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

    httpMock.expectOne('/api/forms/10').flush(mockAppraisalForm);
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    expect(component.appraisalForm).toEqual(mockAppraisalForm);
    expect(component.template).toEqual(mockTemplate);
    expect(component.loading).toBe(false);
  });

  it('should handle form load error', () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/10').error(new ProgressEvent('error'));

    expect(component.error).toBe('Failed to load appraisal form. Please try again.');
    expect(component.loading).toBe(false);
  });

  it('should handle template load error', () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/10').flush(mockAppraisalForm);
    httpMock.expectOne('/api/templates/1').error(new ProgressEvent('error'));

    expect(component.error).toBe('Failed to load form template. Please try again.');
    expect(component.loading).toBe(false);
  });

  // Req 6.9 / Req 5.3 — manager can edit self fields when NOT_STARTED
  it('should allow editing self fields when status is NOT_STARTED', () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/10').flush(mockAppraisalForm);
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    expect(component.canEditSelfFields).toBe(true);
    expect(component.canSaveDraft).toBe(true);
    expect(component.canSubmit).toBe(true);
  });

  it('should allow editing self fields when status is DRAFT_SAVED', () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/10').flush({ ...mockAppraisalForm, status: 'DRAFT_SAVED' });
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    expect(component.canEditSelfFields).toBe(true);
    expect(component.canSaveDraft).toBe(true);
    expect(component.canSubmit).toBe(true);
  });

  // Req 5.6 — prevent editing after submission
  it('should prevent editing when status is SUBMITTED', () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/10').flush({ ...mockAppraisalForm, status: 'SUBMITTED' });
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    expect(component.canEditSelfFields).toBe(false);
    expect(component.canSaveDraft).toBe(false);
    expect(component.canSubmit).toBe(false);
  });

  it('should prevent editing when status is REVIEWED_AND_COMPLETED', () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/10').flush({ ...mockAppraisalForm, status: 'REVIEWED_AND_COMPLETED' });
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    expect(component.canEditSelfFields).toBe(false);
  });

  it('should not allow editing when user is not the form owner', () => {
    mockAuthService.currentUserValue = { id: 999, fullName: 'Other Manager', roles: ['MANAGER'] };
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/10').flush(mockAppraisalForm);
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    expect(component.canEditSelfFields).toBe(false);
  });

  // Req 5.3 — save draft
  it('should save draft successfully', async () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/10').flush(mockAppraisalForm);
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    component.onSaveDraft();

    const req = httpMock.expectOne('/api/forms/10/draft');
    expect(req.request.method).toBe('PUT');
    req.flush({});

    await fixture.whenStable();

    expect(component.saving).toBe(false);
  });

  it('should not save draft when canSaveDraft is false', () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/10').flush({ ...mockAppraisalForm, status: 'SUBMITTED' });
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    component.onSaveDraft();

    expect(component.saving).toBe(false);
  });

  it('should handle save draft error', async () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/10').flush(mockAppraisalForm);
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    component.onSaveDraft();

    httpMock.expectOne('/api/forms/10/draft').error(new ProgressEvent('error'));

    await fixture.whenStable();

    expect(component.saving).toBe(false);
  });

  // Req 5.4 — submit form
  it('should submit form after confirmation', async () => {
    mockDialogService.confirm.mockResolvedValue(true);

    fixture.detectChanges();

    httpMock.expectOne('/api/forms/10').flush(mockAppraisalForm);
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    await component.onSubmit();

    const draftReq = httpMock.expectOne('/api/forms/10/draft');
    expect(draftReq.request.method).toBe('PUT');
    draftReq.flush({});

    const submitReq = httpMock.expectOne('/api/forms/10/submit');
    expect(submitReq.request.method).toBe('POST');
    submitReq.flush({});

    await fixture.whenStable();

    // Req 6.9 — navigates to manager dashboard, not employee dashboard
    expect(router.navigate).toHaveBeenCalledWith(['/manager/dashboard']);
  });

  it('should not submit form if confirmation is cancelled', async () => {
    mockDialogService.confirm.mockResolvedValue(false);

    fixture.detectChanges();

    httpMock.expectOne('/api/forms/10').flush(mockAppraisalForm);
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    await component.onSubmit();

    expect(component.saving).toBe(false);
  });

  it('should not submit when canSubmit is false', async () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/10').flush({ ...mockAppraisalForm, status: 'SUBMITTED' });
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    await component.onSubmit();

    expect(component.saving).toBe(false);
  });

  it('should navigate to manager dashboard on cancel with clean form', () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/10').flush(mockAppraisalForm);
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    component.onCancel();

    expect(router.navigate).toHaveBeenCalledWith(['/manager/dashboard']);
  });

  it('should prompt confirmation on cancel with dirty form', async () => {
    mockDialogService.confirm.mockResolvedValue(true);

    fixture.detectChanges();

    httpMock.expectOne('/api/forms/10').flush(mockAppraisalForm);
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    component.formGroup.markAsDirty();
    component.onCancel();

    await fixture.whenStable();

    expect(mockDialogService.confirm).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/manager/dashboard']);
  });

  it('should return correct status labels', () => {
    expect(component.getStatusLabel('NOT_STARTED')).toBe('Not Started');
    expect(component.getStatusLabel('DRAFT_SAVED')).toBe('Draft Saved');
    expect(component.getStatusLabel('SUBMITTED')).toBe('Submitted');
    expect(component.getStatusLabel('UNDER_REVIEW')).toBe('Under Review');
    expect(component.getStatusLabel('REVIEW_DRAFT_SAVED')).toBe('Review in Progress');
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

  it('should return empty arrays when template is null', () => {
    component.template = null;
    expect(component.getKeyResponsibilitiesItems()).toEqual([]);
    expect(component.getIdpItems()).toEqual([]);
    expect(component.getGoalsItems()).toEqual([]);
  });
});

describe('ManagerSelfAppraisalComponent (readonly mode)', () => {
  let component: ManagerSelfAppraisalComponent;
  let fixture: ComponentFixture<ManagerSelfAppraisalComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [ManagerSelfAppraisalComponent, HttpClientTestingModule, MatSnackBarModule, NoopAnimationsModule],
      providers: [
        provideZonelessChangeDetection(),
        { provide: ActivatedRoute, useValue: { params: of({ id: '10' }), queryParams: of({ readonly: 'true' }) } },
        { provide: AuthService, useValue: { currentUserValue: mockManagerUser } },
        { provide: DialogService, useValue: { confirm: vi.fn() } },
        FormRendererService
      ]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ManagerSelfAppraisalComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should set readonly to true from query params', () => {
    fixture.detectChanges();
    httpMock.expectOne('/api/forms/10').flush(mockAppraisalForm);
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    expect(component.readonly).toBe(true);
  });

  it('should not allow editing in readonly mode even when status is NOT_STARTED', () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/10').flush(mockAppraisalForm);
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    expect(component.canEditSelfFields).toBe(false);
    expect(component.canSaveDraft).toBe(false);
    expect(component.canSubmit).toBe(false);
  });
});
