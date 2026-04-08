import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { User } from '../../../core/models/user.model';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, MatToolbarModule, MatButtonModule, MatIconModule, MatMenuModule],
  template: `
    <mat-toolbar color="primary">
      <button mat-icon-button (click)="menuToggle.emit()">
        <mat-icon>menu</mat-icon>
      </button>
      <span class="title">Employee Appraisal System</span>
      <span class="spacer"></span>
      @if (user) {
        <button mat-button [matMenuTriggerFor]="userMenu">
          <mat-icon>account_circle</mat-icon>
          <span>{{ user.fullName }}</span>
        </button>
        <mat-menu #userMenu="matMenu">
          <button mat-menu-item disabled>
            <mat-icon>badge</mat-icon>
            <span>{{ user.designation }}</span>
          </button>
          <button mat-menu-item disabled>
            <mat-icon>business</mat-icon>
            <span>{{ user.department }}</span>
          </button>
          <mat-divider></mat-divider>
          <button mat-menu-item (click)="logout.emit()">
            <mat-icon>logout</mat-icon>
            <span>Logout</span>
          </button>
        </mat-menu>
      }
    </mat-toolbar>
  `,
  styles: [`
    .title {
      font-size: 1.2rem;
      margin-left: 16px;
    }
    .spacer {
      flex: 1 1 auto;
    }
  `]
})
export class HeaderComponent {
  @Input() user: User | null = null;
  @Output() menuToggle = new EventEmitter<void>();
  @Output() logout = new EventEmitter<void>();
}
