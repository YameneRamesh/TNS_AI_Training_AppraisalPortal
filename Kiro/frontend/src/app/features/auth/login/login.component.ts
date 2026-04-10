import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="login-container">
      <mat-card class="login-card">
        <mat-card-header>
          <mat-card-title>Employee Appraisal System</mat-card-title>
          <mat-card-subtitle>Please sign in to continue</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <form (ngSubmit)="onSubmit()" #loginForm="ngForm">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Employee ID</mat-label>
              <input matInput [(ngModel)]="credentials.employeeId" name="employeeId" required>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Password</mat-label>
              <input matInput type="password" [(ngModel)]="credentials.password" name="password" required>
            </mat-form-field>

            <div *ngIf="errorMessage" class="error-message">
              {{ errorMessage }}
            </div>

            <button mat-raised-button color="primary" type="submit"
                    [disabled]="!loginForm.valid || loading" class="full-width">
              <span *ngIf="!loading">Sign In</span>
              <mat-spinner *ngIf="loading" diameter="20"></mat-spinner>
            </button>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .login-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }

    .login-card {
      width: 100%;
      max-width: 400px;
      margin: 20px;
    }

    .full-width {
      width: 100%;
      margin-bottom: 16px;
    }

    .error-message {
      color: #f44336;
      margin-bottom: 16px;
      font-size: 14px;
    }

    mat-spinner {
      display: inline-block;
      margin: 0 auto;
    }
  `]
})
export class LoginComponent {
  credentials = { employeeId: '', password: '' };
  loading = false;
  errorMessage = '';

  constructor(private authService: AuthService, private router: Router) {}

  onSubmit(): void {
    if (!this.credentials.employeeId || !this.credentials.password) return;

    this.loading = true;
    this.errorMessage = '';

    this.authService.login(this.credentials.employeeId, this.credentials.password).subscribe({
      next: (user) => {
        this.loading = false;
        const roles = user.roles || [];
        if (roles.includes('ADMIN')) this.router.navigate(['/admin']);
        else if (roles.includes('HR')) this.router.navigate(['/hr']);
        else if (roles.includes('MANAGER')) this.router.navigate(['/manager/dashboard']);
        else this.router.navigate(['/employee/dashboard']);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = 'Invalid credentials. Please try again.';
        console.error('Login error:', err);
      }
    });
  }
}
