import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { User } from '../../core/models/user.model';
import { UserService } from '../../core/services/user.service';
import { UserFormDialogComponent } from './user-form-dialog.component';
import { RoleAssignmentDialogComponent } from './role-assignment-dialog.component';

@Component({
  selector: 'app-user-management-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatSnackBarModule,
    MatDialogModule
  ],
  templateUrl: './user-management-dashboard.component.html',
  styleUrls: ['./user-management-dashboard.component.scss']
})
export class UserManagementDashboardComponent implements OnInit {
  displayedColumns: string[] = ['employeeId', 'fullName', 'email', 'designation', 'department', 'roles', 'status', 'actions'];
  users: User[] = [];
  isLoading = false;

  // Pagination
  pageSize = 10;
  pageIndex = 0;
  totalUsers = 0;

  // Filters
  searchControl = new FormControl('');
  roleFilter = new FormControl('');
  statusFilter = new FormControl('');

  constructor(
    private userService: UserService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadUsers();
    this.setupFilters();
  }

  setupFilters(): void {
    this.searchControl.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => {
        this.pageIndex = 0;
        this.loadUsers();
      });

    this.roleFilter.valueChanges.subscribe(() => {
      this.pageIndex = 0;
      this.loadUsers();
    });
    
    this.statusFilter.valueChanges.subscribe(() => {
      this.pageIndex = 0;
      this.loadUsers();
    });
  }

  loadUsers(): void {
    this.isLoading = true;
    
    const params = {
      search: this.searchControl.value || undefined,
      role: this.roleFilter.value || undefined,
      status: this.statusFilter.value || undefined,
      page: this.pageIndex,
      size: this.pageSize
    };

    this.userService.getUsers(params).subscribe({
      next: (response) => {
        console.log('Users response:', response);
        console.log('Content:', response.content);
        console.log('Users array length:', response.content?.length);
        this.users = response.content || [];
        this.totalUsers = response.totalElements || 0;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.isLoading = false;
        this.cdr.detectChanges();
        this.snackBar.open('Failed to load users', 'Close', { duration: 3000 });
        console.error('Error loading users:', error);
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadUsers();
  }

  createUser(): void {
    const dialogRef = this.dialog.open(UserFormDialogComponent, {
      width: '600px',
      data: { mode: 'create' }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadUsers();
      }
    });
  }

  editUser(user: User): void {
    const dialogRef = this.dialog.open(UserFormDialogComponent, {
      width: '600px',
      data: { mode: 'edit', user }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadUsers();
      }
    });
  }

  manageRoles(user: User): void {
    const dialogRef = this.dialog.open(RoleAssignmentDialogComponent, {
      width: '600px',
      data: { user }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadUsers();
      }
    });
  }

  toggleUserStatus(user: User): void {
    const action$ = user.isActive
      ? this.userService.deactivateUser(user.id)
      : this.userService.reactivateUser(user.id);

    action$.subscribe({
      next: () => {
        const msg = user.isActive ? 'User deactivated' : 'User reactivated';
        this.snackBar.open(msg, 'Close', { duration: 3000 });
        this.loadUsers();
      },
      error: (error) => {
        this.snackBar.open(error.error?.message || 'Failed to update user status', 'Close', { duration: 3000 });
      }
    });
  }

  getRoleColor(role: string): string {
    const roleColors: { [key: string]: string } = {
      'ADMIN': 'danger',
      'HR': 'info',
      'MANAGER': 'accent',
      'EMPLOYEE': 'success'
    };
    return roleColors[role] || 'default';
  }

  clearFilters(): void {
    this.searchControl.setValue('');
    this.roleFilter.setValue('');
    this.statusFilter.setValue('');
  }

  getRolesArray(roles: any): string[] {
    if (!roles) return [];
    if (Array.isArray(roles)) return roles;
    if (typeof roles === 'object') return Object.values(roles);
    return [];
  }
}
