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
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Subject, takeUntil } from 'rxjs';
import { HttpClient } from '@angular/common/http';

import { FormRendererService } from '../../form-renderer/form-renderer.service';
import { AppraisalForm, FormData, AppraisalTemplate, FormStatus } from '../../core/models/appraisal.model';
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

import { FieldEditability } from '../../form-renderer/form-renderer.models';

/**
 * Review Form Component — allows a Manager (or Backup Reviewer) to review
 * a submitted employee appraisal form.
 *
 * Requirements:
 * - Requirement 6: Manager Review
 * - 6.2: Manager can add comments and ratings in all Manager-designated fields
 * - 6.3: Manager can save review as draft (→ REVIEW_DRAFT_SAVED)
 * - 6.4: Manager can complete review (→ REVIEWED_AND_COMPLETED)
 * - 6.5: Completion triggers notification email with PDF attachment
 * - 6.7: Employee self-appraisal fields are visible in read-only mode
 * - 6.8: Status tracking: Under Review, Review Draft Saved, Reviewed and Completed
 * - Requirement 15.3: Backup reviewer has same permissions as primary manager
 */
@Component({
  selector: 'app-review-form',
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
    MatDividerModule,
    MatProgressBarModule,
    LoadingComponent,
    HeaderSectionComponent,
    RatingKeySectionComponent,
    KeyResponsibilitiesSectionComponent,
    IdpSectionComponent,
    PolicyAdherenceSectionComponent,
    GoalsSectionComponent,
    SignatureSectionComponent
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
        <!-- Form Header Card -->
        <mat-card class="form-header-card">
          <mat-card-header>
            <mat-card-title>Manager Review</mat-card-title>
            <mat-card-subtitle>{{ appraisalForm.cycleName }}</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            <div class="form-meta">
              <div class="meta-item">
                <span class="label">Employee:</span>
                <span class="value">{{ appraisalForm.employeeName }}</span>
              </div>
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
        <form [formGroup]="formGroup">
          <app-header-section
            [formGroup]="formGroup"
            [formData]="formData"
            [readonly]="true"
            [employeeName]="appraisalForm.employeeName || ''"
            [managerName]="appraisalForm.managerName || ''"
            [designation]="appraisalForm.designation || ''">
          </app-header-section>

          <app-rating-key-section></app-rating-key-section>

          <!-- Overall Evaluation — manager comments + employee comments (read-only) -->
          <mat-card class="section-card">
            <mat-card-header>
              <mat-card-title>Overall Evaluation</mat-card-title>
            </mat-card-header>
            <mat-card-content [formGroup]="overallEvaluationGroup">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Manager's Comments</mat-label>
                <textarea
                  matInput
                  formControlName="managerComments"
                  rows="4"
                  [readonly]="readonly || !canEditManagerFields">
                </textarea>
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Team Member's Comments (read-only)</mat-label>
                <textarea
                  matInput
                  formControlName="teamMemberComments"
                  rows="4"
                  readonly>
                </textarea>
              </mat-form-field>
            </mat-card-content>
          </mat-card>

          <app-key-responsibilities-section
            [formGroup]="formGroup"
            [items]="getKeyResponsibilitiesItems()"
            [readonly]="readonly"
            [canEditSelf]="false"
            [canEditManager]="canEditManagerFields">
          </app-key-responsibilities-section>

          <app-idp-section
            [formGroup]="formGroup"
            [items]="getIdpItems()"
            [readonly]="readonly"
            [canEditSelf]="false"
            [canEditManager]="canEditManagerFields">
          </app-idp-section>

          <app-policy-adherence-section
            [formGroup]="formGroup"
            [items]="getPolicyAdherenceItems()"
            [editability]="fieldEditability">
          </app-policy-adherence-section>

          <app-goals-section
            [formGroup]="formGroup"
            [items]="getGoalsItems()"
            [readonly]="readonly"
            [canEditSelf]="false"
            [canEditManager]="canEditManagerFields">
          </app-goals-section>

          <app-signature-section
            [formGroup]="formGroup"
            [editability]="fieldEditability">
          </app-signature-section>

          <!-- Action Bar -->
          <mat-card class="action-bar-card" *ngIf="!readonly">
            <mat-card-content>
              <div class="action-bar">
                <button mat-stroked-button (click)="onCancel()" [disabled]="saving">
                  <mat-icon>arrow_back</mat-icon>
                  Back to Dashboard
                </button>
                <div class="primary-actions">
                  <button
                    mat-stroked-button
                    color="primary"
                    *ngIf="canSaveReviewDraft"
                    (click)="onSaveDraft()"
                    [disabled]="saving">
                    <mat-icon>save</mat-icon>
                    {{ saving ? 'Saving...' : 'Save Draft' }}
                  </button>
                  <button
                    mat-raised-button
                    color="primary"
                    *ngIf="canCompleteReview"
                    (click)="onCompleteReview()"
                    [disabled]="saving">
                    <mat-icon>check_circle</mat-icon>
                    {{ saving ? 'Completing...' : 'Complete Review' }}
                  </button>
                </div>
              </div>
            </mat-card-content>
          </mat-card>

          <!-- Read-only back button -->
          <div class="readonly-actions" *ngIf="readonly">
            <button mat-stroked-button (click)="onCancel()">
              <mat-icon>arrow_back</mat-icon>
              Back to Dashboard
            </button>
          </div>
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

    .form-header-card,
    .section-card {
      margin-bottom: 24px;
    }

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
      color: rgba(0,0,0,0.6);
    }

    .status-badge {
      padding: 4px 12px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 500;
    }

    .status-badge.status-submitted       { background-color: #e3f2fd; color: #0d47a1; }
    .status-badge.status-under_review    { background-color: #f3e5f5; color: #4a148c; }
    .status-badge.status-review_draft_saved { background-color: #fff3e0; color: #e65100; }
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

    .full-width { width: 100%; }

    .form-content {
      animation: fadeIn 0.3s ease-in;
    }

    @keyframes fadeIn {
      from { opacity: 0; }
      to   { opacity: 1; }
    }

    .action-bar-card {
      margin-bottom: 24px;
      position: sticky;
      bottom: 16px;
      z-index: 10;
      box-shadow: 0 -2px 8px rgba(0,0,0,0.12);
    }

    .action-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;
    }

    .primary-actions {
      display: flex;
      gap: 12px;
    }

    .readonly-actions {
      margin-bottom: 24px;
    }
  `]
})
export class ReviewFormComponent implements OnInit, OnDestroy {
  appraisalForm: AppraisalForm | null = null;
  template: AppraisalTemplate | null = null;
  formData: FormData = {};
  formGroup: FormGroup = new FormGroup({});

  loading = false;
  saving = false;
  error: string | null = null;
  readonly = false;

  canEditManagerFields = false;
  canSaveReviewDraft = false;
  canCompleteReview = false;

  fieldEditability: FieldEditability = {
    selfCommentEditable: false,
    selfRatingEditable: false,
    managerCommentEditable: false,
    managerRatingEditable: false
  };

  private destroy$ = new Subject<void>();
  private formId: number | null = null;

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
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
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
      }),
      overallEvaluation: new FormGroup({
        managerComments:    new FormControl(this.formData.overallEvaluation?.managerComments || ''),
        teamMemberComments: new FormControl({ value: this.formData.overallEvaluation?.teamMemberComments || '', disabled: true })
      })
    };

    // Key Responsibilities
    const krSection = schema.sections.find(s => s.sectionType === 'key_responsibilities');
    if (krSection?.items) {
      controls['keyResponsibilities'] = new FormArray(
        krSection.items.map((item, idx) => {
          const existing = this.formData.keyResponsibilities?.[idx];
          return new FormGroup({
            itemId:         new FormControl(item.id),
            selfComment:    new FormControl({ value: existing?.selfComment || '', disabled: true }),
            selfRating:     new FormControl({ value: existing?.selfRating || '', disabled: true }),
            managerComment: new FormControl(existing?.managerComment || ''),
            managerRating:  new FormControl(existing?.managerRating || '')
          });
        })
      );
    }

    // IDP
    const idpSection = schema.sections.find(s => s.sectionType === 'idp');
    if (idpSection?.items) {
      controls['idp'] = new FormArray(
        idpSection.items.map((item, idx) => {
          const existing = this.formData.idp?.[idx];
          return new FormGroup({
            itemId:         new FormControl(item.id),
            selfComment:    new FormControl({ value: existing?.selfComment || '', disabled: true }),
            selfRating:     new FormControl({ value: existing?.selfRating || '', disabled: true }),
            managerComment: new FormControl(existing?.managerComment || ''),
            managerRating:  new FormControl(existing?.managerRating || '')
          });
        })
      );
    }

    // Goals
    const goalsSection = schema.sections.find(s => s.sectionType === 'goals');
    if (goalsSection?.items) {
      controls['goals'] = new FormArray(
        goalsSection.items.map((item, idx) => {
          const existing = this.formData.goals?.[idx];
          return new FormGroup({
            itemId:         new FormControl(item.id),
            selfComment:    new FormControl({ value: existing?.selfComment || '', disabled: true }),
            selfRating:     new FormControl({ value: existing?.selfRating || '', disabled: true }),
            managerComment: new FormControl(existing?.managerComment || ''),
            managerRating:  new FormControl(existing?.managerRating || '')
          });
        })
      );
    }

    controls['nextYearGoals'] = new FormControl({ value: this.formData.nextYearGoals || '', disabled: true });

    // Policy Adherence
    controls['policyAdherence'] = new FormGroup({
      hrPolicy:         new FormGroup({ managerRating: new FormControl(this.formData.policyAdherence?.hrPolicy?.managerRating ?? null) }),
      availability:     new FormGroup({ managerRating: new FormControl(this.formData.policyAdherence?.availability?.managerRating ?? null) }),
      additionalSupport:new FormGroup({ managerRating: new FormControl(this.formData.policyAdherence?.additionalSupport?.managerRating ?? null) }),
      managerComments:  new FormControl(this.formData.policyAdherence?.managerComments || '')
    });

    // Signature
    controls['signature'] = new FormGroup({
      preparedBy:               new FormControl(this.formData.signature?.preparedBy || ''),
      reviewedBy:               new FormControl(this.formData.signature?.reviewedBy || ''),
      teamMemberAcknowledgement:new FormControl({ value: this.formData.signature?.teamMemberAcknowledgement || '', disabled: true })
    });

    this.formGroup = new FormGroup(controls);
  }

  private updatePermissions(): void {
    if (!this.appraisalForm) return;

    const currentUser = this.authService.currentUserValue;
    const isPrimaryManager = currentUser?.id === this.appraisalForm.managerId;
    const isBackupReviewer = currentUser?.id === this.appraisalForm.backupReviewerId;
    const isManager = isPrimaryManager || isBackupReviewer;

    const editability = this.formRendererService.getFieldEditability(
      false,
      isManager,
      this.appraisalForm.status
    );

    this.canEditManagerFields = editability.managerCommentEditable && !this.readonly;

    // Save draft is only available when the form is actively under review
    const saveDraftStatuses: FormStatus[] = ['UNDER_REVIEW', 'REVIEW_DRAFT_SAVED'];
    this.canSaveReviewDraft = this.canEditManagerFields &&
      saveDraftStatuses.includes(this.appraisalForm.status);

    this.canCompleteReview = this.canEditManagerFields;

    this.fieldEditability = {
      selfCommentEditable: false,
      selfRatingEditable: false,
      managerCommentEditable: this.canEditManagerFields,
      managerRatingEditable: this.canEditManagerFields
    };
  }

  get overallEvaluationGroup(): FormGroup {
    return this.formGroup.get('overallEvaluation') as FormGroup;
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

  getPolicyAdherenceItems(): any[] {
    if (!this.template) return [];
    const schema = this.formRendererService.parseTemplateSchema(this.template.schemaJson);
    return schema.sections.find(s => s.sectionType === 'policy_adherence')?.items || [];
  }

  onSaveDraft(): void {
    if (!this.canSaveReviewDraft || this.saving) return;

    this.saving = true;
    const reviewData = this.collectReviewData();

    this.http.put(`/api/forms/${this.formId}/review/draft`, { formData: reviewData })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.saving = false;
          this.formGroup.markAsPristine();
          // Update local status to reflect the transition
          if (this.appraisalForm) {
            this.appraisalForm.status = 'REVIEW_DRAFT_SAVED';
            this.updatePermissions();
          }
          this.snackBar.open('Review draft saved', 'Close', { duration: 3000 });
        },
        error: () => {
          this.saving = false;
          this.snackBar.open('Failed to save review draft', 'Close', { duration: 3000 });
        }
      });
  }

  async onCompleteReview(): Promise<void> {
    if (!this.canCompleteReview || this.saving) return;

    const reviewData = this.collectReviewData();
    const validation = this.formRendererService.validateForManagerCompletion(reviewData as FormData);
    if (!validation.valid) {
      this.snackBar.open('Please complete all required manager fields before completing the review', 'Close', { duration: 5000 });
      return;
    }

    const confirmed = await this.dialogService.confirm(
      'Complete Review',
      'Are you sure you want to complete this review? A PDF will be generated and notifications will be sent to the employee, manager, and HR.',
      'Complete Review',
      'Cancel'
    );

    if (!confirmed) return;

    this.saving = true;

    // Save draft first, then complete
    this.http.put(`/api/forms/${this.formId}/review/draft`, { formData: reviewData })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.http.post(`/api/forms/${this.formId}/review/complete`, {})
            .pipe(takeUntil(this.destroy$))
            .subscribe({
              next: () => {
                this.saving = false;
                this.snackBar.open('Review completed successfully. Notifications have been sent.', 'Close', { duration: 4000 });
                this.router.navigate(['/manager/dashboard']);
              },
              error: () => {
                this.saving = false;
                this.snackBar.open('Failed to complete review', 'Close', { duration: 3000 });
              }
            });
        },
        error: () => {
          this.saving = false;
          this.snackBar.open('Failed to save review data', 'Close', { duration: 3000 });
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

  private collectReviewData(): Partial<FormData> {
    const v = this.formGroup.getRawValue();

    return {
      overallEvaluation: {
        managerComments:    v.overallEvaluation?.managerComments || '',
        teamMemberComments: v.overallEvaluation?.teamMemberComments || ''
      },
      keyResponsibilities: (v.keyResponsibilities || []).map((item: any) => ({
        itemId:         item.itemId,
        selfComment:    item.selfComment || '',
        selfRating:     item.selfRating || '',
        managerComment: item.managerComment || '',
        managerRating:  item.managerRating || ''
      })),
      idp: (v.idp || []).map((item: any) => ({
        itemId:         item.itemId,
        selfComment:    item.selfComment || '',
        selfRating:     item.selfRating || '',
        managerComment: item.managerComment || '',
        managerRating:  item.managerRating || ''
      })),
      goals: (v.goals || []).map((item: any) => ({
        itemId:         item.itemId,
        selfComment:    item.selfComment || '',
        selfRating:     item.selfRating || '',
        managerComment: item.managerComment || '',
        managerRating:  item.managerRating || ''
      })),
      policyAdherence: {
        hrPolicy:          { managerRating: v.policyAdherence?.hrPolicy?.managerRating },
        availability:      { managerRating: v.policyAdherence?.availability?.managerRating },
        additionalSupport: { managerRating: v.policyAdherence?.additionalSupport?.managerRating },
        managerComments:   v.policyAdherence?.managerComments || ''
      },
      signature: {
        preparedBy:                v.signature?.preparedBy || '',
        reviewedBy:                v.signature?.reviewedBy || '',
        teamMemberAcknowledgement: v.signature?.teamMemberAcknowledgement || ''
      }
    };
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      'SUBMITTED':              'Submitted',
      'UNDER_REVIEW':           'Under Review',
      'REVIEW_DRAFT_SAVED':     'Review in Progress',
      'REVIEWED_AND_COMPLETED': 'Completed'
    };
    return labels[status] || status;
  }
}
