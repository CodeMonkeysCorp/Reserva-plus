import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../core/services/auth.service';

export const authSessionInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return next(request).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        authService.isAuthenticated()
      ) {
        const currentUrl = router.url;
        const isAuthScreen = currentUrl.startsWith('/login') || currentUrl.startsWith('/register');

        authService.logout();
        void router.navigate(['/login'], {
          queryParams: !isAuthScreen && currentUrl.startsWith('/')
            ? { redirect: currentUrl }
            : undefined
        });
      }

      return throwError(() => error);
    })
  );
};
