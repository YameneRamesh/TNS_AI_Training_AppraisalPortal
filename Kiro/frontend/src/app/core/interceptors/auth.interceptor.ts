import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

/**
 * HTTP interceptor for session handling and error responses.
 * - Includes credentials (session cookies) in all requests
 * - Redirects to login on 401 Unauthorized
 * - Handles error responses consistently
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  // Clone request to include credentials (session cookies)
  const authReq = req.clone({
    withCredentials: true
  });

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        // Session expired or not authenticated - redirect to login
        console.warn('Session expired or unauthorized. Redirecting to login.');
        router.navigate(['/login']);
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
