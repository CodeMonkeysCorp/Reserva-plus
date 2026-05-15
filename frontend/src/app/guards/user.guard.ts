import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from '../core/services/auth.service';

export const userGuard: CanActivateFn = (_route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.validateSession().pipe(
    map((session) => {
      if (!session) {
        return router.createUrlTree(['/login'], {
          queryParams: { redirect: state.url }
        });
      }

      if (session.role !== 'ADMIN') {
        return true;
      }

      return router.createUrlTree(['/gerenciar-reservas']);
    })
  );
};
