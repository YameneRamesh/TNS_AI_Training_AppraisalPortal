import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DashboardService } from '../../core/services/dashboard.service';
import { AuthService } from '../../core/services/auth.service';
import { EmployeeDashboard, AppraisalFormSummary } from '../../core/models/dashboard.model';
import { User } from '../../core/models/user.model';

/**
 * Employee dashboard component displaying current appraisal form and historical records.
 *
 * Requirements:
 * - Requirement 7: Employee Dashboard
 * - 7.1: Display current appraisal form with status and deadline
 * - 7.2: Display list of historical appraisal forms (read-only)
 * - 7.3: Display current status of active form
 */
@Component({
  selector: 'app-employee-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatTableModule,
    MatChipsModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  template: `
    <div class="dashboard-container">
      <!-- Header -->
      <div class="dashboard-header">
        <h1>My Appraisal Dashboard</h1>
        <p *ngIf="currentUser">Welcome, {{ currentUser.fullName }}</p>
      </div>

      <!-- Current Appraisal Form -->
      <mat-card class="current-form-card" *ngIf="dashboard">
        <mat-card-header>
          <mat-card-title>Current Appraisal</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <div *ngIf="dashboard.currentForm; else noCurrentForm">
            <div class="form-info">
              <div class="info-row">
                <span class="label">Cycle:</span>
                <span class="value">{{ dashboard.currentForm.cycleName }}</span>
              </div>
              <div class="info-row">
                <span class="label">Status:</span>
                <mat-chip [class]="getStatusClass(dashboard.currentForm.status)">
                  {{ getStatusLabel(dashboard.currentForm.status) }}
                </mat-chip>
              </div>
              <div class="info-row" *ngIf="dashboard.currentForm.submittedAt">
                <span class="label">Submitted:</span>
                <span class="value">{{ dashboard.currentForm.submittedAt | date: 'medium' }}</span>
              </div>
              <div class="info-row" *ngIf="dashboard.currentForm.reviewedAt">
                <span class="label">Reviewed:</span>
                <span class="value">{{ dashboard.currentForm.reviewedAt | date: 'medium' }}</span>
              </div>
            </div>
            <div class="form-actions">
              <button
                mat-raised-button
                color="primary"
                *ngIf="canEditForm(dashboard.currentForm.status)"
                (click)="openForm(dashboard.currentForm.id)">
                <mat-icon>edit</mat-icon>
                {{ getActionLabel(dashboard.currentForm.status) }}
              </button>
              <button
                mat-raised-button
                *ngIf="canViewForm(dashboard.currentForm.status)"
                (click)="viewForm(dashboard.currentForm.id)">
                <mat-icon>visibility</mat-icon>
                View Form
              </button>
              <button
                mat-raised-button
                *ngIf="dashboard.currentForm.pdfAvailable"
                (click)="downloadPdf(dashboard.currentForm.id)">
                <mat-icon>download</mat-icon>
                Download PDF
              </button>
            </div>
          </div>
          <ng-template #noCurrentForm>
            <p class="no-data">No active appraisal cycle at this time.</p>
          </ng-template>
        </mat-card-content>
      </mat-card>

      <!-- Historical Appraisals -->
      <mat-card class="history-card" *ngIf="dashboard && dashboard.historicalForms.length > 0">
        <mat-card-header>
          <mat-card-title>Historical Appraisals</mat-card-title>
          <button mat-button color="primary" (click)="viewAllHistory()">
            View All History
            <mat-icon>arrow_forward</mat-icon>
          </button>
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

            <!-- Submitted Date Column -->
            <ng-container matColumnDef="submittedAt">
              <th mat-header-cell *matHeaderCellDef>Submitted</th>
              <td mat-cell *matCellDef="let form">
                {{ form.submittedAt ? (form.submittedAt | date: 'shortDate') : '-' }}
              </td>
            </ng-container>

            <!-- Reviewed Date Column -->
            <ng-container matColumnDef="reviewedAt">
              <th mat-header-cell *matHeaderCellDef>Reviewed</th>
              <td mat-cell *matCellDef="let form">
                {{ form.reviewedAt ? (form.reviewedAt | date: 'shortDate') : '-' }}
              </td>
            </ng-container>

            <!-- Actions Column -->
            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef>Actions</th>
              <td mat-cell *matCellDef="let form">
                <button mat-icon-button (click)="viewForm(form.id)" matTooltip="View">
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

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
          </table>
        </mat-card-content>
      </mat-card>

      <!-- Loading State -->
      <div *ngIf="loading" class="loading-container">
        <mat-spinner></mat-spinner>
      </div>

      <!-- Error State -->
      <mat-card *ngIf="error" class="error-card">
        <mat-card-content>
          <p class="error-message">{{ error }}</p>
          <button mat-raised-button color="primary" (click)="loadDashboard()">Retry</button>
        </mat-card-content>
      </mat-card>
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

    mat-card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    mat-card-header button {
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .current-form-card,
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

    .no-data {
      color: rgba(0, 0, 0, 0.6);
      font-style: italic;
    }

    .history-table {
      width: 100%;
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
export class EmployeeDashboardComponent implements OnInit {
  dashboard: EmployeeDashboard | null = null;
  currentUser: User | null = null;
  loading = false;
  error: string | null = null;
  displayedColumns = ['cycle', 'status', 'submittedAt', 'reviewedAt', 'actions'];

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

    this.dashboardService.getEmployeeDashboard().subscribe({
      next: (data) => {
        this.dashboard = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load dashboard data. Please try again.';
        this.loading = false;
        console.error('Dashboard load error:', err);
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

  canEditForm(status: string): boolean {
    return status === 'NOT_STARTED' || status === 'DRAFT_SAVED';
  }

  canViewForm(status: string): boolean {
    return status === 'SUBMITTED' || status === 'UNDER_REVIEW' ||
           status === 'REVIEW_DRAFT_SAVED' || status === 'REVIEWED_AND_COMPLETED';
  }

  getActionLabel(status: string): string {
    return status === 'NOT_STARTED' ? 'Start Appraisal' : 'Continue Editing';
  }

  openForm(formId: number): void {
    this.router.navigate(['/employee/appraisal', formId]);
  }

  viewForm(formId: number): void {
    this.router.navigate(['/employee/appraisal', formId], { queryParams: { readonly: true } });
  }

  downloadPdf(formId: number): void {
    window.open(`/api/forms/${formId}/pdf`, '_blank');
  }

  viewAllHistory(): void {
    this.router.navigate(['/employee/history']);
  }
}
