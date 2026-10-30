import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { EditorialActions, EditorialDetail } from '../../../core/admin/admin.models';
import { User } from '../../../core/api.models';
import { AuthService } from '../../../core/auth/auth.service';
import { ContentEditorPage } from './content-editor-page';

const NONE: EditorialActions = {
  editar: false,
  enviarRevision: false,
  revisionEditorial: false,
  revisionClinica: false,
  publicar: false,
  archivar: false,
  restaurar: false,
  asignar: false,
  agregarBloques: false,
  reordenarBloques: false,
  cambiarTipo: false,
  cambiarAcceso: false,
  crearAlgoritmos: false,
  marcarRevisionRequerida: false,
};

function detail(acciones: Partial<EditorialActions>, estado: EditorialDetail['estado']): EditorialDetail {
  return {
    id: 'c1',
    tipo: 'ARTICULO',
    titulo: 'Asma: tratamiento escalonado',
    slug: 'asma-tratamiento-escalonado',
    areaMedica: 'Neumología',
    objetivoAprendizaje: null,
    resumen: 'Resumen.',
    puntosClave: ['Punto uno.', 'Punto dos.'],
    aplicacionClinica: null,
    metodoAprendizaje: 'LECTURA_GUIADA',
    politicaAcceso: 'POR_TIER',
    tierMinimo: { id: 1, codigo: 'BASICO', nombre: 'Básico', descripcion: null, ordenAcceso: 10 },
    fuenteTitulo: 'GINA',
    fuenteAutores: null,
    fuenteUrl: null,
    fuenteDoi: null,
    fechaPublicacionFuente: null,
    fechaRevision: '2026-10-29',
    tiempoLecturaMinutos: 6,
    revisionClinicaRequerida: false,
    revisionClinicaObligatoria: false,
    institucionEmisora: null,
    fuenteEdicion: null,
    poblacionObjetivo: null,
    contextoAtencion: null,
    alcanceClinico: null,
    vigenteHasta: null,
    proximaRevision: null,
    recomendaciones: [],
    procedencia: null,
    titularDerechos: null,
    licenciaUso: null,
    autorizacionPublicacion: false,
    autorizacionReferencia: null,
    estado,
    autor: { id: 'a1', nombre: 'Andrés Autor' },
    revisor: { id: 'r1', nombre: 'Ramón Revisor' },
    enviadoRevisionAt: null,
    aprobadoAt: null,
    publicadoAt: null,
    archivadoAt: null,
    createdAt: '2026-10-29T10:00:00Z',
    updatedAt: '2026-10-29T10:00:00Z',
    version: 3,
    revisionVencida: false,
    revisionVenceEl: '2028-10-29',
    bloques: [],
    revisiones: [],
    acciones: { ...NONE, ...acciones },
  };
}

describe('ContentEditorPage', () => {
  let fixture: ComponentFixture<ContentEditorPage>;
  let httpMock: HttpTestingController;

  async function open(permisos: string[], content: EditorialDetail): Promise<HTMLElement> {
    TestBed.configureTestingModule({
      imports: [ContentEditorPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    const auth = TestBed.inject(AuthService);
    (auth as unknown as { currentUser: { set(value: Partial<User>): void } }).currentUser.set({ permisos });
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ContentEditorPage);
    fixture.componentRef.setInput('id', 'c1');
    TestBed.tick();
    httpMock.expectOne('/api/v1/admin/contenido/c1').flush(content);
    httpMock.expectOne('/api/v1/admin/contenido/areas').flush(['Neumología']);
    httpMock.match('/api/v1/tiers').forEach((r) => r.flush([]));
    httpMock.match('/api/v1/admin/contenido/responsables').forEach((r) => r.flush({ autores: [], revisores: [] }));
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  function buttons(element: HTMLElement): string[] {
    return Array.from(element.querySelectorAll('.flow-actions button')).map((b) => b.textContent?.trim() ?? '');
  }

  afterEach(() => httpMock.verify());

  it('el revisor ve el contenido en sólo lectura con las acciones de revisión', async () => {
    const element = await open(['admin.access', 'content.read.admin', 'content.review'], detail({ revisionEditorial: true }, 'EN_REVISION_EDITORIAL'));

    const titulo = element.querySelector('input[formControlName="titulo"]') as HTMLInputElement;
    expect(titulo.disabled).toBe(true);
    expect(element.querySelector('.save-bar')).toBeNull();
    expect(buttons(element).some((b) => b.includes('Aprobar'))).toBe(true);
    expect(buttons(element).some((b) => b.includes('Solicitar cambios'))).toBe(true);
    expect(buttons(element).some((b) => b.includes('Publicar'))).toBe(false);
  });

  it('el autor edita su borrador y puede enviarlo a revisión', async () => {
    const element = await open(
      ['admin.access', 'content.read.admin', 'content.create', 'content.update.own'],
      detail({ editar: true, enviarRevision: true, archivar: true }, 'BORRADOR'),
    );

    const titulo = element.querySelector('input[formControlName="titulo"]') as HTMLInputElement;
    expect(titulo.disabled).toBe(false);
    expect(titulo.value).toBe('Asma: tratamiento escalonado');
    expect(element.querySelectorAll('.point textarea').length).toBe(2);
    expect(buttons(element).some((b) => b.includes('Enviar a revisión'))).toBe(true);
    expect(buttons(element).some((b) => b.includes('Archivar'))).toBe(true);
  });

  it('una guía muestra los campos clínicos y el editor de bloques', async () => {
    const guia: EditorialDetail = {
      ...detail(
        { editar: true, agregarBloques: true, reordenarBloques: true, crearAlgoritmos: true, cambiarAcceso: true },
        'BORRADOR',
      ),
      tipo: 'GUIA_PRACTICA_CLINICA',
      politicaAcceso: 'POR_TIER',
      bloques: [
        {
          id: 'b1',
          tipo: 'CALLOUT',
          posicion: 1,
          datos: { variante: 'PUNTO_CLAVE', titulo: null, texto: 'Colangitis = drenaje biliar.' },
          autor: 'Andrés Autor',
          updatedAt: '2026-10-29T10:00:00Z',
          medios: [],
          editable: true,
        },
      ],
    };
    const element = await open(['admin.access', 'content.read.admin', 'guides.update.any', 'blocks.create'], guia);

    expect(element.textContent).toContain('Institución emisora');
    expect(element.textContent).toContain('Recomendaciones');
    expect(element.textContent).toContain('Tier de la guía');
    expect(element.querySelectorAll('app-block-editor .item').length).toBe(1);
    expect(element.querySelector('app-block-editor')?.textContent).toContain('Colangitis = drenaje biliar.');
    expect(element.querySelectorAll('app-block-editor .palette .chip').length).toBe(9);
  });

  it('un artículo no muestra los campos de guía ni de recurso', async () => {
    const element = await open(['admin.access', 'content.read.admin'], detail({ editar: true }, 'BORRADOR'));

    expect(element.textContent).not.toContain('Institución emisora');
    expect(element.textContent).not.toContain('Procedencia y derechos');
    expect(element.textContent).toContain('Los artículos son gratis con cuenta');
  });
});
