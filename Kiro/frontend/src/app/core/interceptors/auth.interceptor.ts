import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  const clonedReq = req.clone({ withCredentials: true });

  return next(clonedReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        const isAuthCheck = req.url.includes('/auth/me');
        if (!isAuthCheck) {
          router.navigate(['/login']);
        }
      } else if (error.status === 403) {
        console.error('Access denied: insufficient permissions');
      } else if (error.status === 0) {
        console.error('Network error: Unable to connect to server');
      }

      return throwError(() => error);
    })
  );
};
