// import { inject } from '@angular/core';
// import { Router, CanActivateFn } from '@angular/router';
// import { AuthService } from '../services/auth.service';
// import { map } from 'rxjs/operators';
// import { RoleName } from '../models/user.model';

// /**
//  * Role guard to protect routes that require specific roles.
//  * Usage: Add 'data: { roles: ['ADMIN', 'HR'] }' to route configuration.
//  */
// export const roleGuard: CanActivateFn = (route, state) => {
//   const authService = inject(AuthService);
//   const router = inject(Router);

//   const requiredRoles = route.data['roles'] as RoleName[];

//   if (!requiredRoles || requiredRoles.length === 0) {
//     return true;
//   }

//   return authService.currentUser$.pipe(
//     map(user => {
//       if (!user) {
//         router.navigate(['/login']);
//         return false;
//       }

//       const userRoles = user.roles.map(r => r.name);
//       const hasRequiredRole = requiredRoles.some(role => userRoles.includes(role));

//       if (hasRequiredRole) {
//         return true;
//       } else {
//         console.error('Access denied: insufficient role');
//         router.navigate(['/unauthorized']);
//         return false;
//       }
//     })
//   );
// };

import { CanActivateFn } from '@angular/router';

// Guard disabled for development - all roles are permitted
export const roleGuard: CanActivateFn = () => true;
