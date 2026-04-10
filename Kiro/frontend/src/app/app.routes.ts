import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/login',
    pathMatch: 'full'
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'admin',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () => import('./features/admin/user-management-dashboard.component').then(m => m.UserManagementDashboardComponent)
  },
  {
    path: 'admin/audit-logs',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () => import('./features/admin/audit-log-viewer.component').then(m => m.AuditLogViewerComponent)
  },
  {
    path: 'hr',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['HR', 'ADMIN'] },
    loadComponent: () => import('./features/hr/hr-dashboard.component').then(m => m.HrDashboardComponent)
  },
  {
    path: 'manager',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['MANAGER', 'ADMIN'] },
    loadComponent: () => import('./features/manager/manager-dashboard.component').then(m => m.ManagerDashboardComponent)
  },
  {
    path: 'employee',
    canActivate: [authGuard],
    loadComponent: () => import('./features/employee/employee-dashboard.component').then(m => m.EmployeeDashboardComponent)
  },
  {
    path: '**',
    redirectTo: '/login'
  }
];
