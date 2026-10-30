import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { User } from '../api.models';
import { authGuard, guestGuard, permissionGuard } from './auth.guards';
import { AuthService } from './auth.service';

const USER: User = {
  id: '1',
  nombre: 'Ana',
  apellido: 'Pérez',
  correo: 'ana@example.com',
  rol: 'ALUMNO',
  rolNombre: 'Alumno',
  permisos: [],
  tier: { id: 1, codigo: 'BASICO', nombre: 'Básico', descripcion: null, ordenAcceso: 10 },
  createdAt: '2026-10-29T10:00:00Z',
};

describe('guards de autenticación', () => {
  const route = {} as ActivatedRouteSnapshot;
  const state = { url: '/app/perfil' } as RouterStateSnapshot;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
  });

  function run(guard: typeof authGuard): boolean | UrlTree {
    return TestBed.runInInjectionContext(() => guard(route, state)) as boolean | UrlTree;
  }

  it('authGuard redirige al login conservando la URL cuando no hay sesión', () => {
    const result = run(authGuard);

    const router = TestBed.inject(Router);
    expect(router.serializeUrl(result as UrlTree)).toBe('/login?returnUrl=%2Fapp%2Fperfil');
  });

  it('authGuard permite el acceso con sesión', () => {
    loginAs(USER);

    expect(run(authGuard)).toBe(true);
  });

  it('guestGuard envía al dashboard a un usuario con sesión', () => {
    loginAs(USER);

    expect(TestBed.inject(Router).serializeUrl(run(guestGuard) as UrlTree)).toBe('/app');
  });

  it('guestGuard permite ver login y registro sin sesión', () => {
    expect(run(guestGuard)).toBe(true);
  });

  it('permissionGuard permite el acceso con cualquiera de los permisos indicados', () => {
    loginAs({ ...USER, permisos: ['admin.access', 'audit.read.own'] });

    expect(run(permissionGuard('audit.read', 'audit.read.own'))).toBe(true);
  });

  it('permissionGuard devuelve al inicio del panel si falta el permiso', () => {
    loginAs({ ...USER, permisos: ['admin.access', 'content.read.admin'] });

    const result = run(permissionGuard('users.read'));

    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/admin');
  });

  it('permissionGuard envía a la zona de aprendizaje a un alumno', () => {
    loginAs(USER);

    expect(TestBed.inject(Router).serializeUrl(run(permissionGuard('admin.access')) as UrlTree)).toBe('/app');
  });
});

function loginAs(user: User): void {
  const auth = TestBed.inject(AuthService);
  // Simula la respuesta del backend sin pasar por HTTP.
  vi.spyOn(auth, 'login').mockReturnValue(of(user));
  (auth as unknown as { currentUser: { set(value: User): void } }).currentUser.set(user);
}
