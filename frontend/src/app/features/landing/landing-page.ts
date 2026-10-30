import { Component, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { APP_NAME } from '../../core/brand';
import { ContentService } from '../../core/content.service';
import { MedicalDisclaimer } from '../../shared/medical-disclaimer/medical-disclaimer';
import { ThemeToggle } from '../../shared/theme-toggle/theme-toggle';

interface GuidedStep {
  icon: string;
  title: string;
  text: string;
}

@Component({
  selector: 'app-landing-page',
  imports: [MatButtonModule, MatCardModule, MatIconModule, RouterLink, MedicalDisclaimer, ThemeToggle],
  templateUrl: './landing-page.html',
  styleUrl: './landing-page.scss',
})
export class LandingPage {
  protected readonly appName = APP_NAME;
  protected readonly year = new Date().getFullYear();
  protected readonly auth = inject(AuthService);
  private readonly contentService = inject(ContentService);

  /** Catálogo público de tiers; si falla, la sección simplemente no se muestra. */
  protected readonly tiers = rxResource({ stream: () => this.contentService.tiers() });

  protected readonly steps: GuidedStep[] = [
    {
      icon: 'flag',
      title: 'Objetivo',
      text: 'Cada artículo empieza diciendo qué vas a poder hacer al terminarlo.',
    },
    {
      icon: 'subject',
      title: 'Resumen',
      text: 'La idea central del tema en pocos párrafos, sin relleno.',
    },
    {
      icon: 'checklist',
      title: 'Puntos clave',
      text: 'Los datos que conviene recordar, listos para repasar.',
    },
    {
      icon: 'verified',
      title: 'Fuente y revisión',
      text: 'Referencia bibliográfica, DOI cuando existe y fecha de la última revisión.',
    },
  ];
}
