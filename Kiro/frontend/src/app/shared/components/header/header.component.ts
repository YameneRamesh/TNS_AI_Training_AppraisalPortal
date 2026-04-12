import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { UserProfile } from '../../../core/models/user.model';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, MatToolbarModule, MatButtonModule, MatIconModule, MatMenuModule, MatDividerModule],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss']
})
export class HeaderComponent {
  user = input<UserProfile | null>(null);
  menuToggle = output<void>();
  logout = output<void>();

  getPrimaryRole(): string {
    const u = this.user();
    if (!u || !u.roles || u.roles.length === 0) return '';
    const priority = ['ADMIN', 'HR', 'MANAGER', 'EMPLOYEE'];
    for (const role of priority) {
      if (u.roles.includes(role)) return role;
    }
    return u.roles[0];
  }

  getRoleBadges(): string[] {
    const u = this.user();
    return u?.roles || [];
  }
}
