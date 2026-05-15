import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from '../core/services/auth.service';

export const guestGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.validateSession().pipe(
    map((session) => {
      if (!session) {
        return true;
      }

      return router.createUrlTree([session.role === 'ADMIN' ? '/painel' : '/home']);
    })
  );
};
