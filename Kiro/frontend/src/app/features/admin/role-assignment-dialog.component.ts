import { Component, Inject, OnInit, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { User } from '../../core/models/user.model';
import { UserService } from '../../core/services/user.service';

export interface RoleAssignmentDialogData {
  user: User;
}

interface RoleOption {
  id: number;
  name: string;
  description: string;
  selected: boolean;
}

@Component({
  selector: 'app-role-assignment-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './role-assignment-dialog.component.html',
  styleUrls: ['./role-assignment-dialog.component.scss']
})
export class RoleAssignmentDialogComponent implements OnInit {
  roles: RoleOption[] = [];
  isLoading = false;
  isSaving = false;

  roleDescriptions: { [key: string]: string } = {
    'EMPLOYEE': 'Can complete self-appraisals and view own appraisal history',
    'MANAGER': 'Can review team member appraisals and manage direct reports',
    'HR': 'Can configure cycles, manage templates, and oversee organization-wide appraisals',
    'ADMIN': 'Full system access including user management and system configuration'
  };

  constructor(
    private userService: UserService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    public dialogRef: MatDialogRef<RoleAssignmentDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: RoleAssignmentDialogData
  ) {}

  ngOnInit(): void {
    this.loadRoles();
  }

  loadRoles(): void {
    this.isLoading = true;
    this.userService.getRoles().subscribe({
      next: (response) => {
        this.roles = response.data!.map(role => ({
          id: role.id,
          name: role.name,
          description: this.roleDescriptions[role.name] || '',
          selected: Array.isArray(this.data.user.roles) && this.data.user.roles.includes(role.name)
        }));
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.isLoading = false;
        this.snackBar.open('Failed to load roles', 'Close', { duration: 3000 });
        this.cdr.markForCheck();
      }
    });
  }

  onSave(): void {
    const selectedRoleNames = this.roles
      .filter(role => role.selected)
      .map(role => role.name);

    if (selectedRoleNames.length === 0) {
      this.snackBar.open('Please select at least one role', 'Close', { duration: 3000 });
      return;
    }

    this.isSaving = true;
    this.userService.assignRoles(this.data.user.id, selectedRoleNames).subscribe({
      next: (response) => {
        this.isSaving = false;
        this.snackBar.open('Roles updated successfully', 'Close', { duration: 3000 });
        this.dialogRef.close(response.data);
      },
      error: (error) => {
        this.isSaving = false;
        this.snackBar.open(error.error?.message || 'Failed to update roles', 'Close', { duration: 3000 });
        this.cdr.markForCheck();
      }
    });
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  getRoleIcon(roleName: string): string {
    const icons: { [key: string]: string } = {
      'EMPLOYEE': 'person',
      'MANAGER': 'supervisor_account',
      'HR': 'groups',
      'ADMIN': 'admin_panel_settings'
    };
    return icons[roleName] || 'verified_user';
  }

  getRoleColor(roleName: string): string {
    const colors: { [key: string]: string } = {
      'EMPLOYEE': 'success',
      'MANAGER': 'accent',
      'HR': 'info',
      'ADMIN': 'danger'
    };
    return colors[roleName] || 'default';
  }
}
