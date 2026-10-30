import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { User } from '../../core/api.models';
import { AuthService } from '../../core/auth/auth.service';
import { AdminShell } from './admin-shell';

function userWith(permisos: string[]): User {
  return {
    id: '1',
    nombre: 'Elena',
    apellido: 'Editora',
    correo: 'editor@demo.local',
    rol: 'EDITOR_CONTENIDO',
    rolNombre: 'Editor de contenido',
    permisos,
    tier: { id: 3, codigo: 'PREMIUM', nombre: 'Premium', descripcion: null, ordenAcceso: 30 },
    createdAt: '2026-10-29T10:00:00Z',
  };
}

describe('AdminShell', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AdminShell],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
  });

  async function navLabels(permisos: string[]): Promise<string[]> {
    const auth = TestBed.inject(AuthService);
    (auth as unknown as { currentUser: { set(value: User): void } }).currentUser.set(userWith(permisos));
    const fixture = TestBed.createComponent(AdminShell);
    await fixture.whenStable();
    return Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('mat-nav-list .mdc-list-item__primary-text'),
    ).map((title) => title.textContent?.trim() ?? '');
  }

  it('muestra al editor sólo los módulos editoriales', async () => {
    const labels = await navLabels(['admin.access', 'content.read.admin', 'audit.read.editorial']);

    expect(labels).toEqual(['Inicio', 'Contenido', 'Auditoría']);
  });

  it('muestra a soporte usuarios y actividad, sin contenido ni auditoría', async () => {
    const labels = await navLabels(['admin.access', 'users.read', 'users.status.update']);

    expect(labels).toEqual(['Inicio', 'Usuarios', 'Actividad']);
  });
});
