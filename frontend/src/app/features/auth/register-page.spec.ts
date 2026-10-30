import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormGroup } from '@angular/forms';
import { Router, provideRouter } from '@angular/router';
import { Tier } from '../../core/api.models';
import { RegisterPage } from './register-page';

const TIERS: Tier[] = [
  { id: 1, codigo: 'BASICO', nombre: 'Básico', descripcion: 'Artículos esenciales.', ordenAcceso: 10 },
  { id: 3, codigo: 'PREMIUM', nombre: 'Premium', descripcion: 'Todo el catálogo.', ordenAcceso: 30 },
];

describe('RegisterPage', () => {
  let fixture: ComponentFixture<RegisterPage>;
  let httpMock: HttpTestingController;
  let navigateByUrl: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RegisterPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterPage);
    httpMock = TestBed.inject(HttpTestingController);
    navigateByUrl = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    // La petición de tiers queda pendiente: se responde antes de esperar a que la vista esté estable.
    TestBed.tick();
    httpMock.expectOne('/api/v1/tiers').flush(TIERS);
    await fixture.whenStable();
  });

  afterEach(() => httpMock.verify());

  function form(): FormGroup {
    return fixture.componentInstance['form'] as FormGroup;
  }

  function fillValidForm(): void {
    form().setValue({
      nombre: ' Ana ',
      apellido: 'Pérez',
      correo: 'ana@example.com',
      password: 'contraseña-segura',
      confirmPassword: 'contraseña-segura',
      tierId: 3,
    });
  }

  async function submit(): Promise<void> {
    (fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement).click();
    await fixture.whenStable();
  }

  function text(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  it('detecta que las contraseñas no coinciden', async () => {
    fillValidForm();
    form().controls['confirmPassword'].setValue('otra-contraseña');
    await submit();

    expect(form().controls['confirmPassword'].hasError('passwordMismatch')).toBe(true);
    expect(text()).toContain('Las contraseñas no coinciden.');
    httpMock.expectNone('/api/v1/auth/register');
  });

  it('exige una contraseña de al menos 8 caracteres', async () => {
    fillValidForm();
    form().patchValue({ password: 'corta', confirmPassword: 'corta' });
    await submit();

    expect(text()).toContain('Usa al menos 8 caracteres.');
  });

  it('registra al usuario sin enviar la confirmación y lo lleva al dashboard', async () => {
    fillValidForm();
    await submit();

    const request = httpMock.expectOne('/api/v1/auth/register');
    expect(request.request.body).toEqual({
      nombre: 'Ana',
      apellido: 'Pérez',
      correo: 'ana@example.com',
      password: 'contraseña-segura',
      tierId: 3,
    });
    request.flush({ id: '1', nombre: 'Ana' }, { status: 201, statusText: 'Created' });
    await fixture.whenStable();

    expect(navigateByUrl).toHaveBeenCalledWith('/app');
  });

  it('muestra junto al campo el error de correo duplicado que devuelve la API', async () => {
    fillValidForm();
    await submit();

    httpMock.expectOne('/api/v1/auth/register').flush(
      {
        code: 'CORREO_DUPLICADO',
        message: 'Ya existe una cuenta con ese correo.',
        fieldErrors: [{ field: 'correo', message: 'Ya existe una cuenta con ese correo.' }],
      },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();

    expect(form().controls['correo'].getError('server')).toBe('Ya existe una cuenta con ese correo.');
    expect(text()).toContain('Ya existe una cuenta con ese correo.');
    expect(navigateByUrl).not.toHaveBeenCalled();
  });
});
