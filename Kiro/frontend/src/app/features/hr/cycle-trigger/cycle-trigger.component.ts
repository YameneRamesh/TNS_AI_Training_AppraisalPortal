import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { SelectionModel } from '@angular/cdk/collections';
import { CycleService, TriggerCycleResult } from '../../../core/services/cycle.service';
import { UserService } from '../../../core/services/user.service';
import { AppraisalCycle } from '../../../core/models/appraisal.model';
import { User } from '../../../core/models/user.model';
import { TriggerConfirmationDialogComponent } from '../../../shared/components/trigger-confirmation-dialog/trigger-confirmation-dialog.component';

/**
 * Component for triggering an appraisal cycle for selected employees.
 * Allows HR to select employees and bulk trigger the cycle.
 */
@Component({
  selector: 'app-cycle-trigger',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatTableModule,
    MatCheckboxModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatChipsModule,
    MatDialogModule
  ],
  templateUrl: './cycle-trigger.component.html',
  styleUrl: './cycle-trigger.component.scss'
})
export class CycleTriggerComponent implements OnInit {
  cycle: AppraisalCycle | null = null;
  employees: User[] = [];
  filteredEmployees: User[] = [];
  selection = new SelectionModel<User>(true, []);
  searchControl = new FormControl('');
  
  loading = false;
  triggering = false;
  cycleId: number | null = null;
  
  displayedColumns: string[] = ['select', 'employeeId', 'fullName', 'designation', 'department', 'managerName'];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private cycleService: CycleService,
    private userService: UserService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.cycleId = Number(this.route.snapshot.paramMap.get('id'));
    
    if (this.cycleId) {
      this.loadCycle();
      this.loadEmployees();
    }

    // Setup search filter
    this.searchControl.valueChanges.subscribe(value => {
      this.filterEmployees(value || '');
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
   * Load all active employees
   */
  loadEmployees(): void {
    this.loading = true;
    
    this.userService.getUsers({ isActive: true }).subscribe({
      next: (response) => {
        this.employees = response.data || [];
        this.filteredEmployees = [...this.employees];
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        console.error('Error loading employees:', err);
        this.snackBar.open('Failed to load employees', 'Close', { duration: 3000 });
      }
    });
  }

  /**
   * Filter employees based on search term
   */
  filterEmployees(searchTerm: string): void {
    const term = searchTerm.toLowerCase().trim();
    
    if (!term) {
      this.filteredEmployees = [...this.employees];
      return;
    }

    this.filteredEmployees = this.employees.filter(emp => 
      emp.fullName.toLowerCase().includes(term) ||
      emp.employeeId.toLowerCase().includes(term) ||
      emp.email.toLowerCase().includes(term) ||
      emp.department?.toLowerCase().includes(term) ||
      emp.designation?.toLowerCase().includes(term)
    );
  }

  /**
   * Check if all rows are selected
   */
  isAllSelected(): boolean {
    const numSelected = this.selection.selected.length;
    const numRows = this.filteredEmployees.length;
    return numSelected === numRows && numRows > 0;
  }

  /**
   * Toggle all rows selection
   */
  toggleAllRows(): void {
    if (this.isAllSelected()) {
      this.selection.clear();
    } else {
      this.filteredEmployees.forEach(row => this.selection.select(row));
    }
  }

  /**
   * Check if some but not all rows are selected
   */
  isIndeterminate(): boolean {
    const numSelected = this.selection.selected.length;
    return numSelected > 0 && numSelected < this.filteredEmployees.length;
  }

  /**
   * Trigger cycle for selected employees
   */
  onTrigger(): void {
    if (this.selection.selected.length === 0) {
      this.snackBar.open('Please select at least one employee', 'Close', { duration: 3000 });
      return;
    }

    if (!this.cycleId || !this.cycle) return;

    // Show confirmation dialog
    const dialogRef = this.dialog.open(TriggerConfirmationDialogComponent, {
      width: '600px',
      data: {
        cycleName: this.cycle.name,
        selectedEmployees: this.selection.selected
      },
      disableClose: true
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.executeTrigger();
      }
    });
  }

  /**
   * Execute the actual trigger operation
   */
  private executeTrigger(): void {
    if (!this.cycleId) return;

    const employeeIds = this.selection.selected.map(emp => emp.id);
    
    this.triggering = true;
    this.cycleService.triggerCycle(this.cycleId, { employeeIds }).subscribe({
      next: (response) => {
        this.triggering = false;
        const result = response.data as TriggerCycleResult;
        
        if (result.failureCount === 0) {
          this.snackBar.open(
            `Cycle triggered successfully for ${result.successCount} employee(s)`, 
            'Close', 
            { duration: 5000 }
          );
          this.router.navigate(['/hr/cycles']);
        } else {
          this.showPartialFailureDialog(result);
        }
      },
      error: (err) => {
        this.triggering = false;
        console.error('Error triggering cycle:', err);
        const errorMessage = err.error?.message || 'Failed to trigger cycle. Please try again.';
        this.snackBar.open(errorMessage, 'Close', { duration: 5000 });
      }
    });
  }

  /**
   * Show dialog for partial failure results
   */
  showPartialFailureDialog(result: TriggerCycleResult): void {
    const message = `Cycle triggered with partial success:\n` +
      `✓ Success: ${result.successCount}\n` +
      `✗ Failed: ${result.failureCount}\n\n` +
      `Failed employees:\n` +
      result.failures.map(f => `- ${f.employeeName}: ${f.reason}`).join('\n');
    
    this.snackBar.open(message, 'Close', { duration: 10000 });
    
    // Navigate back after showing message
    setTimeout(() => {
      this.router.navigate(['/hr/cycles']);
    }, 2000);
  }

  /**
   * Cancel and navigate back
   */
  onCancel(): void {
    this.router.navigate(['/hr/cycles']);
  }

  /**
   * Get selected count text
   */
  getSelectionText(): string {
    const count = this.selection.selected.length;
    return count === 0 ? 'No employees selected' : `${count} employee(s) selected`;
  }

  /**
   * Format date for display
   */
  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }
}
