import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, linkedSignal, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { toApiError } from '../../core/api-error';
import { PublicContent } from '../../core/api.models';
import { AuthService } from '../../core/auth/auth.service';
import { APP_NAME } from '../../core/brand';
import { TIPO_CONTENIDO, accesoLabel } from '../../core/content-labels';
import { ContentService } from '../../core/content.service';
import { BlockRenderer } from '../../shared/blocks/block-renderer';
import { MedicalDisclaimer } from '../../shared/medical-disclaimer/medical-disclaimer';
import { StateMessage } from '../../shared/state-message/state-message';

interface Section {
  id: string;
  title: string;
  icon: string;
}

type LoadProblem = 'login' | 'locked' | 'not-found' | 'error';

/**
 * Lectura de la versión publicada de un artículo, una GPC o un recurso de estudio. Se usa en la ruta pública
 * (`/contenido/:slug`) y en la zona privada (`/app/contenido/:slug`).
 */
@Component({
  selector: 'app-content-detail-page',
  imports: [
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    BlockRenderer,
    MedicalDisclaimer,
    StateMessage,
  ],
  templateUrl: './content-detail-page.html',
  styleUrl: './content-detail-page.scss',
  host: { '(window:scroll)': 'updateReadingProgress()' },
})
export class ContentDetailPage {
  private readonly contentService = inject(ContentService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);

  /** Parámetro de ruta `:slug`, enlazado por el router. */
  readonly slug = input.required<string>();

  protected readonly appName = APP_NAME;
  protected readonly tipos = TIPO_CONTENIDO;
  /** Todo el catálogo vive en la zona privada: se vuelve al panel. */
  protected readonly volver = '/app';

  protected readonly reading = rxResource({
    params: () => this.slug(),
    stream: ({ params }) => this.contentService.open(params),
  });
  protected readonly content = computed(() => this.reading.value()?.contenido ?? null);
  protected readonly acceso = computed(() => {
    const value = this.reading.value();
    return value ? accesoLabel(value.acceso) : null;
  });

  /** Progreso mostrado; se actualiza localmente al marcar como completado. */
  protected readonly progress = linkedSignal(() => this.reading.value()?.progreso ?? null);
  protected readonly completed = computed(() => this.progress()?.estado === 'COMPLETADO');
  protected readonly completing = signal(false);
  protected readonly readingProgress = signal(0);

  protected readonly problem = computed<{ kind: LoadProblem; message: string } | null>(() => {
    const error = this.reading.error();
    if (!error) {
      return null;
    }
    const apiError = toApiError(error);
    if (apiError.code === 'REQUIERE_REGISTRO') {
      return { kind: 'login', message: apiError.message };
    }
    if (apiError.code === 'TIER_INSUFICIENTE') {
      return { kind: 'locked', message: apiError.message };
    }
    if (apiError.status === 404) {
      return { kind: 'not-found', message: apiError.message };
    }
    return { kind: 'error', message: apiError.message };
  });

  protected readonly sections = computed(() => {
    const content = this.content();
    return content ? buildSections(content) : [];
  });

  protected returnUrl(): string {
    return this.router.url;
  }

  protected markCompleted(contenidoId: string): void {
    if (this.completing()) {
      return;
    }
    this.completing.set(true);
    this.contentService.complete(contenidoId).subscribe({
      next: (progress) => {
        this.progress.set(progress);
        this.completing.set(false);
        this.snackBar.open('¡Completado!', undefined, { duration: 3000 });
      },
      error: (error: unknown) => {
        this.completing.set(false);
        this.snackBar.open(toApiError(error).message, 'Cerrar', { duration: 5000 });
      },
    });
  }

  protected updateReadingProgress(): void {
    const element = document.documentElement;
    const scrollable = element.scrollHeight - element.clientHeight;
    this.readingProgress.set(scrollable > 0 ? Math.min(100, (element.scrollTop / scrollable) * 100) : 100);
  }
}

function buildSections(c: PublicContent): Section[] {
  const sections: Section[] = [];
  const add = (cond: unknown, id: string, title: string, icon: string) => {
    if (cond) {
      sections.push({ id, title, icon });
    }
  };
  add(c.objetivoAprendizaje, 'objetivo', 'Objetivo de aprendizaje', 'flag');
  add(c.resumen, 'resumen', c.tipo === 'GUIA_PRACTICA_CLINICA' ? 'Resumen ejecutivo' : 'Resumen', 'subject');
  add(c.guia, 'alcance', 'Población y alcance', 'groups');
  add(c.guia?.recomendaciones.length, 'recomendaciones', 'Recomendaciones', 'fact_check');
  add(c.puntosClave.length, 'puntos-clave', 'Puntos clave', 'checklist');
  add(c.aplicacionClinica, 'aplicacion-clinica', 'Aplicación clínica', 'clinical_notes');
  add(c.bloques.length, 'desarrollo', c.tipo === 'ARTICULO' ? 'Material complementario' : 'Desarrollo', 'auto_stories');
  add(true, 'fuente', c.tipo === 'RECURSO_ESTUDIO' ? 'Procedencia y fuente' : 'Fuente y revisión', 'menu_book');
  return sections;
}
