import { DatePipe } from '@angular/common';
import { Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { TipoContenido } from '../../core/api.models';
import { AuthService } from '../../core/auth/auth.service';
import { TIPO_CONTENIDO } from '../../core/content-labels';
import { ContentService } from '../../core/content.service';
import { CatalogBrowser } from '../../shared/catalog-browser/catalog-browser';
import { StateMessage } from '../../shared/state-message/state-message';

@Component({
  selector: 'app-dashboard-page',
  imports: [
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressBarModule,
    CatalogBrowser,
    StateMessage,
  ],
  templateUrl: './dashboard-page.html',
  styleUrl: './dashboard-page.scss',
})
export class DashboardPage {
  private readonly contentService = inject(ContentService);

  /** Query params enlazados por el router: `?q=texto&tipo=…&page=2` (página en base 1). */
  readonly q = input<string>();
  readonly page = input<string>();
  readonly tipo = input<string>();

  protected readonly user = inject(AuthService).user;
  protected readonly tipoLabel = TIPO_CONTENIDO;
  protected readonly query = computed(() => (this.q() ?? '').trim());
  protected readonly searching = computed(() => this.query().length > 0);
  protected readonly pageIndex = computed(() => Math.max(0, (Number.parseInt(this.page() ?? '1', 10) || 1) - 1));
  protected readonly tipoFiltro = computed(() => {
    const tipo = this.tipo();
    return tipo && tipo in TIPO_CONTENIDO ? (tipo as TipoContenido) : null;
  });

  protected readonly learning = rxResource({ stream: () => this.contentService.learning() });
}
