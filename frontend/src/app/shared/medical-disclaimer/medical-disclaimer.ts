import { Component } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-medical-disclaimer',
  imports: [MatIconModule],
  template: `
    <aside class="disclaimer" aria-label="Aviso de uso">
      <mat-icon aria-hidden="true">health_and_safety</mat-icon>
      <p>
        <strong>Contenido educativo.</strong> No sustituye el juicio clínico profesional ni una
        evaluación, diagnóstico o tratamiento individualizado.
      </p>
    </aside>
  `,
  styles: `
    .disclaimer {
      display: flex;
      gap: 12px;
      align-items: flex-start;
      padding: 12px 16px;
      border-radius: var(--mat-sys-corner-medium);
      background: var(--mat-sys-tertiary-container);
      color: var(--mat-sys-on-tertiary-container);
      font: var(--mat-sys-body-medium);
    }
    mat-icon {
      flex: none;
    }
    p {
      margin: 0;
    }
  `,
})
export class MedicalDisclaimer {}
