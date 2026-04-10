import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { AuditLog } from '../../core/models/audit.model';
import { AuditService } from '../../core/services/audit.service';

@Component({
  selector: 'app-audit-log-viewer',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatTooltipModule,
    MatSnackBarModule
  ],
  templateUrl: './audit-log-viewer.component.html',
  styleUrls: ['./audit-log-viewer.component.scss']
})
export class AuditLogViewerComponent implements OnInit {
  displayedColumns: string[] = ['createdAt', 'userName', 'action', 'entityType', 'entityId', 'ipAddress', 'details'];
  auditLogs: AuditLog[] = [];
  isLoading = false;

  // Pagination
  pageSize = 25;
  pageIndex = 0;
  totalLogs = 0;

  // Filters
  actionFilter = new FormControl('');
  startDateFilter = new FormControl<Date | null>(null);
  endDateFilter = new FormControl<Date | null>(null);

  // Available actions for filter dropdown
  availableActions = [
    'LOGIN',
    'LOGOUT',
    'FORM_SUBMIT',
    'REVIEW_COMPLETE',
    'ROLE_CHANGE',
    'CYCLE_TRIGGER',
    'FORM_REOPEN',
    'USER_CREATE',
    'USER_UPDATE',
    'USER_DELETE'
  ];

  constructor(
    private auditService: AuditService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadAuditLogs();
    this.setupFilters();
  }

  setupFilters(): void {
    this.actionFilter.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => {
        this.pageIndex = 0;
        this.loadAuditLogs();
      });

    this.startDateFilter.valueChanges.subscribe(() => {
      this.pageIndex = 0;
      this.loadAuditLogs();
    });

    this.endDateFilter.valueChanges.subscribe(() => {
      this.pageIndex = 0;
      this.loadAuditLogs();
    });
  }

  loadAuditLogs(): void {
    this.isLoading = true;

    const params = {
      action: this.actionFilter.value || undefined,
      startDate: this.startDateFilter.value ? this.formatDate(this.startDateFilter.value) : undefined,
      endDate: this.endDateFilter.value ? this.formatDate(this.endDateFilter.value) : undefined,
      page: this.pageIndex,
      size: this.pageSize
    };

    this.auditService.getAuditLogs(params).subscribe({
      next: (response) => {
        this.auditLogs = response.content;
        this.totalLogs = response.totalElements;
        this.isLoading = false;
      },
      error: (error) => {
        this.isLoading = false;
        this.snackBar.open('Failed to load audit logs', 'Close', { duration: 3000 });
        console.error('Error loading audit logs:', error);
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadAuditLogs();
  }

  clearFilters(): void {
    this.actionFilter.setValue('');
    this.startDateFilter.setValue(null);
    this.endDateFilter.setValue(null);
  }

  formatDate(date: Date): string {
    return date.toISOString().split('T')[0];
  }

  formatDateTime(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  }

  getActionColor(action: string): string {
    const actionColors: { [key: string]: string } = {
      'LOGIN': 'success',
      'LOGOUT': 'neutral',
      'FORM_SUBMIT': 'info',
      'REVIEW_COMPLETE': 'success',
      'ROLE_CHANGE': 'warning',
      'CYCLE_TRIGGER': 'accent',
      'FORM_REOPEN': 'warning',
      'USER_CREATE': 'success',
      'USER_UPDATE': 'info',
      'USER_DELETE': 'danger'
    };
    return actionColors[action] || 'default';
  }

  parseDetails(details: string | undefined): any {
    if (!details) return null;
    try {
      return JSON.parse(details);
    } catch {
      return details;
    }
  }

  getDetailsDisplay(details: string | undefined): string {
    if (!details) return '-';
    const parsed = this.parseDetails(details);
    if (typeof parsed === 'object') {
      return JSON.stringify(parsed, null, 2);
    }
    return parsed;
  }
}
