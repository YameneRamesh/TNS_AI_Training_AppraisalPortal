import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { UserProfile } from '../../../core/models/user.model';

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
    <nav class="sidebar-nav">
      <div class="nav-header">
        <mat-icon class="nav-logo">assessment</mat-icon>
        <span class="nav-brand">TNS Appraisal</span>
      </div>
      <mat-nav-list>
        @for (item of visibleMenuItems; track item.route) {
          <a mat-list-item [routerLink]="item.route" routerLinkActive="active">
            <mat-icon matListItemIcon>{{ item.icon }}</mat-icon>
            <span matListItemTitle>{{ item.label }}</span>
          </a>
        }
      </mat-nav-list>
    </nav>
  `,
  styles: [`
    .sidebar-nav {
      height: 100%;
      background: linear-gradient(180deg, var(--color-primary-900, #0A1628) 0%, var(--color-primary-800, #0F2240) 100%);
      color: white;
    }

    .nav-header {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 1.25rem 1rem;
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    }

    .nav-header .nav-logo {
      color: var(--color-accent-400, #FBBF24);
      font-size: 28px;
      width: 28px;
      height: 28px;
    }

    .nav-header .nav-brand {
      font-weight: 700;
      font-size: 1rem;
      letter-spacing: -0.02em;
      color: #ffffff;
    }

    :host ::ng-deep .mat-mdc-nav-list {
      padding-top: 0.5rem;
      color: rgba(255, 255, 255, 0.88) !important;
      --mdc-list-list-item-label-text-color: rgba(255, 255, 255, 0.88);
    }

    :host ::ng-deep .mat-mdc-nav-list a.mat-mdc-list-item,
    :host ::ng-deep .mat-mdc-nav-list a.mat-mdc-list-item .mdc-list-item__content,
    :host ::ng-deep .mat-mdc-nav-list a.mat-mdc-list-item .mdc-list-item__primary-text,
    :host ::ng-deep .mat-mdc-nav-list a.mat-mdc-list-item .mat-mdc-list-item-title,
    :host ::ng-deep .mat-mdc-nav-list a.mat-mdc-list-item .mat-mdc-list-item-unscoped-content,
    :host ::ng-deep .mat-mdc-nav-list a.mat-mdc-list-item .mat-icon,
    :host ::ng-deep .mat-mdc-nav-list a.mat-mdc-list-item .mdc-list-item__start {
      color: rgba(255, 255, 255, 0.78) !important;
    }

    :host ::ng-deep a.mat-mdc-list-item {
      color: rgba(255, 255, 255, 0.78) !important;
      margin: 2px 8px;
      border-radius: 8px;
      text-decoration: none;
    }

    :host ::ng-deep a.mat-mdc-list-item:hover {
      background-color: rgba(255, 255, 255, 0.08);
      color: #ffffff !important;
    }

    :host ::ng-deep a.mat-mdc-list-item.active {
      background-color: rgba(245, 158, 11, 0.15);
      color: var(--color-accent-400, #FBBF24) !important;
      border-left: 3px solid var(--color-accent-500, #F59E0B);
    }

    :host ::ng-deep a.mat-mdc-list-item .mat-icon,
    :host ::ng-deep a.mat-mdc-list-item .mdc-list-item__primary-text,
    :host ::ng-deep a.mat-mdc-list-item .mat-mdc-list-item-title,
    :host ::ng-deep a.mat-mdc-list-item .mdc-list-item__start {
      color: inherit !important;
    }
  `]
})
export class SidebarComponent {
  user = input<UserProfile | null>(null);

  private menuItems: MenuItem[] = [
    { label: 'Employee Dashboard', icon: 'person', route: '/employee', roles: ['EMPLOYEE', 'MANAGER'] },
    { label: 'Manager Dashboard', icon: 'supervisor_account', route: '/manager', roles: ['MANAGER'] },
    { label: 'Cycle Management', icon: 'event', route: '/hr/cycles', roles: ['HR'] },
    { label: 'Templates', icon: 'description', route: '/hr/templates', roles: ['HR'] },
    { label: 'User Management', icon: 'admin_panel_settings', route: '/admin/users', roles: ['ADMIN'] },
    { label: 'Audit Logs', icon: 'history', route: '/admin/audit-logs', roles: ['ADMIN'] }
  ];

  get visibleMenuItems(): MenuItem[] {
    const currentUser = this.user();
    if (!currentUser) return [];

    return this.menuItems.filter(item =>
      item.roles.some(role => currentUser.roles.includes(role))
    );
  }
}
