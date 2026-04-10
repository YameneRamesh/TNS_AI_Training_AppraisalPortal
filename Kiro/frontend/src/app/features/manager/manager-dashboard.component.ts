import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DashboardService } from '../../core/services/dashboard.service';
import { AuthService } from '../../core/services/auth.service';
import { ManagerDashboard, TeamMemberForm, AppraisalFormSummary } from '../../core/models/dashboard.model';
import { User } from '../../core/models/user.model';

/**
 * Manager dashboard component displaying own appraisal status and team review progress.
 *
 * Requirements:
 * - Requirement 8: Manager Dashboard
 * - 8.1: Display manager's own current Appraisal_Form and status
 * - 8.2: Display list of direct reportees with Appraisal_Form status
 * - 8.3: Display count of pending and completed reviews
 * - 8.4: Display team's overall appraisal completion percentage
 * - 8.5: Provide hyperlink from each team member entry to their Appraisal_Form
 * - 8.6: Display historical Appraisal_Cycles (read-only access)
 */
@Component({
  selector: 'app-manager-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatTableModule,
    MatChipsModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatTooltipModule
  ],
  template: `
    <div class="dashboard-container">
      <!-- Header -->
      <div class="dashboard-header">
        <h1>Manager Dashboard</h1>
        <p *ngIf="currentUser">Welcome, {{ currentUser.fullName }}</p>
      </div>

      <!-- Loading State -->
      <div *ngIf="loading" class="loading-container">
        <mat-spinner></mat-spinner>
      </div>

      <!-- Error State -->
      <mat-card *ngIf="error && !loading" class="error-card">
        <mat-card-content>
          <p class="error-message">{{ error }}</p>
          <button mat-raised-button color="primary" (click)="loadDashboard()">Retry</button>
        </mat-card-content>
      </mat-card>

      <ng-container *ngIf="dashboard && !loading">

        <!-- Summary Metrics (Req 8.3, 8.4) -->
        <div class="metrics-row">
          <mat-card class="metric-card">
            <mat-card-content>
              <div class="metric-value">{{ dashboard.pendingReviews }}</div>
              <div class="metric-label">Pending Reviews</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="metric-card">
            <mat-card-content>
              <div class="metric-value">{{ dashboard.completedReviews }}</div>
              <div class="metric-label">Completed Reviews</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="metric-card completion-card">
            <mat-card-content>
              <div class="metric-value">{{ dashboard.completionPercentage | number:'1.0-0' }}%</div>
              <div class="metric-label">Team Completion</div>
              <mat-progress-bar
                mode="determinate"
                [value]="dashboard.completionPercentage"
                class="completion-bar">
              </mat-progress-bar>
            </mat-card-content>
          </mat-card>
        </div>

        <!-- Manager's Own Appraisal (Req 8.1) -->
        <mat-card class="own-form-card">
          <mat-card-header>
            <mat-card-title>My Appraisal</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div *ngIf="dashboard.ownForm; else noOwnForm">
              <div class="form-info">
                <div class="info-row">
                  <span class="label">Cycle:</span>
                  <span class="value">{{ dashboard.ownForm.cycleName }}</span>
                </div>
                <div class="info-row">
                  <span class="label">Status:</span>
                  <mat-chip [class]="getStatusClass(dashboard.ownForm.status)">
                    {{ getStatusLabel(dashboard.ownForm.status) }}
                  </mat-chip>
                </div>
                <div class="info-row" *ngIf="dashboard.ownForm.submittedAt">
                  <span class="label">Submitted:</span>
                  <span class="value">{{ dashboard.ownForm.submittedAt | date:'medium' }}</span>
                </div>
                <div class="info-row" *ngIf="dashboard.ownForm.reviewedAt">
                  <span class="label">Reviewed:</span>
                  <span class="value">{{ dashboard.ownForm.reviewedAt | date:'medium' }}</span>
                </div>
              </div>
              <div class="form-actions">
                <button
                  mat-raised-button
                  color="primary"
                  *ngIf="canEditOwnForm(dashboard.ownForm.status)"
                  (click)="openOwnForm(dashboard.ownForm.id)">
                  <mat-icon>edit</mat-icon>
                  {{ getOwnFormActionLabel(dashboard.ownForm.status) }}
                </button>
                <button
                  mat-raised-button
                  *ngIf="canViewOwnForm(dashboard.ownForm.status)"
                  (click)="viewOwnForm(dashboard.ownForm.id)">
                  <mat-icon>visibility</mat-icon>
                  View Form
                </button>
                <button
                  mat-raised-button
                  *ngIf="dashboard.ownForm.pdfAvailable"
                  (click)="downloadPdf(dashboard.ownForm.id)">
                  <mat-icon>download</mat-icon>
                  Download PDF
                </button>
              </div>
            </div>
            <ng-template #noOwnForm>
              <p class="no-data">No active appraisal cycle at this time.</p>
            </ng-template>
          </mat-card-content>
        </mat-card>

        <!-- Team Appraisals (Req 8.2, 8.5) -->
        <mat-card class="team-card">
          <mat-card-header>
            <mat-card-title>Team Appraisals</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div *ngIf="dashboard.teamForms.length > 0; else noTeamForms">
              <table mat-table [dataSource]="dashboard.teamForms" class="team-table">

                <!-- Employee Name Column -->
                <ng-container matColumnDef="employeeName">
                  <th mat-header-cell *matHeaderCellDef>Team Member</th>
                  <td mat-cell *matCellDef="let member">{{ member.employeeName }}</td>
                </ng-container>

                <!-- Designation Column -->
                <ng-container matColumnDef="designation">
                  <th mat-header-cell *matHeaderCellDef>Designation</th>
                  <td mat-cell *matCellDef="let member">{{ member.designation }}</td>
                </ng-container>

                <!-- Status Column -->
                <ng-container matColumnDef="status">
                  <th mat-header-cell *matHeaderCellDef>Status</th>
                  <td mat-cell *matCellDef="let member">
                    <mat-chip [class]="getStatusClass(member.status)">
                      {{ getStatusLabel(member.status) }}
                    </mat-chip>
                  </td>
                </ng-container>

                <!-- Submitted Date Column -->
                <ng-container matColumnDef="submittedAt">
                  <th mat-header-cell *matHeaderCellDef>Submitted</th>
                  <td mat-cell *matCellDef="let member">
                    {{ member.submittedAt ? (member.submittedAt | date:'shortDate') : '-' }}
                  </td>
                </ng-container>

                <!-- Actions Column (Req 8.5 - hyperlink to form) -->
                <ng-container matColumnDef="actions">
                  <th mat-header-cell *matHeaderCellDef>Actions</th>
                  <td mat-cell *matCellDef="let member">
                    <button
                      mat-icon-button
                      color="primary"
                      *ngIf="canReviewForm(member.status)"
                      (click)="reviewForm(member.formId)"
                      matTooltip="Review Form">
                      <mat-icon>rate_review</mat-icon>
                    </button>
                    <button
                      mat-icon-button
                      (click)="viewTeamMemberForm(member.formId)"
                      matTooltip="View Form">
                      <mat-icon>visibility</mat-icon>
                    </button>
                  </td>
                </ng-container>

                <tr mat-header-row *matHeaderRowDef="teamTableColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: teamTableColumns;"></tr>
              </table>
            </div>
            <ng-template #noTeamForms>
              <p class="no-data">No team members have active appraisal forms.</p>
            </ng-template>
          </mat-card-content>
        </mat-card>

        <!-- Historical Appraisal Cycles (Req 8.6) -->
        <mat-card class="history-card" *ngIf="dashboard.historicalForms && dashboard.historicalForms.length > 0">
          <mat-card-header>
            <mat-card-title>Historical Appraisals</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <table mat-table [dataSource]="dashboard.historicalForms" class="history-table">

              <!-- Cycle Column -->
              <ng-container matColumnDef="cycle">
                <th mat-header-cell *matHeaderCellDef>Cycle</th>
                <td mat-cell *matCellDef="let form">{{ form.cycleName }}</td>
              </ng-container>

              <!-- Status Column -->
              <ng-container matColumnDef="status">
                <th mat-header-cell *matHeaderCellDef>Status</th>
                <td mat-cell *matCellDef="let form">
                  <mat-chip [class]="getStatusClass(form.status)">
                    {{ getStatusLabel(form.status) }}
                  </mat-chip>
                </td>
              </ng-container>

              <!-- Reviewed Date Column -->
              <ng-container matColumnDef="reviewedAt">
                <th mat-header-cell *matHeaderCellDef>Reviewed</th>
                <td mat-cell *matCellDef="let form">
                  {{ form.reviewedAt ? (form.reviewedAt | date:'shortDate') : '-' }}
                </td>
              </ng-container>

              <!-- Actions Column -->
              <ng-container matColumnDef="actions">
                <th mat-header-cell *matHeaderCellDef>Actions</th>
                <td mat-cell *matCellDef="let form">
                  <button mat-icon-button (click)="viewOwnForm(form.id)" matTooltip="View (Read-only)">
                    <mat-icon>visibility</mat-icon>
                  </button>
                  <button
                    mat-icon-button
                    *ngIf="form.pdfAvailable"
                    (click)="downloadPdf(form.id)"
                    matTooltip="Download PDF">
                    <mat-icon>download</mat-icon>
                  </button>
                </td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="historyTableColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: historyTableColumns;"></tr>
            </table>
          </mat-card-content>
        </mat-card>

      </ng-container>
    </div>
  `,
  styles: [`
    .dashboard-container {
      padding: 24px;
      max-width: 1200px;
      margin: 0 auto;
    }

    .dashboard-header {
      margin-bottom: 24px;
    }

    .dashboard-header h1 {
      margin: 0 0 8px 0;
      font-size: 28px;
      font-weight: 500;
    }

    .dashboard-header p {
      margin: 0;
      color: rgba(0, 0, 0, 0.6);
    }

    .metrics-row {
      display: flex;
      gap: 16px;
      margin-bottom: 24px;
      flex-wrap: wrap;
    }

    .metric-card {
      flex: 1;
      min-width: 160px;
      text-align: center;
    }

    .metric-value {
      font-size: 36px;
      font-weight: 600;
      color: #1976d2;
      line-height: 1.2;
    }

    .metric-label {
      font-size: 13px;
      color: rgba(0, 0, 0, 0.6);
      margin-top: 4px;
    }

    .completion-bar {
      margin-top: 10px;
      border-radius: 4px;
    }

    .own-form-card,
    .team-card,
    .history-card,
    .error-card {
      margin-bottom: 24px;
    }

    .form-info {
      margin-bottom: 16px;
    }

    .info-row {
      display: flex;
      align-items: center;
      margin-bottom: 12px;
    }

    .info-row .label {
      font-weight: 500;
      min-width: 120px;
      color: rgba(0, 0, 0, 0.6);
    }

    .info-row .value {
      color: rgba(0, 0, 0, 0.87);
    }

    .form-actions {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
    }

    .form-actions button {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .team-table,
    .history-table {
      width: 100%;
    }

    .no-data {
      color: rgba(0, 0, 0, 0.6);
      font-style: italic;
    }

    mat-chip {
      font-size: 12px;
      min-height: 24px;
    }

    mat-chip.status-not-started { background-color: #e0e0e0; }
    mat-chip.status-draft { background-color: #fff3e0; }
    mat-chip.status-submitted { background-color: #e3f2fd; }
    mat-chip.status-under-review { background-color: #f3e5f5; }
    mat-chip.status-completed { background-color: #e8f5e9; }

    .loading-container {
      display: flex;
      justify-content: center;
      padding: 48px;
    }

    .error-message {
      color: #d32f2f;
      margin-bottom: 16px;
    }
  `]
})
export class ManagerDashboardComponent implements OnInit {
  dashboard: ManagerDashboard | null = null;
  currentUser: User | null = null;
  loading = false;
  error: string | null = null;

  teamTableColumns = ['employeeName', 'designation', 'status', 'submittedAt', 'actions'];
  historyTableColumns = ['cycle', 'status', 'reviewedAt', 'actions'];

  constructor(
    private dashboardService: DashboardService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.currentUserValue;
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading = true;
    this.error = null;

    this.dashboardService.getManagerDashboard().subscribe({
      next: (data) => {
        this.dashboard = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load dashboard data. Please try again.';
        this.loading = false;
        console.error('Manager dashboard load error:', err);
      }
    });
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      'NOT_STARTED': 'Not Started',
      'DRAFT_SAVED': 'Draft Saved',
      'SUBMITTED': 'Submitted',
      'UNDER_REVIEW': 'Under Review',
      'REVIEW_DRAFT_SAVED': 'Review in Progress',
      'REVIEWED_AND_COMPLETED': 'Completed'
    };
    return labels[status] || status;
  }

  getStatusClass(status: string): string {
    const classes: Record<string, string> = {
      'NOT_STARTED': 'status-not-started',
      'DRAFT_SAVED': 'status-draft',
      'SUBMITTED': 'status-submitted',
      'UNDER_REVIEW': 'status-under-review',
      'REVIEW_DRAFT_SAVED': 'status-under-review',
      'REVIEWED_AND_COMPLETED': 'status-completed'
    };
    return classes[status] || '';
  }

  canEditOwnForm(status: string): boolean {
    return status === 'NOT_STARTED' || status === 'DRAFT_SAVED';
  }

  canViewOwnForm(status: string): boolean {
    return ['SUBMITTED', 'UNDER_REVIEW', 'REVIEW_DRAFT_SAVED', 'REVIEWED_AND_COMPLETED'].includes(status);
  }

  canReviewForm(status: string): boolean {
    return status === 'SUBMITTED' || status === 'UNDER_REVIEW' || status === 'REVIEW_DRAFT_SAVED';
  }

  getOwnFormActionLabel(status: string): string {
    return status === 'NOT_STARTED' ? 'Start Appraisal' : 'Continue Editing';
  }

  openOwnForm(formId: number): void {
    this.router.navigate(['/manager/appraisal', formId]);
  }

  viewOwnForm(formId: number): void {
    this.router.navigate(['/manager/appraisal', formId], { queryParams: { readonly: true } });
  }

  reviewForm(formId: number): void {
    this.router.navigate(['/manager/review', formId]);
  }

  viewTeamMemberForm(formId: number): void {
    this.router.navigate(['/manager/review', formId], { queryParams: { readonly: true } });
  }

  downloadPdf(formId: number): void {
    window.open(`/api/forms/${formId}/pdf`, '_blank');
  }
}
