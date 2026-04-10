import { Component, Inject, OnInit, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { User } from '../../core/models/user.model';
import { UserService, UserCreateRequest, UserUpdateRequest } from '../../core/services/user.service';

export interface UserFormDialogData {
  user?: User;
  mode: 'create' | 'edit';
}

@Component({
  selector: 'app-user-form-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './user-form-dialog.component.html',
  styleUrls: ['./user-form-dialog.component.scss']
})
export class UserFormDialogComponent implements OnInit {
  userForm: FormGroup;
  isLoading = false;
  hidePassword = true;
  availableManagers: User[] = [];

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    public dialogRef: MatDialogRef<UserFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: UserFormDialogData
  ) {
    this.userForm = this.fb.group({
      employeeId: ['', [Validators.required, Validators.pattern(/^[A-Z0-9-]+$/)]],
      fullName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', this.data.mode === 'create' ? [Validators.required, Validators.minLength(8)] : []],
      designation: [''],
      department: [''],
      managerId: [null],
      roles: [this.data.mode === 'create' ? [] : null, this.data.mode === 'create' ? [this.rolesRequired] : []]
    });

    if (this.data.mode === 'edit' && this.data.user) {
      this.userForm.patchValue({
        employeeId: this.data.user.employeeId,
        fullName: this.data.user.fullName,
        email: this.data.user.email,
        designation: this.data.user.designation,
        department: this.data.user.department
      });
      // Disable employeeId in edit mode
      this.userForm.get('employeeId')?.disable();
    }
  }

  ngOnInit(): void {
    // Use setTimeout to avoid ExpressionChangedAfterItHasBeenCheckedError
    // when dialog is opened inside a change detection cycle
    setTimeout(() => this.loadManagers());
  }

  loadManagers(): void {
    this.userService.getUsers({ role: 'MANAGER', size: 100 }).subscribe({
      next: (response) => {
        this.availableManagers = response.content;
        this.cdr.markForCheck();
      },
      error: () => {
        this.cdr.markForCheck();
      }
    });
  }

  onSubmit(): void {
    if (this.userForm.invalid) {
      this.userForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;

    if (this.data.mode === 'create') {
      const createRequest: UserCreateRequest = this.userForm.value;
      this.userService.createUser(createRequest).subscribe({
        next: (response) => {
          this.isLoading = false;
          this.snackBar.open('User created successfully', 'Close', { duration: 3000 });
          this.dialogRef.close(response.data);
        },
        error: (error) => {
          this.isLoading = false;
          this.snackBar.open(error.error?.message || 'Failed to create user', 'Close', { duration: 3000 });
          this.cdr.markForCheck();
        }
      });
    } else if (this.data.mode === 'edit' && this.data.user) {
      const updateRequest: UserUpdateRequest = {
        fullName: this.userForm.get('fullName')?.value,
        email: this.userForm.get('email')?.value,
        designation: this.userForm.get('designation')?.value,
        department: this.userForm.get('department')?.value,
        managerId: this.userForm.get('managerId')?.value
      };
      
      this.userService.updateUser(this.data.user.id, updateRequest).subscribe({
        next: (response) => {
          this.isLoading = false;
          this.snackBar.open('User updated successfully', 'Close', { duration: 3000 });
          this.dialogRef.close(response.data);
        },
        error: (error) => {
          this.isLoading = false;
          this.snackBar.open(error.error?.message || 'Failed to update user', 'Close', { duration: 3000 });
          this.cdr.markForCheck();
        }
      });
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  togglePasswordVisibility(): void {
    this.hidePassword = !this.hidePassword;
  }

  rolesRequired(control: AbstractControl): ValidationErrors | null {
    const val = control.value;
    return Array.isArray(val) && val.length > 0 ? null : { required: true };
  }

  getTitle(): string {
    return this.data.mode === 'create' ? 'Create New User' : 'Edit User';
  }
}
