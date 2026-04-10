import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormGroup, FormControl, FormArray, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { Subject, takeUntil } from 'rxjs';
import { HttpClient } from '@angular/common/http';

import { FormRendererService } from '../../form-renderer/form-renderer.service';
import { AppraisalForm, FormData, AppraisalTemplate } from '../../core/models/appraisal.model';
import { AuthService } from '../../core/services/auth.service';
import { DialogService } from '../../shared/services/dialog.service';
import { LoadingComponent } from '../../shared/components/loading/loading.component';

import { HeaderSectionComponent } from '../../form-renderer/sections/header-section.component';
import { RatingKeySectionComponent } from '../../form-renderer/sections/rating-key-section.component';
import { KeyResponsibilitiesSectionComponent } from '../../form-renderer/sections/key-responsibilities-section.component';
import { IdpSectionComponent } from '../../form-renderer/sections/idp-section.component';
import { PolicyAdherenceSectionComponent } from '../../form-renderer/sections/policy-adherence-section.component';
import { GoalsSectionComponent } from '../../form-renderer/sections/goals-section.component';
import { SignatureSectionComponent } from '../../form-renderer/sections/signature-section.component';
import { FormNavigationComponent } from '../../form-renderer/form-navigation.component';

/**
 * Manager Self-Appraisal Component
 *
 * Allows a Manager to complete their own self-appraisal following the same
 * workflow as an Employee (Requirement 6.9).
 *
 * The form is accessible under /manager/appraisal/:id so it stays within the
 * MANAGER role guard, avoiding the need for the manager to also hold the
 * EMPLOYEE role to access /employee/appraisal/:id.
 *
 * Requirements:
 * - Requirement 6.9: Manager can complete their own self-appraisal following
 *                    the same workflow as an Employee
 * - Requirement 5.3: Save form as draft
 * - Requirement 5.4: Submit completed form
 * - Requirement 5.6: Prevent editing after submission (unless reopened by HR)
 * - Requirement 5.7: View historical forms in read-only mode
 */
@Component({
  selector: 'app-manager-self-appraisal',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatIconModule,
    LoadingComponent,
    HeaderSectionComponent,
    RatingKeySectionComponent,
    KeyResponsibilitiesSectionComponent,
    IdpSectionComponent,
    PolicyAdherenceSectionComponent,
    GoalsSectionComponent,
    SignatureSectionComponent,
    FormNavigationComponent
  ],
  template: `
    <div class="form-container">
      <app-loading *ngIf="loading"></app-loading>

      <mat-card *ngIf="error && !loading" class="error-card">
        <mat-card-content>
          <p class="error-message">{{ error }}</p>
          <button mat-raised-button color="primary" (click)="loadForm()">Retry</button>
        </mat-card-content>
      </mat-card>

      <div *ngIf="appraisalForm && template && !loading" class="form-content">
        <!-- Form Header -->
        <mat-card class="form-header-card">
          <mat-card-header>
            <mat-card-title>My Self-Appraisal</mat-card-title>
            <mat-card-subtitle>{{ appraisalForm.cycleName }}</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            <div class="form-meta">
              <div class="meta-item">
                <span class="label">Status:</span>
                <span class="value status-badge" [class]="'status-' + appraisalForm.status.toLowerCase()">
                  {{ getStatusLabel(appraisalForm.status) }}
                </span>
              </div>
              <div class="meta-item" *ngIf="appraisalForm.submittedAt">
                <span class="label">Submitted:</span>
                <span class="value">{{ appraisalForm.submittedAt | date:'medium' }}</span>
              </div>
              <div class="meta-item" *ngIf="readonly">
                <span class="readonly-badge">Read-Only Mode</span>
              </div>
            </div>
          </mat-card-content>
        </mat-card>

        <!-- Dynamic Form Sections -->
        <form [formGroup]="formGroup" (ngSubmit)="onSubmit()">
          <app-header-section
            [formGroup]="formGroup"
            [formData]="formData"
            [readonly]="readonly"
            [employeeName]="appraisalForm.employeeName || ''"
            [managerName]="appraisalForm.managerName || ''"
            [designation]="appraisalForm.designation || ''">
          </app-header-section>

          <app-rating-key-section></app-rating-key-section>

          <app-key-responsibilities-section
            [formGroup]="formGroup"
            [items]="getKeyResponsibilitiesItems()"
            [readonly]="readonly"
            [canEditSelf]="canEditSelfFields"
            [canEditManager]="false">
          </app-key-responsibilities-section>

          <app-idp-section
            [formGroup]="formGroup"
            [items]="getIdpItems()"
            [readonly]="readonly"
            [canEditSelf]="canEditSelfFields"
            [canEditManager]="false">
          </app-idp-section>

          <app-policy-adherence-section
            [formGroup]="formGroup"
            [readonly]="readonly"
            [canEditManager]="false">
          </app-policy-adherence-section>

          <app-goals-section
            [formGroup]="formGroup"
            [items]="getGoalsItems()"
            [readonly]="readonly"
            [canEditSelf]="canEditSelfFields"
            [canEditManager]="false">
          </app-goals-section>

          <app-signature-section
            [formGroup]="formGroup"
            [readonly]="readonly"
            [canEditSelf]="canEditSelfFields"
            [canEditManager]="false">
          </app-signature-section>

          <app-form-navigation
            [canSaveDraft]="canSaveDraft"
            [canSubmit]="canSubmit"
            [readonly]="readonly"
            [saving]="saving"
            (saveDraft)="onSaveDraft()"
            (submit)="onSubmit()"
            (cancel)="onCancel()">
          </app-form-navigation>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .form-container {
      padding: 24px;
      max-width: 1200px;
      margin: 0 auto;
    }

    .error-card { margin-bottom: 24px; }

    .error-message {
      color: #d32f2f;
      margin-bottom: 16px;
    }

    .form-header-card { margin-bottom: 24px; }

    .form-meta {
      display: flex;
      gap: 24px;
      flex-wrap: wrap;
      margin-top: 16px;
    }

    .meta-item {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .meta-item .label {
      font-weight: 500;
      color: rgba(0, 0, 0, 0.6);
    }

    .status-badge {
      padding: 4px 12px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 500;
    }

    .status-badge.status-not_started       { background-color: #e0e0e0; color: #424242; }
    .status-badge.status-draft_saved       { background-color: #fff3e0; color: #e65100; }
    .status-badge.status-submitted         { background-color: #e3f2fd; color: #0d47a1; }
    .status-badge.status-under_review      { background-color: #f3e5f5; color: #4a148c; }
    .status-badge.status-reviewed_and_completed { background-color: #e8f5e9; color: #1b5e20; }

    .readonly-badge {
      padding: 4px 12px;
      border-radius: 12px;
      background-color: #fafafa;
      color: #616161;
      font-size: 12px;
      font-weight: 500;
      border: 1px solid #e0e0e0;
    }

    .form-content {
      animation: fadeIn 0.3s ease-in;
    }

    @keyframes fadeIn {
      from { opacity: 0; }
      to   { opacity: 1; }
    }
  `]
})
export class ManagerSelfAppraisalComponent implements OnInit, OnDestroy {
  appraisalForm: AppraisalForm | null = null;
  template: AppraisalTemplate | null = null;
  formData: FormData = {};
  formGroup: FormGroup = new FormGroup({});

  loading = false;
  saving = false;
  error: string | null = null;
  readonly = false;

  canEditSelfFields = false;
  canSaveDraft = false;
  canSubmit = false;

  private destroy$ = new Subject<void>();
  private formId: number | null = null;
  private autoSaveInterval: any;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient,
    private formRendererService: FormRendererService,
    private authService: AuthService,
    private dialogService: DialogService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe(params => {
      this.readonly = params['readonly'] === 'true';
    });

    this.route.params.pipe(takeUntil(this.destroy$)).subscribe(params => {
      this.formId = +params['id'];
      if (this.formId) {
        this.loadForm();
      }
    });

    // Auto-save draft every 2 minutes while editing
    this.autoSaveInterval = setInterval(() => {
      if (!this.readonly && this.canSaveDraft && this.formGroup.dirty) {
        this.onSaveDraft(true);
      }
    }, 120000);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    clearInterval(this.autoSaveInterval);
  }

  loadForm(): void {
    if (!this.formId) return;
    this.loading = true;
    this.error = null;

    this.http.get<AppraisalForm>(`/api/forms/${this.formId}`)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (form) => {
          this.appraisalForm = form;
          this.formData = form.formData || {};
          this.loadTemplate(form.templateId);
          this.updatePermissions();
        },
        error: () => {
          this.error = 'Failed to load appraisal form. Please try again.';
          this.loading = false;
        }
      });
  }

  private loadTemplate(templateId: number): void {
    this.http.get<AppraisalTemplate>(`/api/templates/${templateId}`)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (template) => {
          this.template = template;
          this.initializeForm();
          this.loading = false;
        },
        error: () => {
          this.error = 'Failed to load form template. Please try again.';
          this.loading = false;
        }
      });
  }

  private initializeForm(): void {
    if (!this.template || !this.appraisalForm) return;

    const schema = this.formRendererService.parseTemplateSchema(this.template.schemaJson);

    const controls: Record<string, any> = {
      header: new FormGroup({
        dateOfHire:   new FormControl(this.formData.header?.dateOfHire || ''),
        dateOfReview: new FormControl(this.formData.header?.dateOfReview || ''),
        reviewPeriod: new FormControl(this.formData.header?.reviewPeriod || ''),
        typeOfReview: new FormControl(this.formData.header?.typeOfReview || 'Annual')
      })
    };

    const krSection = schema.sections.find(s => s.sectionType === 'key_responsibilities');
    if (krSection?.items) {
      controls['keyResponsibilities'] = new FormArray(
        krSection.items.map((item, idx) => {
          const existing = this.formData.keyResponsibilities?.[idx];
          return new FormGroup({
            itemId:      new FormControl(item.id),
            selfComment: new FormControl(existing?.selfComment || ''),
            selfRating:  new FormControl(existing?.selfRating || ''),
            managerComment: new FormControl({ value: existing?.managerComment || '', disabled: true }),
            managerRating:  new FormControl({ value: existing?.managerRating || '', disabled: true })
          });
        })
      );
    }

    const idpSection = schema.sections.find(s => s.sectionType === 'idp');
    if (idpSection?.items) {
      controls['idp'] = new FormArray(
        idpSection.items.map((item, idx) => {
          const existing = this.formData.idp?.[idx];
          return new FormGroup({
            itemId:      new FormControl(item.id),
            selfComment: new FormControl(existing?.selfComment || ''),
            selfRating:  new FormControl(existing?.selfRating || ''),
            managerComment: new FormControl({ value: existing?.managerComment || '', disabled: true }),
            managerRating:  new FormControl({ value: existing?.managerRating || '', disabled: true })
          });
        })
      );
    }

    const goalsSection = schema.sections.find(s => s.sectionType === 'goals');
    if (goalsSection?.items) {
      controls['goals'] = new FormArray(
        goalsSection.items.map((item, idx) => {
          const existing = this.formData.goals?.[idx];
          return new FormGroup({
            itemId:      new FormControl(item.id),
            selfComment: new FormControl(existing?.selfComment || ''),
            selfRating:  new FormControl(existing?.selfRating || ''),
            managerComment: new FormControl({ value: existing?.managerComment || '', disabled: true }),
            managerRating:  new FormControl({ value: existing?.managerRating || '', disabled: true })
          });
        })
      );
    }

    controls['nextYearGoals']     = new FormControl(this.formData.nextYearGoals || '');
    controls['teamMemberComments'] = new FormControl(this.formData.overallEvaluation?.teamMemberComments || '');

    this.formGroup = new FormGroup(controls);
  }

  private updatePermissions(): void {
    if (!this.appraisalForm) return;

    const currentUser = this.authService.currentUserValue;
    const isOwner = currentUser?.id === this.appraisalForm.employeeId;

    const editability = this.formRendererService.getFieldEditability(
      isOwner,
      false,
      this.appraisalForm.status
    );

    this.canEditSelfFields = editability.selfCommentEditable && !this.readonly;
    this.canSaveDraft      = this.canEditSelfFields;
    this.canSubmit         = this.canEditSelfFields;
  }

  getKeyResponsibilitiesItems(): any[] {
    if (!this.template) return [];
    const schema = this.formRendererService.parseTemplateSchema(this.template.schemaJson);
    return schema.sections.find(s => s.sectionType === 'key_responsibilities')?.items || [];
  }

  getIdpItems(): any[] {
    if (!this.template) return [];
    const schema = this.formRendererService.parseTemplateSchema(this.template.schemaJson);
    return schema.sections.find(s => s.sectionType === 'idp')?.items || [];
  }

  getGoalsItems(): any[] {
    if (!this.template) return [];
    const schema = this.formRendererService.parseTemplateSchema(this.template.schemaJson);
    return schema.sections.find(s => s.sectionType === 'goals')?.items || [];
  }

  onSaveDraft(isAutoSave = false): void {
    if (!this.canSaveDraft || this.saving) return;

    this.saving = true;
    const draftData = this.collectFormData();

    this.http.put(`/api/forms/${this.formId}/draft`, { formData: draftData })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.saving = false;
          this.formGroup.markAsPristine();
          if (!isAutoSave) {
            this.snackBar.open('Draft saved successfully', 'Close', { duration: 3000 });
          }
        },
        error: () => {
          this.saving = false;
          this.snackBar.open('Failed to save draft', 'Close', { duration: 3000 });
        }
      });
  }

  async onSubmit(): Promise<void> {
    if (!this.canSubmit || this.saving) return;

    const draftData = this.collectFormData();
    const validation = this.formRendererService.validateForEmployeeSubmission(draftData);
    if (!validation.valid) {
      this.snackBar.open('Please complete all required fields before submitting', 'Close', { duration: 5000 });
      return;
    }

    const confirmed = await this.dialogService.confirm(
      'Submit Self-Appraisal',
      'Are you sure you want to submit your self-appraisal? You will not be able to edit it after submission.',
      'Submit',
      'Cancel'
    );

    if (!confirmed) return;

    this.saving = true;

    this.http.put(`/api/forms/${this.formId}/draft`, { formData: draftData })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.http.post(`/api/forms/${this.formId}/submit`, {})
            .pipe(takeUntil(this.destroy$))
            .subscribe({
              next: () => {
                this.saving = false;
                this.formGroup.markAsPristine();
                this.snackBar.open('Self-appraisal submitted successfully', 'Close', { duration: 3000 });
                this.router.navigate(['/manager/dashboard']);
              },
              error: () => {
                this.saving = false;
                this.snackBar.open('Failed to submit self-appraisal', 'Close', { duration: 3000 });
              }
            });
        },
        error: () => {
          this.saving = false;
          this.snackBar.open('Failed to save form data before submission', 'Close', { duration: 3000 });
        }
      });
  }

  onCancel(): void {
    if (this.formGroup.dirty) {
      this.dialogService.confirm(
        'Unsaved Changes',
        'You have unsaved changes. Are you sure you want to leave?',
        'Leave',
        'Stay'
      ).then(confirmed => {
        if (confirmed) this.router.navigate(['/manager/dashboard']);
      });
    } else {
      this.router.navigate(['/manager/dashboard']);
    }
  }

  private collectFormData(): FormData {
    const v = this.formGroup.value;
    return {
      header: {
        dateOfHire:   v.header?.dateOfHire || '',
        dateOfReview: v.header?.dateOfReview || '',
        reviewPeriod: v.header?.reviewPeriod || '',
        typeOfReview: v.header?.typeOfReview || 'Annual'
      },
      keyResponsibilities: (v.keyResponsibilities || []).map((item: any) => ({
        itemId:      item.itemId,
        selfComment: item.selfComment || '',
        selfRating:  item.selfRating || ''
      })),
      idp: (v.idp || []).map((item: any) => ({
        itemId:      item.itemId,
        selfComment: item.selfComment || '',
        selfRating:  item.selfRating || ''
      })),
      goals: (v.goals || []).map((item: any) => ({
        itemId:      item.itemId,
        selfComment: item.selfComment || '',
        selfRating:  item.selfRating || ''
      })),
      nextYearGoals:     v.nextYearGoals || '',
      overallEvaluation: { teamMemberComments: v.teamMemberComments || '' }
    };
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      'NOT_STARTED':            'Not Started',
      'DRAFT_SAVED':            'Draft Saved',
      'SUBMITTED':              'Submitted',
      'UNDER_REVIEW':           'Under Review',
      'REVIEW_DRAFT_SAVED':     'Review in Progress',
      'REVIEWED_AND_COMPLETED': 'Completed'
    };
    return labels[status] || status;
  }
}
