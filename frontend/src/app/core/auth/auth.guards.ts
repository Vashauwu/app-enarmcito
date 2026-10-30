import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/** Rutas privadas: sin sesión se redirige al login conservando la URL solicitada. */
export const authGuard: CanActivateFn = (_route, state) => {
  if (inject(AuthService).isAuthenticated()) {
    return true;
  }
  return inject(Router).createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

/**
 * Exige al menos uno de los permisos. Sin ellos se redirige al inicio del panel (o a la zona de aprendizaje
 * si tampoco puede entrar al panel). Es una ayuda de navegación: la API valida cada petición.
 */
export function permissionGuard(...permisos: string[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    if (auth.can(...permisos)) {
      return true;
    }
    return inject(Router).createUrlTree([auth.can('admin.access') ? '/admin' : '/app']);
  };
}

/** Login y registro: con sesión activa se lleva directamente al dashboard. */
export const guestGuard: CanActivateFn = () => {
  if (inject(AuthService).isAuthenticated()) {
    return inject(Router).createUrlTree(['/app']);
  }
  return true;
};
