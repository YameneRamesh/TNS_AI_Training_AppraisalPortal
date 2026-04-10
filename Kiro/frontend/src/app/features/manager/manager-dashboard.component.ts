import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-manager-dashboard',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule],
  template: `
    <div class="dashboard-container" style="padding: 24px;">
      <mat-card>
        <mat-card-header>
          <mat-icon mat-card-avatar>supervisor_account</mat-icon>
          <mat-card-title>Manager Dashboard</mat-card-title>
          <mat-card-subtitle>Team appraisal overview</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content style="padding: 16px;">
          <p>Welcome to the Manager Dashboard.</p>
          <p style="color: #888; margin-top: 8px;">Team review features are coming in Phase 2.</p>
        </mat-card-content>
      </mat-card>
    </div>
  `
})
export class ManagerDashboardComponent {}
