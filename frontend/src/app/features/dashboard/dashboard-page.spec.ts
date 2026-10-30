import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ContentSummary, Learning, Page } from '../../core/api.models';
import { CATALOG_PAGE_SIZE } from '../../shared/catalog-browser/catalog-browser';
import { DashboardPage } from './dashboard-page';

const ARTICLE: ContentSummary = {
  id: 'a1',
  slug: 'sepsis-3-definiciones',
  titulo: 'Sepsis-3: definiciones de sepsis y choque séptico',
  areaMedica: 'Urgencias',
  extracto: 'El tercer consenso internacional…',
  tiempoLecturaMinutos: 5,
  tipo: 'ARTICULO',
  acceso: { politica: 'REGISTRADO_GRATUITO', tierCodigo: null, tierNombre: null },
  proximaRevision: null,
  requiereActualizacion: false,
};

function page(items: ContentSummary[]): Page<ContentSummary> {
  return { items, page: 0, size: CATALOG_PAGE_SIZE, totalItems: items.length, totalPages: 1 };
}

describe('DashboardPage', () => {
  let fixture: ComponentFixture<DashboardPage>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    fixture = TestBed.createComponent(DashboardPage);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  function text(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  it('muestra el último artículo en «Continuar aprendizaje» y el catálogo', async () => {
    TestBed.tick();
    const learning: Learning = {
      ultimo: {
        contenido: ARTICLE,
        progreso: {
          contenidoId: 'a1',
          estado: 'EN_PROGRESO',
          ultimaConsultaAt: '2026-10-29T10:00:00Z',
          completadoAt: null,
        },
      },
      completados: 0,
      enProgreso: 1,
    };
    httpMock.expectOne('/api/v1/me/learning').flush(learning);
    httpMock.expectOne((req) => req.url === '/api/v1/content' && !req.params.has('q')).flush(page([ARTICLE]));
    await fixture.whenStable();

    const continueLink = fixture.nativeElement.querySelector('.continue-action') as HTMLAnchorElement;
    expect(text()).toContain('Continuar aprendizaje');
    expect(continueLink.getAttribute('href')).toBe('/app/contenido/sepsis-3-definiciones');
    expect(fixture.nativeElement.querySelectorAll('app-content-card').length).toBe(1);
  });

  it('indica cuando el usuario aún no ha abierto artículos', async () => {
    TestBed.tick();
    httpMock.expectOne('/api/v1/me/learning').flush({ ultimo: null, completados: 0, enProgreso: 0 });
    httpMock.expectOne('/api/v1/content?page=0&size=' + CATALOG_PAGE_SIZE).flush(page([]));
    await fixture.whenStable();

    expect(text()).toContain('Aún no has abierto ningún artículo');
  });

  it('busca con el término de la URL y muestra el estado sin resultados', async () => {
    fixture.componentRef.setInput('q', 'cardiología');
    TestBed.tick();

    const request = httpMock.expectOne((req) => req.url === '/api/v1/content');
    expect(request.request.params.get('q')).toBe('cardiología');
    request.flush(page([]));
    httpMock.expectOne('/api/v1/me/learning').flush({ ultimo: null, completados: 0, enProgreso: 0 });
    await fixture.whenStable();

    expect(text()).toContain('Resultados para “cardiología”');
    expect(text()).toContain('Sin resultados');
    expect(text()).not.toContain('Continuar aprendizaje');
  });

  it('ofrece reintentar si falla la carga del catálogo', async () => {
    TestBed.tick();
    httpMock.expectOne('/api/v1/me/learning').flush({ ultimo: null, completados: 0, enProgreso: 0 });
    httpMock
      .expectOne((req) => req.url === '/api/v1/content')
      .flush(null, { status: 0, statusText: 'Unknown Error' });
    await fixture.whenStable();

    expect(text()).toContain('No pudimos cargar la biblioteca');
    expect(text()).toContain('No pudimos conectar con el servidor');
  });
});
