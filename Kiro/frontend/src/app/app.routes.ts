import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/hr/cycles', // Redirecting to cycles for development testing (login disabled)
    pathMatch: 'full'
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'hr/templates',
    loadComponent: () => import('./features/hr/template-list/template-list.component').then(m => m.TemplateListComponent)
  },
  {
    path: 'hr/templates/:id',
    loadComponent: () => import('./features/hr/template-viewer/template-viewer.component').then(m => m.TemplateViewerComponent)
  },
  {
    path: 'hr/cycles',
    loadComponent: () => import('./features/hr/cycle-dashboard/cycle-dashboard.component').then(m => m.CycleDashboardComponent)
  },
  {
    path: 'hr/cycles/create',
    loadComponent: () => import('./features/hr/cycle-create/cycle-create.component').then(m => m.CycleCreateComponent)
  },
  {
    path: 'hr/cycles/:id',
    loadComponent: () => import('./features/hr/cycle-details/cycle-details.component').then(m => m.CycleDetailsComponent)
  }
];
