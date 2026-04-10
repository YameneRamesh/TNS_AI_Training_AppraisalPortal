import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatMenuModule } from '@angular/material/menu';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { CycleService } from '../../../core/services/cycle.service';
import { AppraisalCycle, AppraisalForm } from '../../../core/models/appraisal.model';
import { ReopenFormDialogComponent } from '../../../shared/components/reopen-form-dialog/reopen-form-dialog.component';
import { AssignBackupReviewerDialogComponent } from '../../../shared/components/assign-backup-reviewer-dialog/assign-backup-reviewer-dialog.component';

/**
 * Component for viewing cycle details and managing forms.
 * Allows HR to view all forms in a cycle and reopen them if needed.
 */
@Component({
  selector: 'app-cycle-details',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatMenuModule,
    MatDialogModule
  ],
  templateUrl: './cycle-details.component.html',
  styleUrl: './cycle-details.component.scss'
})
export class CycleDetailsComponent implements OnInit {
  cycle: AppraisalCycle | null = null;
  forms: AppraisalForm[] = [];
  filteredForms: AppraisalForm[] = [];
  searchControl = new FormControl('');
  
  loading = false;
  cycleId: number | null = null;
  
  displayedColumns: string[] = ['employeeName', 'employeeId', 'managerName', 'backupReviewerName', 'status', 'submittedAt', 'reviewedAt', 'actions'];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private cycleService: CycleService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.cycleId = Number(this.route.snapshot.paramMap.get('id'));
    
    if (this.cycleId) {
      this.loadCycle();
      this.loadForms();
    }

    // Setup search filter
    this.searchControl.valueChanges.subscribe(value => {
      this.filterForms(value || '');
    });
  }

  /**
   * Load cycle details
   */
  loadCycle(): void {
    if (!this.cycleId) return;

    this.cycleService.getCycleById(this.cycleId).subscribe({
      next: (response) => {
        this.cycle = response.data || null;
      },
      error: (err) => {
        console.error('Error loading cycle:', err);
        this.snackBar.open('Failed to load cycle details', 'Close', { duration: 3000 });
      }
    });
  }

  /**
   * Load forms for this cycle
   * Note: This would typically be a separate API endpoint like /api/cycles/{id}/forms
   * For now, we'll use a placeholder
   */
  loadForms(): void {
    this.loading = true;
    
    // TODO: Replace with actual API call when backend endpoint is ready
    // this.cycleService.getCycleForms(this.cycleId).subscribe(...)
    
    // Placeholder - in real implementation, this would fetch from backend
    this.forms = [];
    this.filteredForms = [];
    this.loading = false;
  }

  /**
   * Filter forms based on search term
   */
  filterForms(searchTerm: string): void {
    const term = searchTerm.toLowerCase().trim();
    
    if (!term) {
      this.filteredForms = [...this.forms];
      return;
    }

    this.filteredForms = this.forms.filter(form => 
      form.employeeName?.toLowerCase().includes(term) ||
      form.managerName?.toLowerCase().includes(term) ||
      form.status.toLowerCase().includes(term)
    );
  }

  /**
   * Open reopen form dialog
   */
  reopenForm(form: AppraisalForm): void {
    const dialogRef = this.dialog.open(ReopenFormDialogComponent, {
      width: '600px',
      data: { form },
      disableClose: true
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed && this.cycleId) {
        this.executeReopen(form);
      }
    });
  }

  /**
   * Execute form reopen
   */
  private executeReopen(form: AppraisalForm): void {
    if (!this.cycleId) return;

    this.cycleService.reopenForm(this.cycleId, form.id).subscribe({
      next: () => {
        this.snackBar.open('Form reopened successfully', 'Close', { duration: 3000 });
        this.loadForms(); // Reload to get updated status
      },
      error: (err) => {
        console.error('Error reopening form:', err);
        const errorMessage = err.error?.message || 'Failed to reopen form. Please try again.';
        this.snackBar.open(errorMessage, 'Close', { duration: 5000 });
      }
    });
  }

  /**
   * Open assign backup reviewer dialog
   */
  assignBackupReviewer(form: AppraisalForm): void {
    if (!this.cycleId) return;

    const dialogRef = this.dialog.open(AssignBackupReviewerDialogComponent, {
      width: '600px',
      data: { 
        form,
        cycleId: this.cycleId
      },
      disableClose: true
    });

    dialogRef.afterClosed().subscribe(backupReviewerId => {
      if (backupReviewerId && this.cycleId) {
        this.executeAssignBackupReviewer(form, backupReviewerId);
      }
    });
  }

  /**
   * Execute backup reviewer assignment
   */
  private executeAssignBackupReviewer(form: AppraisalForm, backupReviewerId: number): void {
    if (!this.cycleId) return;

    this.cycleService.assignBackupReviewer(this.cycleId, form.id, backupReviewerId).subscribe({
      next: () => {
        this.snackBar.open('Backup reviewer assigned successfully', 'Close', { duration: 3000 });
        this.loadForms(); // Reload to get updated data
      },
      error: (err) => {
        console.error('Error assigning backup reviewer:', err);
        const errorMessage = err.error?.message || 'Failed to assign backup reviewer. Please try again.';
        this.snackBar.open(errorMessage, 'Close', { duration: 5000 });
      }
    });
  }

  /**
   * Check if form can be reopened
   */
  canReopen(form: AppraisalForm): boolean {
    return form.status === 'SUBMITTED' || 
           form.status === 'UNDER_REVIEW' ||
           form.status === 'REVIEW_DRAFT_SAVED' ||
           form.status === 'REVIEWED_AND_COMPLETED';
  }

  /**
   * Navigate back to cycles list
   */
  goBack(): void {
    this.router.navigate(['/hr/cycles']);
  }

  /**
   * Get status chip color
   */
  getStatusColor(status: string): string {
    switch (status) {
      case 'NOT_STARTED':
        return 'default';
      case 'DRAFT_SAVED':
        return 'accent';
      case 'SUBMITTED':
        return 'primary';
      case 'UNDER_REVIEW':
      case 'REVIEW_DRAFT_SAVED':
        return 'primary';
      case 'REVIEWED_AND_COMPLETED':
        return 'primary';
      default:
        return 'default';
    }
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

  /**
   * Format date for display
   */
  formatDate(dateString: string | undefined): string {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }
}
