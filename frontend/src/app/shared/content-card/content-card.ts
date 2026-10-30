import { Component, computed, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { ContentSummary } from '../../core/api.models';
import { TIPO_CONTENIDO, accesoLabel } from '../../core/content-labels';

/** Tarjeta de un contenido del catálogo. Toda la tarjeta es clicable a través del enlace del título. */
@Component({
  selector: 'app-content-card',
  imports: [MatCardModule, MatIconModule, RouterLink],
  templateUrl: './content-card.html',
  styleUrl: './content-card.scss',
})
export class ContentCard {
  readonly content = input.required<ContentSummary>();
  /** `/app/contenido` en la zona privada; `/contenido` en el catálogo público. */
  readonly basePath = input('/app/contenido');

  protected readonly tipo = computed(() => TIPO_CONTENIDO[this.content().tipo]);
  protected readonly acceso = computed(() => accesoLabel(this.content().acceso));
}
