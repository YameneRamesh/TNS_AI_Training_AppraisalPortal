import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AppraisalForm } from '../../../core/models/appraisal.model';
import { User } from '../../../core/models/user.model';
import { UserService } from '../../../core/services/user.service';

export interface AssignBackupReviewerData {
  form: AppraisalForm;
  cycleId: number;
}

/**
 * Dialog for assigning a backup reviewer to an appraisal form.
 * Allows HR to select a manager or HR user as backup reviewer.
 */
@Component({
  selector: 'app-assign-backup-reviewer-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatSelectModule,
    MatProgressSpinnerModule
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>person_add</mat-icon>
      Assign Backup Reviewer
    </h2>
    
    <mat-dialog-content>
      <div class="dialog-content">
        <p class="description">
          Assign a backup reviewer for <strong>{{ data.form.employeeName }}</strong>'s appraisal form.
        </p>

        <div class="form-details">
          <div class="detail-row">
            <span class="label">Employee ID:</span>
            <span class="value">{{ data.form.employeeId }}</span>
          </div>
          <div class="detail-row">
            <span class="label">Primary Manager:</span>
            <span class="value">{{ data.form.managerName }}</span>
          </div>
          <div class="detail-row" *ngIf="data.form.backupReviewerName">
            <span class="label">Current Backup:</span>
            <span class="value">{{ data.form.backupReviewerName }}</span>
          </div>
        </div>

        <div *ngIf="loading" class="loading-container">
          <mat-spinner diameter="30"></mat-spinner>
          <p>Loading reviewers...</p>
        </div>

        <mat-form-field appearance="outline" class="full-width" *ngIf="!loading">
          <mat-label>Select Backup Reviewer</mat-label>
          <mat-select [formControl]="backupReviewerControl" placeholder="Choose a manager or HR user">
            <mat-option *ngFor="let reviewer of availableReviewers" [value]="reviewer.id">
              {{ reviewer.fullName }} ({{ reviewer.designation }})
            </mat-option>
          </mat-select>
          <mat-icon matPrefix>person</mat-icon>
          <mat-hint *ngIf="availableReviewers.length === 0">No available reviewers</mat-hint>
          <mat-error *ngIf="backupReviewerControl.hasError('required')">
            Please select a backup reviewer
          </mat-error>
        </mat-form-field>

        <div class="info-message">
          <mat-icon>info</mat-icon>
          <p>
            The backup reviewer will have the same permissions as the primary manager 
            to review and complete this appraisal form.
          </p>
        </div>
      </div>
    </mat-dialog-content>
    
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="null">Cancel</button>
      <button 
        mat-raised-button 
        color="primary" 
        (click)="onAssign()"
        [disabled]="backupReviewerControl.invalid || loading">
        Assign Reviewer
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
        color: #1976d2;
      }
    }

    mat-dialog-content {
      min-width: 500px;
      max-width: 600px;

      @media (max-width: 600px) {
        min-width: 300px;
      }
    }

    .dialog-content {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
    }

    .description {
      margin: 0;
      line-height: 1.6;

      strong {
        font-weight: 600;
        color: #1976d2;
      }
    }

    .form-details {
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
      }
    }

    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.75rem;
      padding: 2rem 0;

      p {
        margin: 0;
        color: rgba(0, 0, 0, 0.6);
        font-size: 0.875rem;
      }
    }

    .full-width {
      width: 100%;
    }

    .info-message {
      display: flex;
      align-items: flex-start;
      gap: 0.75rem;
      padding: 1rem;
      background-color: #e3f2fd;
      border-radius: 4px;
      border-left: 4px solid #2196f3;

      mat-icon {
        color: #2196f3;
        font-size: 1.25rem;
        width: 1.25rem;
        height: 1.25rem;
        margin-top: 0.125rem;
        flex-shrink: 0;
      }

      p {
        margin: 0;
        font-size: 0.875rem;
        line-height: 1.5;
        color: rgba(0, 0, 0, 0.87);
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
export class AssignBackupReviewerDialogComponent implements OnInit {
  backupReviewerControl = new FormControl<number | null>(null, Validators.required);
  availableReviewers: User[] = [];
  loading = true;

  dialogRef = inject(MatDialogRef<AssignBackupReviewerDialogComponent>);
  data: AssignBackupReviewerData = inject(MAT_DIALOG_DATA);
  private userService = inject(UserService);
  private cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    if (this.data.form.backupReviewerId) {
      this.backupReviewerControl.setValue(this.data.form.backupReviewerId);
    }
    this.loadAvailableReviewers();
  }

  loadAvailableReviewers(): void {
    this.loading = true;

    this.userService.getUsers({ isActive: true, size: 1000 }).subscribe({
      next: (response) => {
        const users: User[] = response.data?.content || [];

        this.availableReviewers = users.filter((user: User) => {
          const hasManagerOrHRRole = Array.isArray(user.roles) && user.roles.some((role: any) => {
            if (typeof role === 'string') {
              return role === 'MANAGER' || role === 'HR';
            }
            return role?.name === 'MANAGER' || role?.name === 'HR';
          });
          const isNotPrimaryManager = user.id !== this.data.form.managerId;
          return hasManagerOrHRRole && isNotPrimaryManager;
        });

        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.loading = false;
        this.cdr.detectChanges();
        console.error('Error loading reviewers:', err);
      }
    });
  }

  /**
   * Assign the selected backup reviewer
   */
  onAssign(): void {
    if (this.backupReviewerControl.valid) {
      const backupReviewerId = this.backupReviewerControl.value;
      this.dialogRef.close(backupReviewerId);
    }
  }
}
