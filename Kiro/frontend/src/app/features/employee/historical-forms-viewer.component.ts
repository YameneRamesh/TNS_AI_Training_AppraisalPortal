import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule } from '@angular/forms';
import { AppraisalForm } from '../../core/models/appraisal.model';

/**
 * Historical Forms Viewer Component
 * 
 * Displays all historical appraisal forms for the logged-in employee
 * with filtering, sorting, and detailed view capabilities.
 * 
 * Requirements:
 * - Requirement 7.2: Display list of appraisal forms from previous cycles
 * - Requirement 13: Historical Appraisal Access
 * - 13.1: Preserve all forms and template versions
 * - 13.2: Render historical forms in read-only mode
 * - 13.3: Employee can access only their own historical forms
 */
@Component({
  selector: 'app-historical-forms-viewer',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    FormsModule
  ],
  template: `
    <div class="historical-viewer-container">
      <!-- Header -->
      <div class="viewer-header">
        <h1>Historical Appraisals</h1>
        <p>View your past performance appraisals</p>
      </div>

      <!-- Filters -->
      <mat-card class="filters-card">
        <mat-card-content>
          <div class="filters-row">
            <mat-form-field appearance="outline">
              <mat-label>Search Cycle</mat-label>
              <input matInput [(ngModel)]="searchTerm" (ngModelChange)="applyFilters()" placeholder="e.g., 2024-25">
              <mat-icon matSuffix>search</mat-icon>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Filter by Status</mat-label>
              <mat-select [(ngModel)]="statusFilter" (ngModelChange)="applyFilters()">
                <mat-option value="">All Statuses</mat-option>
                <mat-option value="SUBMITTED">Submitted</mat-option>
                <mat-option value="REVIEWED_AND_COMPLETED">Completed</mat-option>
              </mat-select>
            </mat-form-field>

            <button mat-raised-button (click)="clearFilters()" *ngIf="searchTerm || statusFilter">
              <mat-icon>clear</mat-icon>
              Clear Filters
            </button>
          </div>
        </mat-card-content>
      </mat-card>

      <!-- Historical Forms Table -->
      <mat-card class="forms-card" *ngIf="!loading && filteredForms.length > 0">
        <mat-card-content>
          <table mat-table [dataSource]="filteredForms" class="forms-table">
            
            <!-- Cycle Name Column -->
            <ng-container matColumnDef="cycleName">
              <th mat-header-cell *matHeaderCellDef>Appraisal Cycle</th>
              <td mat-cell *matCellDef="let form">
                <div class="cycle-info">
                  <strong>{{ form.cycleName }}</strong>
                  <span class="cycle-dates" *ngIf="form.createdAt">
                    Created: {{ form.createdAt | date: 'MMM yyyy' }}
                  </span>
                </div>
              </td>
            </ng-container>

            <!-- Manager Column -->
            <ng-container matColumnDef="manager">
              <th mat-header-cell *matHeaderCellDef>Reviewed By</th>
              <td mat-cell *matCellDef="let form">
                {{ form.managerName || 'N/A' }}
              </td>
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
              <th mat-header-cell *matHeaderCellDef>Submitted On</th>
              <td mat-cell *matCellDef="let form">
                <div class="date-info">
                  {{ form.submittedAt ? (form.submittedAt | date: 'mediumDate') : '-' }}
                  <span class="time-info" *ngIf="form.submittedAt">
                    {{ form.submittedAt | date: 'shortTime' }}
                  </span>
                </div>
              </td>
            </ng-container>

            <!-- Reviewed Date Column -->
            <ng-container matColumnDef="reviewedAt">
              <th mat-header-cell *matHeaderCellDef>Reviewed On</th>
              <td mat-cell *matCellDef="let form">
                <div class="date-info">
                  {{ form.reviewedAt ? (form.reviewedAt | date: 'mediumDate') : '-' }}
                  <span class="time-info" *ngIf="form.reviewedAt">
                    {{ form.reviewedAt | date: 'shortTime' }}
                  </span>
                </div>
              </td>
            </ng-container>

            <!-- Actions Column -->
            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef>Actions</th>
              <td mat-cell *matCellDef="let form">
                <div class="action-buttons">
                  <button 
                    mat-icon-button 
                    color="primary"
                    (click)="viewForm(form.id)" 
                    matTooltip="View Form">
                    <mat-icon>visibility</mat-icon>
                  </button>
                  <button 
                    mat-icon-button 
                    color="accent"
                    *ngIf="form.pdfStoragePath"
                    (click)="downloadPdf(form.id)" 
                    matTooltip="Download PDF">
                    <mat-icon>download</mat-icon>
                  </button>
                </div>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;" class="form-row"></tr>
          </table>

          <!-- Results Summary -->
          <div class="results-summary">
            Showing {{ filteredForms.length }} of {{ historicalForms.length }} appraisal(s)
          </div>
        </mat-card-content>
      </mat-card>

      <!-- Empty State -->
      <mat-card class="empty-state-card" *ngIf="!loading && filteredForms.length === 0 && !error">
        <mat-card-content>
          <mat-icon class="empty-icon">history</mat-icon>
          <h2>No Historical Appraisals Found</h2>
          <p *ngIf="searchTerm || statusFilter">
            Try adjusting your filters to see more results.
          </p>
          <p *ngIf="!searchTerm && !statusFilter">
            You don't have any completed appraisals yet.
          </p>
          <button mat-raised-button color="primary" (click)="goToDashboard()">
            <mat-icon>dashboard</mat-icon>
            Go to Dashboard
          </button>
        </mat-card-content>
      </mat-card>

      <!-- Loading State -->
      <div *ngIf="loading" class="loading-container">
        <mat-spinner></mat-spinner>
        <p>Loading historical appraisals...</p>
      </div>

      <!-- Error State -->
      <mat-card *ngIf="error" class="error-card">
        <mat-card-content>
          <mat-icon class="error-icon">error_outline</mat-icon>
          <h2>Failed to Load Historical Appraisals</h2>
          <p class="error-message">{{ error }}</p>
          <button mat-raised-button color="primary" (click)="loadHistoricalForms()">
            <mat-icon>refresh</mat-icon>
            Retry
          </button>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .historical-viewer-container {
      padding: 24px;
      max-width: 1400px;
      margin: 0 auto;
    }

    .viewer-header {
      margin-bottom: 24px;
    }

    .viewer-header h1 {
      margin: 0 0 8px 0;
      font-size: 28px;
      font-weight: 500;
      color: rgba(0, 0, 0, 0.87);
    }

    .viewer-header p {
      margin: 0;
      color: rgba(0, 0, 0, 0.6);
      font-size: 14px;
    }

    .filters-card {
      margin-bottom: 24px;
    }

    .filters-row {
      display: flex;
      gap: 16px;
      align-items: center;
      flex-wrap: wrap;
    }

    .filters-row mat-form-field {
      flex: 1;
      min-width: 200px;
    }

    .forms-card {
      margin-bottom: 24px;
    }

    .forms-table {
      width: 100%;
    }

    .form-row {
      cursor: pointer;
      transition: background-color 0.2s;
    }

    .form-row:hover {
      background-color: rgba(0, 0, 0, 0.04);
    }

    .cycle-info {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .cycle-dates {
      font-size: 12px;
      color: rgba(0, 0, 0, 0.6);
    }

    .date-info {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    .time-info {
      font-size: 11px;
      color: rgba(0, 0, 0, 0.5);
    }

    mat-chip {
      font-size: 12px;
      min-height: 24px;
    }

    mat-chip.status-submitted {
      background-color: #e3f2fd;
      color: #1976d2;
    }

    mat-chip.status-under-review {
      background-color: #f3e5f5;
      color: #7b1fa2;
    }

    mat-chip.status-completed {
      background-color: #e8f5e9;
      color: #388e3c;
    }

    .action-buttons {
      display: flex;
      gap: 4px;
    }

    .results-summary {
      margin-top: 16px;
      padding: 8px 0;
      text-align: right;
      font-size: 14px;
      color: rgba(0, 0, 0, 0.6);
    }

    .empty-state-card,
    .error-card {
      text-align: center;
      padding: 48px 24px;
    }

    .empty-icon,
    .error-icon {
      font-size: 64px;
      width: 64px;
      height: 64px;
      color: rgba(0, 0, 0, 0.3);
      margin-bottom: 16px;
    }

    .error-icon {
      color: #d32f2f;
    }

    .empty-state-card h2,
    .error-card h2 {
      margin: 0 0 8px 0;
      font-size: 20px;
      font-weight: 500;
    }

    .empty-state-card p,
    .error-card p {
      margin: 0 0 24px 0;
      color: rgba(0, 0, 0, 0.6);
    }

    .error-message {
      color: #d32f2f;
    }

    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 64px 24px;
      gap: 16px;
    }

    .loading-container p {
      color: rgba(0, 0, 0, 0.6);
    }
  `]
})
export class HistoricalFormsViewerComponent implements OnInit {
  historicalForms: AppraisalForm[] = [];
  filteredForms: AppraisalForm[] = [];
  loading = false;
  error: string | null = null;
  
  searchTerm = '';
  statusFilter = '';
  
  displayedColumns = ['cycleName', 'manager', 'status', 'submittedAt', 'reviewedAt', 'actions'];

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadHistoricalForms();
  }

  loadHistoricalForms(): void {
    this.loading = true;
    this.error = null;

    this.http.get<AppraisalForm[]>('/api/forms/history').subscribe({
      next: (forms) => {
        this.historicalForms = forms;
        this.applyFilters();
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Unable to load historical appraisals. Please try again later.';
        this.loading = false;
        console.error('Failed to load historical forms:', err);
      }
    });
  }

  applyFilters(): void {
    this.filteredForms = this.historicalForms.filter(form => {
      const matchesSearch = !this.searchTerm || 
        form.cycleName?.toLowerCase().includes(this.searchTerm.toLowerCase());
      
      const matchesStatus = !this.statusFilter || 
        form.status === this.statusFilter;
      
      return matchesSearch && matchesStatus;
    });
  }

  clearFilters(): void {
    this.searchTerm = '';
    this.statusFilter = '';
    this.applyFilters();
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
      'SUBMITTED': 'status-submitted',
      'UNDER_REVIEW': 'status-under-review',
      'REVIEW_DRAFT_SAVED': 'status-under-review',
      'REVIEWED_AND_COMPLETED': 'status-completed'
    };
    return classes[status] || '';
  }

  viewForm(formId: number): void {
    this.router.navigate(['/employee/appraisal', formId], { 
      queryParams: { readonly: true, historical: true } 
    });
  }

  downloadPdf(formId: number): void {
    window.open(`/api/forms/${formId}/pdf`, '_blank');
  }

  goToDashboard(): void {
    this.router.navigate(['/employee/dashboard']);
  }
}
