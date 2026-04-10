import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
// import { inject } from '@angular/core';
// import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // const router = inject(Router); // Disabled for development

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        console.warn('Session expired or unauthorized.');
        // router.navigate(['/login']); // Disabled for development
      } else if (error.status === 403) {
        console.error('Access denied: insufficient permissions');
      } else if (error.status === 0) {
        console.error('Network error: Unable to connect to server');
      }

      return throwError(() => error);
    })
  );
};
