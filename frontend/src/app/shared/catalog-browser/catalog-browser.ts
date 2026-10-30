import { Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Router, RouterLink } from '@angular/router';
import { TipoContenido } from '../../core/api.models';
import { toApiError } from '../../core/api-error';
import { TIPO_CONTENIDO } from '../../core/content-labels';
import { ContentService } from '../../core/content.service';
import { ContentCard } from '../content-card/content-card';
import { StateMessage } from '../state-message/state-message';

export const CATALOG_PAGE_SIZE = 9;

/**
 * Resultados del catálogo con filtro por tipo y paginación. Los filtros viven en la URL (`?q=&tipo=&page=`) para que
 * se puedan compartir y respeten el botón atrás.
 */
@Component({
  selector: 'app-catalog-browser',
  imports: [
    RouterLink,
    MatButtonModule,
    MatChipsModule,
    MatIconModule,
    MatPaginatorModule,
    MatProgressBarModule,
    ContentCard,
    StateMessage,
  ],
  templateUrl: './catalog-browser.html',
  styleUrl: './catalog-browser.scss',
})
export class CatalogBrowser {
  private readonly contentService = inject(ContentService);
  private readonly router = inject(Router);

  readonly query = input('');
  /** Página en base 0. */
  readonly page = input(0);
  readonly tipo = input<TipoContenido | null>(null);
  readonly basePath = input('/app/contenido');
  readonly titulo = input('Biblioteca');
  /** Ruta a la que vuelve «Limpiar búsqueda». */
  readonly clearPath = input('/app');

  protected readonly pageSize = CATALOG_PAGE_SIZE;
  protected readonly tipos = (Object.keys(TIPO_CONTENIDO) as TipoContenido[]).map((codigo) => ({
    codigo,
    ...TIPO_CONTENIDO[codigo],
  }));
  protected readonly searching = computed(() => this.query().trim().length > 0);

  protected readonly catalog = rxResource({
    params: () => ({ q: this.query().trim(), page: this.page(), tipo: this.tipo() }),
    stream: ({ params }) => this.contentService.search(params.q, params.page, CATALOG_PAGE_SIZE, params.tipo),
  });
  protected readonly error = computed(() => (this.catalog.error() ? toApiError(this.catalog.error()).message : null));

  protected selectTipo(tipo: TipoContenido | null): void {
    void this.router.navigate([], { queryParams: { tipo, page: null }, queryParamsHandling: 'merge' });
  }

  protected changePage(event: PageEvent): void {
    void this.router.navigate([], {
      queryParams: { page: event.pageIndex > 0 ? event.pageIndex + 1 : null },
      queryParamsHandling: 'merge',
    });
  }
}
