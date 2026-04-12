import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { of } from 'rxjs';
import { map, switchMap, take } from 'rxjs/operators';

export const roleGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const requiredRoles = route.data['roles'] as string[];

  if (!requiredRoles || requiredRoles.length === 0) {
    return true;
  }

  return authService.currentUser$.pipe(
    take(1),
    switchMap(user => user ? of(user) : authService.resolveSessionUser()),
    map(user => {
      if (!user) {
        router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
        return false;
      }

      const hasRequiredRole = requiredRoles.some(role => user.roles.includes(role));

      if (hasRequiredRole) {
        return true;
      }

      router.navigate(['/login']);
      return false;
    })
  );
};
