import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { GoogleSignIn } from './google-sign-in';

describe('GoogleSignIn', () => {
  let fixture: ComponentFixture<GoogleSignIn>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GoogleSignIn],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(GoogleSignIn);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  async function respond(google: boolean): Promise<void> {
    TestBed.tick();
    httpMock.expectOne('/api/v1/auth/proveedores').flush({ google });
    await fixture.whenStable();
  }

  function link(): HTMLAnchorElement | null {
    return (fixture.nativeElement as HTMLElement).querySelector('a.google-btn');
  }

  it('no muestra nada si el ambiente no tiene Google configurado', async () => {
    await respond(false);

    expect(link()).toBeNull();
  });

  it('enlaza a la API para iniciar sesión', async () => {
    await respond(true);

    expect(link()?.getAttribute('href')).toBe('/api/v1/auth/google/iniciar');
    expect(link()?.textContent).toContain('Continuar con Google');
  });

  it('en modo vincular pide la vinculación a la cuenta con sesión', async () => {
    fixture.componentRef.setInput('modo', 'vincular');
    await respond(true);

    expect(link()?.getAttribute('href')).toBe('/api/v1/auth/google/iniciar?modo=vincular');
  });

  it('muestra el separador sólo si se pide', async () => {
    fixture.componentRef.setInput('divider', true);
    await respond(true);

    expect((fixture.nativeElement as HTMLElement).querySelector('.google-divider')).not.toBeNull();
  });
});
