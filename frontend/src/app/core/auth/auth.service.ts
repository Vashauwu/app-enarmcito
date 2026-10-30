import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, firstValueFrom, finalize, map, tap } from 'rxjs';
import {
  API_URL,
  AuthProviders,
  Identities,
  LoginRequest,
  RegisterRequest,
  UpdateProfileRequest,
  User,
} from '../api.models';

/**
 * Estado de la sesión del usuario. La sesión vive en una cookie HttpOnly del backend; aquí sólo se
 * guarda una copia de los datos públicos del usuario para la interfaz.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly currentUser = signal<User | null>(null);

  readonly user = this.currentUser.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUser() !== null);
  private readonly permissions = computed(() => new Set(this.currentUser()?.permisos ?? []));

  /**
   * `true` si el usuario tiene al menos uno de los permisos indicados. Sólo decide qué se muestra:
   * el backend vuelve a verificar cada petición.
   */
  can(...permisos: string[]): boolean {
    const own = this.permissions();
    return permisos.some((permiso) => own.has(permiso));
  }

  /** Recupera la sesión existente al arrancar la aplicación. Nunca falla. */
  async restoreSession(): Promise<void> {
    try {
      this.currentUser.set(await firstValueFrom(this.http.get<User>(`${API_URL}/auth/me`)));
    } catch {
      this.currentUser.set(null);
    }
  }

  login(credentials: LoginRequest): Observable<User> {
    return this.http
      .post<User>(`${API_URL}/auth/login`, credentials)
      .pipe(tap((user) => this.currentUser.set(user)));
  }

  register(data: RegisterRequest): Observable<User> {
    return this.http
      .post<User>(`${API_URL}/auth/register`, data)
      .pipe(tap((user) => this.currentUser.set(user)));
  }

  logout(): Observable<void> {
    return this.http.post<unknown>(`${API_URL}/auth/logout`, null).pipe(
      map(() => undefined),
      finalize(() => this.currentUser.set(null)),
    );
  }

  updateProfile(data: UpdateProfileRequest): Observable<User> {
    return this.http
      .put<User>(`${API_URL}/me/profile`, data)
      .pipe(tap((user) => this.currentUser.set(user)));
  }

  /** Proveedores externos disponibles (para mostrar u ocultar «Continuar con Google»). */
  providers(): Observable<AuthProviders> {
    return this.http.get<AuthProviders>(`${API_URL}/auth/proveedores`);
  }

  identities(): Observable<Identities> {
    return this.http.get<Identities>(`${API_URL}/me/identidades`);
  }

  unlinkGoogle(): Observable<void> {
    return this.http
      .post<unknown>(`${API_URL}/me/identidades/google/desvincular`, null)
      .pipe(map(() => undefined));
  }

  /** Olvida la sesión local cuando el backend indica que ya no es válida. */
  clearSession(): void {
    this.currentUser.set(null);
  }
}
