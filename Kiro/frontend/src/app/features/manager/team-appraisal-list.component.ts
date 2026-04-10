import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TeamMemberForm } from '../../core/models/dashboard.model';

export interface TeamMemberAction {
  formId: number;
  readonly: boolean;
}

/**
 * Team appraisal list component for the manager module.
 *
 * Displays a table of direct reportees with their appraisal form statuses
 * and provides navigation actions to individual review forms.
 *
 * Requirements:
 * - 8.2: Display list of direct reportees with Appraisal_Form status
 * - 8.5: Provide hyperlink from each team member entry to their Appraisal_Form
 * - 6.1: Submitted forms appear under "Pending Reviews"
 */
@Component({
  selector: 'app-team-appraisal-list',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatChipsModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule
  ],
  template: `
    <div class="team-list-container">
      <div *ngIf="teamForms.length > 0; else noTeamForms">
        <table mat-table [dataSource]="teamForms" class="team-table">

          <!-- Employee Name Column -->
          <ng-container matColumnDef="employeeName">
            <th mat-header-cell *matHeaderCellDef>Employee Name</th>
            <td mat-cell *matCellDef="let member">{{ member.employeeName }}</td>
          </ng-container>

          <!-- Designation Column -->
          <ng-container matColumnDef="designation">
            <th mat-header-cell *matHeaderCellDef>Designation</th>
            <td mat-cell *matCellDef="let member">{{ member.designation }}</td>
          </ng-container>

          <!-- Department Column -->
          <ng-container matColumnDef="department">
            <th mat-header-cell *matHeaderCellDef>Department</th>
            <td mat-cell *matCellDef="let member">{{ member.department || '-' }}</td>
          </ng-container>

          <!-- Status Column -->
          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Form Status</th>
            <td mat-cell *matCellDef="let member">
              <mat-chip [class]="getStatusClass(member.status)" disableRipple>
                {{ getStatusLabel(member.status) }}
              </mat-chip>
            </td>
          </ng-container>

          <!-- Actions Column (Req 8.5) -->
          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef>Actions</th>
            <td mat-cell *matCellDef="let member">
              <button
                mat-stroked-button
                color="primary"
                *ngIf="canReview(member.status)"
                (click)="onReview(member)"
                matTooltip="Review this appraisal form"
                class="action-btn">
                <mat-icon>rate_review</mat-icon>
                Review
              </button>
              <button
                mat-stroked-button
                *ngIf="canView(member.status)"
                (click)="onView(member)"
                matTooltip="View completed appraisal (read-only)"
                class="action-btn">
                <mat-icon>visibility</mat-icon>
                View
              </button>
              <span *ngIf="!canReview(member.status) && !canView(member.status)" class="no-action">
                —
              </span>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
        </table>
      </div>

      <ng-template #noTeamForms>
        <p class="no-data">No team members have active appraisal forms.</p>
      </ng-template>
    </div>
  `,
  styles: [`
    .team-list-container {
      width: 100%;
    }

    .team-table {
      width: 100%;
    }

    mat-chip {
      font-size: 12px;
      min-height: 24px;
      cursor: default;
    }

    /* Status color coding */
    mat-chip.status-not-started {
      background-color: #ffcdd2;
      color: #b71c1c;
    }

    mat-chip.status-draft {
      background-color: #ffe0b2;
      color: #e65100;
    }

    mat-chip.status-submitted {
      background-color: #ffe0b2;
      color: #e65100;
    }

    mat-chip.status-under-review {
      background-color: #bbdefb;
      color: #0d47a1;
    }

    mat-chip.status-review-draft {
      background-color: #bbdefb;
      color: #0d47a1;
    }

    mat-chip.status-completed {
      background-color: #c8e6c9;
      color: #1b5e20;
    }

    .action-btn {
      margin-right: 8px;
    }

    .action-btn mat-icon {
      font-size: 18px;
      height: 18px;
      width: 18px;
      margin-right: 4px;
    }

    .no-action {
      color: rgba(0, 0, 0, 0.38);
    }

    .no-data {
      color: rgba(0, 0, 0, 0.6);
      font-style: italic;
      padding: 16px 0;
    }
  `]
})
export class TeamAppraisalListComponent {
  @Input() teamForms: TeamMemberForm[] = [];
  @Output() memberSelected = new EventEmitter<TeamMemberAction>();

  displayedColumns = ['employeeName', 'designation', 'department', 'status', 'actions'];

  /** Forms in these statuses can be actively reviewed by the manager (Req 6.1, 8.5) */
  canReview(status: string): boolean {
    return status === 'SUBMITTED' || status === 'UNDER_REVIEW' || status === 'REVIEW_DRAFT_SAVED';
  }

  /** Completed forms can be viewed in read-only mode */
  canView(status: string): boolean {
    return status === 'REVIEWED_AND_COMPLETED';
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
    return labels[status] ?? status;
  }

  getStatusClass(status: string): string {
    const classes: Record<string, string> = {
      'NOT_STARTED': 'status-not-started',
      'DRAFT_SAVED': 'status-draft',
      'SUBMITTED': 'status-submitted',
      'UNDER_REVIEW': 'status-under-review',
      'REVIEW_DRAFT_SAVED': 'status-review-draft',
      'REVIEWED_AND_COMPLETED': 'status-completed'
    };
    return classes[status] ?? '';
  }

  onReview(member: TeamMemberForm): void {
    this.memberSelected.emit({ formId: member.formId, readonly: false });
  }

  onView(member: TeamMemberForm): void {
    this.memberSelected.emit({ formId: member.formId, readonly: true });
  }
}
