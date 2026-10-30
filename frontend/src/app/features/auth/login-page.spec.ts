import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { LoginPage } from './login-page';

describe('LoginPage', () => {
  let fixture: ComponentFixture<LoginPage>;
  let httpMock: HttpTestingController;
  let navigateByUrl: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginPage);
    httpMock = TestBed.inject(HttpTestingController);
    navigateByUrl = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    // El botón de Google consulta los proveedores; estas pruebas no dependen de la respuesta.
    TestBed.tick();
    httpMock.expectOne('/api/v1/auth/proveedores').flush({ google: false });
    await fixture.whenStable();
  });

  afterEach(() => {
    httpMock.verify();
  });

  function type(selector: string, value: string): void {
    const input = fixture.nativeElement.querySelector(selector) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  async function submit(): Promise<void> {
    (fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement).click();
    await fixture.whenStable();
  }

  function text(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  it('valida los campos antes de llamar a la API', async () => {
    await submit();

    expect(text()).toContain('Escribe tu correo.');
    expect(text()).toContain('Escribe tu contraseña.');
    httpMock.expectNone('/api/v1/auth/login');
  });

  it('inicia sesión y redirige al dashboard', async () => {
    type('input[formControlName="correo"]', 'ana@example.com');
    type('input[formControlName="password"]', 'contraseña-segura');
    await submit();

    const request = httpMock.expectOne('/api/v1/auth/login');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ correo: 'ana@example.com', password: 'contraseña-segura' });
    request.flush({ id: '1', nombre: 'Ana' });
    await fixture.whenStable();

    expect(navigateByUrl).toHaveBeenCalledWith('/app');
  });

  it('muestra el mensaje genérico de la API ante credenciales incorrectas', async () => {
    type('input[formControlName="correo"]', 'ana@example.com');
    type('input[formControlName="password"]', 'incorrecta');
    await submit();

    httpMock
      .expectOne('/api/v1/auth/login')
      .flush(
        { code: 'CREDENCIALES_INVALIDAS', message: 'Correo o contraseña incorrectos.' },
        { status: 401, statusText: 'Unauthorized' },
      );
    await fixture.whenStable();

    expect(text()).toContain('Correo o contraseña incorrectos.');
    expect(navigateByUrl).not.toHaveBeenCalled();
  });

  it('sólo vuelve a rutas internas de la zona privada', async () => {
    fixture.componentRef.setInput('returnUrl', 'https://sitio-externo.example');
    type('input[formControlName="correo"]', 'ana@example.com');
    type('input[formControlName="password"]', 'contraseña-segura');
    await submit();

    httpMock.expectOne('/api/v1/auth/login').flush({ id: '1' });
    await fixture.whenStable();

    expect(navigateByUrl).toHaveBeenCalledWith('/app');
  });

  it('explica por qué falló el acceso con Google', async () => {
    fixture.componentRef.setInput('google', 'GOOGLE_CORREO_EXISTENTE');
    await fixture.whenStable();

    expect(text()).toContain('Ya existe una cuenta con ese correo');
    expect(text()).toContain('vincula Google desde Mi perfil');
  });

  it('ignora códigos de Google desconocidos', async () => {
    fixture.componentRef.setInput('google', '<script>');
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('.form-alert')).toBeNull();
  });
});
