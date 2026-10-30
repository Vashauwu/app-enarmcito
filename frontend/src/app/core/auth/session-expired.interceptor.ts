import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/**
 * Si una petición privada responde 401, la sesión expiró: se limpia el estado local y se vuelve al login.
 * Las rutas `/auth/*` gestionan su propio 401 (credenciales incorrectas o sesión inexistente al arrancar), y el
 * catálogo público responde 401 `REQUIERE_REGISTRO` a visitantes sin sesión, que no implica una sesión vencida.
 */
export const sessionExpiredInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return next(req).pipe(
    catchError((error: unknown) => {
      // REQUIERE_REGISTRO: material que pide cuenta; la página muestra su propia invitación a entrar.
      const requiereRegistro =
        error instanceof HttpErrorResponse && (error.error as { code?: string } | null)?.code === 'REQUIERE_REGISTRO';
      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        !req.url.includes('/auth/') &&
        !requiereRegistro
      ) {
        auth.clearSession();
        void router.navigate(['/login'], { queryParams: { returnUrl: router.url, sesion: 'expirada' } });
      }
      return throwError(() => error);
    }),
  );
};
