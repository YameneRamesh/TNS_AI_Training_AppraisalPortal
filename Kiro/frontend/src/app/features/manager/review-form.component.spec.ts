import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { provideZonelessChangeDetection } from '@angular/core';

import { ReviewFormComponent } from './review-form.component';
import { FormRendererService } from '../../form-renderer/form-renderer.service';
import { AuthService } from '../../core/services/auth.service';
import { DialogService } from '../../shared/services/dialog.service';
import { AppraisalForm, AppraisalTemplate } from '../../core/models/appraisal.model';

const mockManagerUser = { id: 200, fullName: 'Jane Manager', email: 'jane@example.com', roles: ['MANAGER'] };

const mockAppraisalForm: AppraisalForm = {
  id: 1, cycleId: 1, cycleName: '2025-26 Annual Review',
  employeeId: 100, employeeName: 'John Doe',
  managerId: 200, managerName: 'Jane Manager',
  templateId: 1, status: 'SUBMITTED', formData: {
    overallEvaluation: { teamMemberComments: 'My comments', managerComments: '' }
  },
  createdAt: '2025-04-01T00:00:00Z', updatedAt: '2025-04-01T00:00:00Z'
};

const mockTemplate: AppraisalTemplate = {
  id: 1, version: '3.0',
  schemaJson: JSON.stringify({
    version: '3.0',
    sections: [
      { sectionType: 'key_responsibilities', title: 'Key Responsibilities', items: [{ id: 'kr_1', label: 'Responsibility 1', ratingScale: 'competency' }] },
      { sectionType: 'idp', title: 'IDP', items: [{ id: 'idp_1', label: 'NextGen Tech Skills', ratingScale: 'competency' }] },
      { sectionType: 'goals', title: 'Goals', items: [{ id: 'goal_1', label: 'Goal 1', ratingScale: 'competency' }] },
      { sectionType: 'policy_adherence', title: 'Policy Adherence', items: [{ id: 'policy_hr', label: 'Follow HR Policy', ratingScale: 'policy_1_10' }] }
    ]
  }),
  isActive: true, createdBy: 1, createdAt: '2025-01-01T00:00:00Z'
};

describe('ReviewFormComponent', () => {
  let component: ReviewFormComponent;
  let fixture: ComponentFixture<ReviewFormComponent>;
  let httpMock: HttpTestingController;
  let router: Router;
  let mockAuthService: { currentUserValue: any };
  let mockDialogService: { confirm: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    mockAuthService = { currentUserValue: mockManagerUser };
    mockDialogService = { confirm: vi.fn() };

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [ReviewFormComponent, HttpClientTestingModule, MatSnackBarModule, NoopAnimationsModule],
      providers: [
        provideZonelessChangeDetection(),
        { provide: ActivatedRoute, useValue: { params: of({ id: '1' }), queryParams: of({}) } },
        { provide: AuthService, useValue: mockAuthService },
        { provide: DialogService, useValue: mockDialogService },
        FormRendererService
      ]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockImplementation(() => Promise.resolve(true));

    fixture = TestBed.createComponent(ReviewFormComponent);
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

    httpMock.expectOne('/api/forms/1').flush(mockAppraisalForm);
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    expect(component.appraisalForm).toEqual(mockAppraisalForm);
    expect(component.template).toEqual(mockTemplate);
    expect(component.loading).toBe(false);
  });

  it('should handle form load error', () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/1').error(new ProgressEvent('error'));

    expect(component.error).toBe('Failed to load appraisal form. Please try again.');
    expect(component.loading).toBe(false);
  });

  it('should handle template load error', () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/1').flush(mockAppraisalForm);
    httpMock.expectOne('/api/templates/1').error(new ProgressEvent('error'));

    expect(component.error).toBe('Failed to load form template. Please try again.');
    expect(component.loading).toBe(false);
  });

  // Req 6.2 — manager can edit manager-designated fields when form is SUBMITTED
  it('should allow manager to edit fields when form is SUBMITTED', () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/1').flush(mockAppraisalForm);
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    expect(component.canEditManagerFields).toBe(true);
  });

  // Req 6.3 — save draft is only available once the form is actively under review
  it('should not allow saving review draft when form is SUBMITTED (draft requires UNDER_REVIEW)', () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/1').flush(mockAppraisalForm);
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    // canSaveReviewDraft requires UNDER_REVIEW or REVIEW_DRAFT_SAVED, not SUBMITTED
    expect(component.canSaveReviewDraft).toBe(false);
    // But manager can still edit fields and complete the review
    expect(component.canEditManagerFields).toBe(true);
    expect(component.canCompleteReview).toBe(true);
  });

  it('should allow saving review draft when form is UNDER_REVIEW', () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/1').flush({ ...mockAppraisalForm, status: 'UNDER_REVIEW' });
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    expect(component.canSaveReviewDraft).toBe(true);
  });

  it('should allow saving review draft when form is REVIEW_DRAFT_SAVED', () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/1').flush({ ...mockAppraisalForm, status: 'REVIEW_DRAFT_SAVED' });
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    expect(component.canSaveReviewDraft).toBe(true);
  });

  // Req 6.4 — manager can complete review
  it('should allow completing review when form is SUBMITTED', () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/1').flush(mockAppraisalForm);
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    expect(component.canCompleteReview).toBe(true);
  });

  // Req 6.8 — no edit permissions when form is REVIEWED_AND_COMPLETED
  it('should not allow editing when form is REVIEWED_AND_COMPLETED', () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/1').flush({ ...mockAppraisalForm, status: 'REVIEWED_AND_COMPLETED' });
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    expect(component.canEditManagerFields).toBe(false);
    expect(component.canSaveReviewDraft).toBe(false);
    expect(component.canCompleteReview).toBe(false);
  });

  // Req 15.3 — backup reviewer has same permissions as primary manager
  it('should allow backup reviewer to edit manager fields', () => {
    mockAuthService.currentUserValue = { id: 999, fullName: 'Backup Reviewer', roles: ['MANAGER'] };
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/1').flush({ ...mockAppraisalForm, status: 'UNDER_REVIEW', backupReviewerId: 999 });
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    expect(component.canEditManagerFields).toBe(true);
    expect(component.canSaveReviewDraft).toBe(true);
    expect(component.canCompleteReview).toBe(true);
  });

  it('should not allow a non-manager user to edit fields', () => {
    mockAuthService.currentUserValue = { id: 300, fullName: 'Other User', roles: ['EMPLOYEE'] };
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/1').flush(mockAppraisalForm);
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    expect(component.canEditManagerFields).toBe(false);
  });

  // Req 6.3 — save draft API call (requires UNDER_REVIEW status)
  it('should make PUT request to save review draft', () => {
    fixture.detectChanges();
    httpMock.expectOne('/api/forms/1').flush({ ...mockAppraisalForm, status: 'UNDER_REVIEW' });
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    component.onSaveDraft();

    const req = httpMock.expectOne('/api/forms/1/review/draft');
    expect(req.request.method).toBe('PUT');
    req.flush({});
  });

  it('should update status to REVIEW_DRAFT_SAVED after successful save draft', () => {
    // Test without template rendering to avoid NG0100 from status mutation in HTTP callback
    component.appraisalForm = { ...mockAppraisalForm, status: 'UNDER_REVIEW' };
    component.template = mockTemplate;
    component['formId'] = 1;
    component['updatePermissions']();

    component.onSaveDraft();
    httpMock.expectOne('/api/forms/1/review/draft').flush({});

    expect(component.appraisalForm?.status).toBe('REVIEW_DRAFT_SAVED');
  });

  it('should reset saving flag after draft save error', () => {
    // Test without template rendering to avoid NG0100
    component.appraisalForm = { ...mockAppraisalForm, status: 'UNDER_REVIEW' };
    component.template = mockTemplate;
    component['formId'] = 1;
    component['updatePermissions']();

    component.onSaveDraft();
    httpMock.expectOne('/api/forms/1/review/draft').error(new ProgressEvent('error'));

    expect(component.saving).toBe(false);
  });

  it('should not save draft when canSaveReviewDraft is false', () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/1').flush({ ...mockAppraisalForm, status: 'REVIEWED_AND_COMPLETED' });
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    component.onSaveDraft();

    // No draft request should be made
    expect(component.saving).toBe(false);
  });

  // Req 6.4 — complete review API call
  it('should complete review after confirmation', async () => {
    mockDialogService.confirm.mockResolvedValue(true);

    // Test without template rendering to avoid NG0100
    component.appraisalForm = { ...mockAppraisalForm, status: 'UNDER_REVIEW' };
    component.template = mockTemplate;
    component['formId'] = 1;
    component['updatePermissions']();

    const formRendererService = TestBed.inject(FormRendererService);
    vi.spyOn(formRendererService, 'validateForManagerCompletion').mockReturnValue({ valid: true, errors: {} });

    await component.onCompleteReview();

    const draftReq = httpMock.expectOne('/api/forms/1/review/draft');
    expect(draftReq.request.method).toBe('PUT');
    draftReq.flush({});

    const completeReq = httpMock.expectOne('/api/forms/1/review/complete');
    expect(completeReq.request.method).toBe('POST');
    completeReq.flush({});

    await fixture.whenStable();

    expect(router.navigate).toHaveBeenCalledWith(['/manager/dashboard']);
  });

  it('should not complete review if confirmation is cancelled', async () => {
    mockDialogService.confirm.mockResolvedValue(false);

    component.appraisalForm = { ...mockAppraisalForm, status: 'UNDER_REVIEW' };
    component.template = mockTemplate;
    component['formId'] = 1;
    component['updatePermissions']();

    const formRendererService = TestBed.inject(FormRendererService);
    vi.spyOn(formRendererService, 'validateForManagerCompletion').mockReturnValue({ valid: true, errors: {} });

    await component.onCompleteReview();

    expect(component.saving).toBe(false);
  });

  it('should navigate to manager dashboard on cancel with no dirty form', () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/1').flush(mockAppraisalForm);
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    component.onCancel();

    expect(router.navigate).toHaveBeenCalledWith(['/manager/dashboard']);
  });

  it('should prompt confirmation on cancel with dirty form', async () => {
    mockDialogService.confirm.mockResolvedValue(true);

    fixture.detectChanges();

    httpMock.expectOne('/api/forms/1').flush(mockAppraisalForm);
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    component.formGroup.markAsDirty();
    component.onCancel();

    await fixture.whenStable();

    expect(mockDialogService.confirm).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/manager/dashboard']);
  });

  it('should return correct status labels', () => {
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

  it('should extract policy adherence items from template', () => {
    component.template = mockTemplate;
    const items = component.getPolicyAdherenceItems();
    expect(items.length).toBe(1);
    expect(items[0].id).toBe('policy_hr');
  });

  it('should return empty arrays when template is null', () => {
    component.template = null;
    expect(component.getKeyResponsibilitiesItems()).toEqual([]);
    expect(component.getIdpItems()).toEqual([]);
    expect(component.getGoalsItems()).toEqual([]);
    expect(component.getPolicyAdherenceItems()).toEqual([]);
  });

  it('should expose overallEvaluationGroup from formGroup', () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/1').flush(mockAppraisalForm);
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    expect(component.overallEvaluationGroup).toBeTruthy();
    expect(component.overallEvaluationGroup.get('managerComments')).toBeTruthy();
    expect(component.overallEvaluationGroup.get('teamMemberComments')).toBeTruthy();
  });
});

describe('ReviewFormComponent (readonly mode)', () => {
  let component: ReviewFormComponent;
  let fixture: ComponentFixture<ReviewFormComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [ReviewFormComponent, HttpClientTestingModule, MatSnackBarModule, NoopAnimationsModule],
      providers: [
        provideZonelessChangeDetection(),
        { provide: ActivatedRoute, useValue: { params: of({ id: '1' }), queryParams: of({ readonly: 'true' }) } },
        { provide: AuthService, useValue: { currentUserValue: mockManagerUser } },
        { provide: DialogService, useValue: { confirm: vi.fn() } },
        FormRendererService
      ]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ReviewFormComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should set readonly to true from query params', () => {
    fixture.detectChanges();
    // Flush the HTTP request triggered by ngOnInit
    httpMock.expectOne('/api/forms/1').flush(mockAppraisalForm);
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    expect(component.readonly).toBe(true);
  });

  it('should not allow editing in readonly mode even when form is SUBMITTED', () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/forms/1').flush(mockAppraisalForm);
    httpMock.expectOne('/api/templates/1').flush(mockTemplate);

    expect(component.canEditManagerFields).toBe(false);
    expect(component.canSaveReviewDraft).toBe(false);
    expect(component.canCompleteReview).toBe(false);
  });
});
