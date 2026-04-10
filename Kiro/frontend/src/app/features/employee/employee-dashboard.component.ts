import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-employee-dashboard',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule],
  template: `
    <div class="dashboard-container" style="padding: 24px;">
      <mat-card>
        <mat-card-header>
          <mat-icon mat-card-avatar>person</mat-icon>
          <mat-card-title>Employee Dashboard</mat-card-title>
          <mat-card-subtitle>Your appraisal overview</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content style="padding: 16px;">
          <p>Welcome to the Employee Appraisal Portal.</p>
          <p style="color: #888; margin-top: 8px;">Appraisal form features are coming in Phase 2.</p>
        </mat-card-content>
      </mat-card>
    </div>
  `
})
export class EmployeeDashboardComponent {}
