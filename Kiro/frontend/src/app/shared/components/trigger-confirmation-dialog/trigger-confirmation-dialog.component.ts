import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { User } from '../../../core/models/user.model';

export interface TriggerConfirmationData {
  cycleName: string;
  selectedEmployees: User[];
}

/**
 * Confirmation dialog for bulk cycle trigger.
 * Shows summary of selected employees and confirms the action.
 */
@Component({
  selector: 'app-trigger-confirmation-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatListModule
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>warning</mat-icon>
      Confirm Cycle Trigger
    </h2>
    
    <mat-dialog-content>
      <div class="confirmation-message">
        <p>
          You are about to trigger the cycle <strong>{{ data.cycleName }}</strong> 
          for <strong>{{ data.selectedEmployees.length }}</strong> employee(s).
        </p>
        
        <p class="warning-text">
          This action will:
        </p>
        <ul class="action-list">
          <li>Create appraisal forms for all selected employees</li>
          <li>Send notification emails to employees and their managers</li>
          <li>Make the forms available for self-appraisal</li>
        </ul>

        <div class="employee-summary" *ngIf="data.selectedEmployees.length <= 10">
          <p class="summary-title">Selected Employees:</p>
          <mat-list dense>
            <mat-list-item *ngFor="let employee of data.selectedEmployees">
              <span class="employee-name">{{ employee.fullName }}</span>
              <span class="employee-id">({{ employee.employeeId }})</span>
            </mat-list-item>
          </mat-list>
        </div>

        <div class="employee-summary" *ngIf="data.selectedEmployees.length > 10">
          <p class="summary-title">
            {{ data.selectedEmployees.length }} employees selected
          </p>
          <p class="summary-note">
            Too many to display individually. Please review your selection before confirming.
          </p>
        </div>
      </div>
    </mat-dialog-content>
    
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="false">Cancel</button>
      <button mat-raised-button color="primary" [mat-dialog-close]="true">
        Confirm & Trigger
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
        color: #ff9800;
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

    .warning-text {
      font-weight: 500;
      margin-top: 1.5rem !important;
    }

    .action-list {
      margin: 0.5rem 0 1.5rem 1.5rem;
      padding: 0;
      
      li {
        margin-bottom: 0.5rem;
        line-height: 1.5;
      }
    }

    .employee-summary {
      margin-top: 1.5rem;
      padding: 1rem;
      background-color: #f5f5f5;
      border-radius: 4px;
      max-height: 300px;
      overflow-y: auto;

      .summary-title {
        font-weight: 600;
        margin: 0 0 0.75rem 0;
        color: rgba(0, 0, 0, 0.87);
      }

      .summary-note {
        margin: 0;
        font-size: 0.875rem;
        color: rgba(0, 0, 0, 0.6);
        font-style: italic;
      }

      mat-list {
        padding: 0;
      }

      mat-list-item {
        height: auto;
        min-height: 36px;
        font-size: 0.875rem;

        .employee-name {
          font-weight: 500;
          margin-right: 0.5rem;
        }

        .employee-id {
          color: rgba(0, 0, 0, 0.6);
          font-size: 0.8125rem;
        }
      }
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
export class TriggerConfirmationDialogComponent {
  dialogRef = inject(MatDialogRef<TriggerConfirmationDialogComponent>);
  data: TriggerConfirmationData = inject(MAT_DIALOG_DATA);
}
