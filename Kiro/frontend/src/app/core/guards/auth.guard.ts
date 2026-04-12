import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { of } from 'rxjs';
import { map, switchMap, take } from 'rxjs/operators';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.isAuthenticated$.pipe(
    take(1),
    switchMap(isAuthenticated => {
      if (isAuthenticated) {
        return of(true);
      }
      return authService.resolveSessionUser().pipe(
        map(user => {
          if (user) {
            return true;
          }
          router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
          return false;
        })
      );
    })
  );
};
