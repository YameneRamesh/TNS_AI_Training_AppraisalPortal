import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { map, first } from 'rxjs/operators';

/**
 * Auth guard to protect routes that require authentication.
 * Waits for initial auth check to complete before deciding.
 * Redirects to login if user is not authenticated.
 */
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // If we have a stored user, allow immediately (optimistic)
  // The auth check will validate the session in the background
  if (authService.currentUserValue) {
    return true;
  }

  // Otherwise wait for the auth check to complete
  return authService.isAuthenticated$.pipe(
    first(),
    map(isAuthenticated => {
      if (isAuthenticated) {
        return true;
      } else {
        router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
        return false;
      }
    })
  );
};
