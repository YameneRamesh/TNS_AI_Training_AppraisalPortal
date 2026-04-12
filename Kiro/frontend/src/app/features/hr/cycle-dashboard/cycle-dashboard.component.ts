import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { DashboardService } from '../../../core/services/dashboard.service';
import { CycleService } from '../../../core/services/cycle.service';
import { HRDashboard, DepartmentProgress } from '../../../core/models/dashboard.model';
import { AppraisalCycle } from '../../../core/models/appraisal.model';

/**
 * Component for HR cycle management dashboard.
 * Displays active cycle metrics, department progress, and cycle list.
 */
@Component({
  selector: 'app-cycle-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatSnackBarModule
  ],
  templateUrl: './cycle-dashboard.component.html',
  styleUrl: './cycle-dashboard.component.scss'
})
export class CycleDashboardComponent implements OnInit {
  dashboardData: HRDashboard | null = null;
  cycles: AppraisalCycle[] = [];
  loading = false;
  error: string | null = null;

  departmentColumns: string[] = ['department', 'totalEmployees', 'completedAppraisals', 'completionPercentage'];
  cycleColumns: string[] = ['name', 'startDate', 'endDate', 'status', 'actions'];

  constructor(
    private dashboardService: DashboardService,
    private cycleService: CycleService,
    private router: Router,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadDashboard();
    this.loadCycles();
  }

  loadDashboard(): void {
    this.dashboardService.getHRDashboard().subscribe({
      next: (response) => {
        this.dashboardData = response.data || null;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading HR dashboard:', err);
        this.snackBar.open('Failed to load dashboard metrics', 'Close', { duration: 3000 });
        this.cdr.detectChanges();
      }
    });
  }

  loadCycles(): void {
    this.loading = true;
    this.error = null;

    this.cycleService.getCycles().subscribe({
      next: (response) => {
        this.cycles = response.data || [];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = 'Failed to load cycles. Please try again.';
        this.loading = false;
        console.error('Error loading cycles:', err);
        this.snackBar.open('Failed to load cycles', 'Close', { duration: 3000 });
        this.cdr.detectChanges();
      }
    });
  }

  /**
   * Navigate to create new cycle
   */
  createCycle(): void {
    this.router.navigate(['/hr/cycles/create']);
  }

  /**
   * Navigate to cycle details
   */
  viewCycle(cycle: AppraisalCycle): void {
    this.router.navigate(['/hr/cycles', cycle.id]);
  }

  /**
   * Navigate to trigger cycle
   */
  triggerCycle(cycle: AppraisalCycle): void {
    this.router.navigate(['/hr/cycles', cycle.id, 'trigger']);
  }

  /**
   * Get status chip color
   */
  getStatusColor(status: string): string {
    switch (status) {
      case 'ACTIVE':
        return 'primary';
      case 'CLOSED':
        return 'accent';
      case 'DRAFT':
      default:
        return 'warn';
    }
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

  /**
   * Get completion percentage color
   */
  getCompletionColor(percentage: number): string {
    if (percentage >= 80) return 'primary';
    if (percentage >= 50) return 'accent';
    return 'warn';
  }
}
