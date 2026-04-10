import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { User } from '../../../core/models/user.model';

interface MenuItem {
  label: string;
  icon: string;
  route: string;
  roles: string[];
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, MatListModule, MatIconModule],
  template: `
    <mat-nav-list>
      @for (item of visibleMenuItems; track item.route) {
        <a mat-list-item [routerLink]="item.route" routerLinkActive="active">
          <mat-icon matListItemIcon>{{ item.icon }}</mat-icon>
          <span matListItemTitle>{{ item.label }}</span>
        </a>
      }
    </mat-nav-list>
  `,
  styles: [`
    .active {
      background-color: rgba(0, 0, 0, 0.04);
    }
  `]
})
export class SidebarComponent {
  @Input() user: User | null = null;

  private menuItems: MenuItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/dashboard', roles: ['EMPLOYEE', 'MANAGER', 'HR', 'ADMIN'] },
    { label: 'My Appraisal', icon: 'assignment', route: '/employee/appraisal', roles: ['EMPLOYEE', 'MANAGER'] },
    { label: 'Team Reviews', icon: 'people', route: '/manager/reviews', roles: ['MANAGER'] },
    { label: 'Cycle Management', icon: 'event', route: '/hr/cycles', roles: ['HR'] },
    { label: 'User Management', icon: 'admin_panel_settings', route: '/admin/users', roles: ['ADMIN'] },
    { label: 'Audit Logs', icon: 'history', route: '/admin/audit', roles: ['ADMIN'] }
  ];

  get visibleMenuItems(): MenuItem[] {
    if (!this.user) return [];
    
    const userRoles = this.user.roles.map(r => r.name as string);
    return this.menuItems.filter(item => 
      item.roles.some(role => userRoles.includes(role))
    );
  }
}
