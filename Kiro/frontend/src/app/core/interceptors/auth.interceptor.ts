import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, tap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * HTTP interceptor for session handling and error responses.
 * - Includes credentials (session cookies) in all requests
 * - Refreshes session activity on successful API calls
 * - Redirects to login on 401 Unauthorized
 * - Handles error responses consistently
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const authService = inject(AuthService);

  // Clone request to include credentials (session cookies)
  const authReq = req.clone({
    withCredentials: true
  });

  return next(authReq).pipe(
    tap(() => {
      // Refresh session activity on successful API calls
      authService.refreshSession();
    }),
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        // Don't redirect for auth endpoints (login = wrong creds, me = initial check)
        if (req.url.includes('/auth/login') || req.url.includes('/auth/me')) {
          return throwError(() => error);
        }

        // Session expired or not authenticated - redirect to login
        console.warn('Session expired or unauthorized. Redirecting to login.');
        
        const isSessionExpired = error.error?.message?.includes('Session expired');
        if (isSessionExpired) {
          console.warn('Session expired due to inactivity');
        }
        
        router.navigate(['/login'], { 
          queryParams: { sessionExpired: isSessionExpired ? 'true' : 'false' }
        });
      } else if (error.status === 403) {
        // Forbidden - insufficient permissions
        console.error('Access denied: insufficient permissions');
      } else if (error.status === 0) {
        // Network error or CORS issue
        console.error('Network error: Unable to connect to server');
      }

      return throwError(() => error);
    })
  );
};
