import { Component, Inject, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AppraisalForm } from '../../../core/models/appraisal.model';

export interface ReopenFormData {
  form: AppraisalForm;
}

/**
 * Confirmation dialog for reopening a submitted or completed appraisal form.
 * Warns HR about the implications of reopening.
 */
@Component({
  selector: 'app-reopen-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>lock_open</mat-icon>
      Reopen Appraisal Form
    </h2>
    
    <mat-dialog-content>
      <div class="confirmation-message">
        <p>
          You are about to reopen the appraisal form for 
          <strong>{{ data.form.employeeName }}</strong>.
        </p>
        
        <div class="form-details">
          <div class="detail-row">
            <span class="label">Employee ID:</span>
            <span class="value">{{ data.form.employeeId }}</span>
          </div>
          <div class="detail-row">
            <span class="label">Current Status:</span>
            <span class="value status-badge" [class]="getStatusClass()">
              {{ formatStatus(data.form.status) }}
            </span>
          </div>
          <div class="detail-row">
            <span class="label">Cycle:</span>
            <span class="value">{{ data.form.cycleName }}</span>
          </div>
        </div>

        <div class="warning-box">
          <mat-icon>warning</mat-icon>
          <div class="warning-content">
            <p class="warning-title">Warning</p>
            <p>Reopening this form will:</p>
            <ul>
              <li>Reset the form status to allow re-submission</li>
              <li>Allow the employee to edit their self-appraisal</li>
              <li *ngIf="isCompleted()">Remove the completed review data</li>
              <li>Require the employee to resubmit the form</li>
            </ul>
          </div>
        </div>

        <p class="confirm-text">
          Are you sure you want to proceed?
        </p>
      </div>
    </mat-dialog-content>
    
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="false">Cancel</button>
      <button mat-raised-button color="warn" [mat-dialog-close]="true">
        Reopen Form
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    h2 {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin: 0;
      
      mat-icon {
        color: #f44336;
      }
    }

    mat-dialog-content {
      min-width: 500px;
      max-width: 600px;

      @media (max-width: 600px) {
        min-width: 300px;
      }
    }

    .confirmation-message {
      p {
        margin: 0 0 1rem 0;
        line-height: 1.6;
      }

      strong {
        font-weight: 600;
        color: #1976d2;
      }
    }

    .form-details {
      margin: 1.5rem 0;
      padding: 1rem;
      background-color: #f5f5f5;
      border-radius: 4px;

      .detail-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 0.5rem 0;
        border-bottom: 1px solid #e0e0e0;

        &:last-child {
          border-bottom: none;
        }

        .label {
          font-weight: 500;
          color: rgba(0, 0, 0, 0.6);
        }

        .value {
          font-weight: 500;
          color: rgba(0, 0, 0, 0.87);
        }

        .status-badge {
          padding: 0.25rem 0.75rem;
          border-radius: 12px;
          font-size: 0.8125rem;
          font-weight: 600;

          &.submitted {
            background-color: #fff3e0;
            color: #f57c00;
          }

          &.completed {
            background-color: #e8f5e9;
            color: #2e7d32;
          }

          &.under-review {
            background-color: #e3f2fd;
            color: #1976d2;
          }
        }
      }
    }

    .warning-box {
      display: flex;
      gap: 0.75rem;
      padding: 1rem;
      margin: 1.5rem 0;
      background-color: #fff3e0;
      border-radius: 4px;
      border-left: 4px solid #ff9800;

      > mat-icon {
        color: #ff9800;
        font-size: 1.5rem;
        width: 1.5rem;
        height: 1.5rem;
        flex-shrink: 0;
      }

      .warning-content {
        flex: 1;

        .warning-title {
          font-weight: 600;
          margin: 0 0 0.5rem 0;
          color: rgba(0, 0, 0, 0.87);
        }

        p {
          margin: 0 0 0.5rem 0;
          font-size: 0.875rem;
        }

        ul {
          margin: 0.5rem 0 0 1.25rem;
          padding: 0;

          li {
            margin-bottom: 0.25rem;
            font-size: 0.875rem;
            line-height: 1.5;
          }
        }
      }
    }

    .confirm-text {
      font-weight: 500;
      margin-top: 1.5rem !important;
    }

    mat-dialog-actions {
      padding: 1rem 1.5rem;
      margin: 0;
      
      button {
        min-width: 100px;
      }
    }
  `]
})
export class ReopenFormDialogComponent {
  dialogRef = inject(MatDialogRef<ReopenFormDialogComponent>);
  data: ReopenFormData = inject(MAT_DIALOG_DATA);

  /**
   * Check if form is completed
   */
  isCompleted(): boolean {
    return this.data.form.status === 'REVIEWED_AND_COMPLETED';
  }

  /**
   * Get status CSS class
   */
  getStatusClass(): string {
    const status = this.data.form.status;
    if (status === 'SUBMITTED') return 'submitted';
    if (status === 'REVIEWED_AND_COMPLETED') return 'completed';
    if (status === 'UNDER_REVIEW' || status === 'REVIEW_DRAFT_SAVED') return 'under-review';
    return '';
  }

  /**
   * Format status for display
   */
  formatStatus(status: string): string {
    return status.replace(/_/g, ' ').toLowerCase()
      .split(' ')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }
}
