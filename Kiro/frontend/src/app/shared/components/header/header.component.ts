import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { MatBadgeModule } from '@angular/material/badge';
import { User } from '../../../core/models/user.model';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    CommonModule, 
    MatToolbarModule, 
    MatButtonModule, 
    MatIconModule, 
    MatMenuModule,
    MatDividerModule,
    MatBadgeModule
  ],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss']
})
export class HeaderComponent {
  @Input() user: User | null = null;
  @Output() menuToggle = new EventEmitter<void>();
  @Output() logout = new EventEmitter<void>();

  getRoleBadges(): string[] {
    return this.user?.roles || [];
  }

  getPrimaryRole(): string {
    if (!this.user?.roles || this.user.roles.length === 0) {
      return 'User';
    }
    // Priority order: ADMIN > HR > MANAGER > EMPLOYEE
    if (this.user.roles.includes('ADMIN')) return 'Admin';
    if (this.user.roles.includes('HR')) return 'HR';
    if (this.user.roles.includes('MANAGER')) return 'Manager';
    return 'Employee';
  }
}
