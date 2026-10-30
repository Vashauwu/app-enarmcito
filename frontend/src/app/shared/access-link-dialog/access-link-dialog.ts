import { Clipboard } from '@angular/cdk/clipboard';
import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { AccessLink } from '../../core/admin/admin.models';

export interface AccessLinkDialogData {
  enlace: AccessLink;
  nombre: string;
}

/** Muestra una única vez el enlace de invitación o restablecimiento para copiarlo. */
@Component({
  selector: 'app-access-link-dialog',
  imports: [DatePipe, MatButtonModule, MatDialogModule, MatIconModule],
  template: `
    <h2 mat-dialog-title>
      {{ data.enlace.tipo === 'INVITACION' ? 'Invitación creada' : 'Enlace de acceso generado' }}
    </h2>
    <mat-dialog-content>
      <p>
        Envía este enlace a <strong>{{ data.nombre }}</strong> por un canal de confianza. Con él definirá su
        contraseña; nadie más la conocerá.
      </p>
      <div class="link" aria-label="Enlace de acceso">{{ data.enlace.url }}</div>
      <p class="warning">
        <mat-icon aria-hidden="true">warning</mat-icon>
        <span>
          Sólo se muestra ahora. Caduca el {{ data.enlace.expiraAt | date: 'd MMM y, HH:mm' }} y deja de servir
          tras usarse o si generas otro.
        </span>
      </p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button matButton="outlined" type="button" (click)="copy()">
        <mat-icon>{{ copied() ? 'check' : 'content_copy' }}</mat-icon>
        {{ copied() ? 'Copiado' : 'Copiar enlace' }}
      </button>
      <button matButton="filled" mat-dialog-close type="button">Listo</button>
    </mat-dialog-actions>
  `,
  styles: `
    .link {
      padding: 12px;
      border-radius: var(--mat-sys-corner-small);
      background: var(--mat-sys-surface-container-high);
      font-family: ui-monospace, 'Cascadia Code', Consolas, monospace;
      font-size: 0.85rem;
      overflow-wrap: anywhere;
      user-select: all;
    }
    .warning {
      display: flex;
      gap: 8px;
      align-items: flex-start;
      margin: 16px 0 0;
      color: var(--mat-sys-on-surface-variant);
      font: var(--mat-sys-body-small);
    }
  `,
})
export class AccessLinkDialog {
  protected readonly data = inject<AccessLinkDialogData>(MAT_DIALOG_DATA);
  private readonly clipboard = inject(Clipboard);
  protected readonly copied = signal(false);

  protected copy(): void {
    this.copied.set(this.clipboard.copy(this.data.enlace.url));
  }
}
