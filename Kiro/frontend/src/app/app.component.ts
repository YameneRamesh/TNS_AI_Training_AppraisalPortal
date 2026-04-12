import { Component, computed, inject, viewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterOutlet, NavigationEnd } from '@angular/router';
import { MatSidenavModule, MatSidenav } from '@angular/material/sidenav';
import { filter, map, startWith } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';
import { AuthService } from './core/services/auth.service';
import { HeaderComponent } from './shared/components/header/header.component';
import { SidebarComponent } from './shared/components/sidebar/sidebar.component';
import { UserProfile } from './core/models/user.model';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    MatSidenavModule,
    HeaderComponent,
    SidebarComponent
  ],
  template: `
    @if (showLayout()) {
      <div class="app-shell">
        <app-header
          [user]="currentUser()"
          (menuToggle)="sidenav.toggle()"
          (logout)="onLogout()" />

        <mat-sidenav-container class="app-sidenav-container">
          <mat-sidenav #sidenav mode="side" [opened]="true" class="app-sidenav">
            <app-sidebar [user]="currentUser()" />
          </mat-sidenav>

          <mat-sidenav-content class="app-content">
            <router-outlet />
          </mat-sidenav-content>
        </mat-sidenav-container>
      </div>
    } @else {
      <router-outlet />
    }
  `,
  styles: [`
    .app-shell {
      display: flex;
      flex-direction: column;
      height: 100vh;
    }

    .app-sidenav-container {
      flex: 1;
    }

    .app-sidenav {
      width: 260px;
      border-right: none;
    }

    .app-content {
      background-color: var(--surface-secondary, #F8FAFD);
      padding: 1.5rem;
    }
  `]
})
export class AppComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  sidenav = viewChild.required<MatSidenav>('sidenav');

  currentUser = toSignal(this.authService.currentUser$, { initialValue: null as UserProfile | null });

  private currentUrl = toSignal(
    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .pipe(
        map((e: NavigationEnd) => e.urlAfterRedirects),
        startWith(this.router.url)
      ),
    { initialValue: this.router.url }
  );

  showLayout = computed(() => this.currentUrl() !== '/login' && this.currentUser() !== null);

  onLogout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/login']),
      error: () => {
        this.authService.clearUser();
        this.router.navigate(['/login']);
      }
    });
  }
}
