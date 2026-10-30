import { Component, computed, inject, input, model, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AdminApi } from '../../../../core/admin/admin-api.service';
import { Medio } from '../../../../core/admin/admin.models';
import { toApiError } from '../../../../core/api-error';
import { AuthService } from '../../../../core/auth/auth.service';

export type MediaKind = 'imagen' | 'pdf';

const ESTADO: Record<Medio['estado'], string> = {
  PENDIENTE: 'Pendiente de aprobación',
  APROBADO: 'Aprobado',
  RECHAZADO: 'Rechazado',
  ARCHIVADO: 'Archivado',
};

/**
 * Elige un medio ya cargado o sube uno nuevo con sus metadatos (texto alternativo, crédito, licencia y declaración de
 * derechos). Sólo un medio aprobado se muestra al público: aquí se puede aprobar si el rol lo permite.
 */
@Component({
  selector: 'app-media-picker',
  imports: [
    FormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
  ],
  template: `
    <div class="picker">
      @if (selected(); as m) {
        <div class="current">
          @if (kind() === 'imagen') {
            <img [src]="fileUrl(m.id)" [alt]="m.textoAlternativo ?? ''" />
          } @else {
            <mat-icon class="pdf" aria-hidden="true">picture_as_pdf</mat-icon>
          }
          <div class="current-info">
            <strong>{{ m.nombreOriginal }}</strong>
            <span class="state" [attr.data-estado]="m.estado">{{ estado[m.estado] }}</span>
            @if (m.estado === 'PENDIENTE' && m.acciones.aprobar) {
              <button matButton="tonal" type="button" (click)="approve(m)">Aprobar para publicación</button>
            }
          </div>
        </div>
      } @else if (value()) {
        <p class="hint">Archivo seleccionado: {{ value() }}</p>
      }

      <div class="actions">
        <button matButton="outlined" type="button" (click)="showLibrary.set(!showLibrary())">
          <mat-icon>photo_library</mat-icon>Biblioteca
        </button>
        <button matButton="outlined" type="button" (click)="showUpload.set(!showUpload())">
          <mat-icon>upload</mat-icon>Subir archivo
        </button>
      </div>

      @if (showLibrary()) {
        @if (library.isLoading()) {
          <mat-progress-bar mode="indeterminate" />
        } @else if (library.error()) {
          <p class="error" role="alert">No pudimos cargar la biblioteca. Puedes subir un archivo nuevo.</p>
        }
        <ul class="grid" role="listbox" [attr.aria-label]="'Archivos disponibles'">
          @for (m of options(); track m.id) {
            <li>
              <button type="button" class="tile" [class.on]="m.id === value()" (click)="choose(m)">
                @if (kind() === 'imagen') {
                  <img [src]="fileUrl(m.id)" [alt]="m.textoAlternativo ?? ''" loading="lazy" />
                } @else {
                  <mat-icon aria-hidden="true">picture_as_pdf</mat-icon>
                }
                <span class="name">{{ m.nombreOriginal }}</span>
                <span class="state" [attr.data-estado]="m.estado">{{ estado[m.estado] }}</span>
              </button>
            </li>
          } @empty {
            <li class="hint">Todavía no hay archivos de este tipo. Sube el primero.</li>
          }
        </ul>
      }

      @if (showUpload()) {
        <fieldset class="upload" [disabled]="uploading()">
          <legend>Nuevo archivo</legend>
          <input
            #file
            type="file"
            [accept]="kind() === 'imagen' ? 'image/jpeg,image/png,image/webp,image/svg+xml' : 'application/pdf'"
            (change)="picked = file.files?.[0] ?? null"
          />
          @if (kind() === 'imagen') {
            <mat-form-field subscriptSizing="dynamic">
              <mat-label>Texto alternativo *</mat-label>
              <input matInput [(ngModel)]="meta.textoAlternativo" maxlength="500" name="alt" />
              <mat-hint>Describe lo que muestra para quien usa lector de pantalla.</mat-hint>
            </mat-form-field>
            <mat-form-field subscriptSizing="dynamic">
              <mat-label>Pie de figura</mat-label>
              <input matInput [(ngModel)]="meta.pie" maxlength="500" name="pie" />
            </mat-form-field>
          }
          <mat-form-field subscriptSizing="dynamic">
            <mat-label>Crédito / fuente</mat-label>
            <input matInput [(ngModel)]="meta.credito" maxlength="300" name="credito" />
          </mat-form-field>
          <mat-form-field subscriptSizing="dynamic">
            <mat-label>Licencia o autorización de uso</mat-label>
            <input matInput [(ngModel)]="meta.licencia" maxlength="200" name="licencia" />
          </mat-form-field>
          <mat-checkbox [(ngModel)]="meta.derechosConfirmados" name="derechos">
            Confirmo que tengo derechos para publicar este archivo
          </mat-checkbox>
          <mat-checkbox [(ngModel)]="meta.sinDatosPaciente" name="paciente">
            No contiene datos identificables de pacientes
          </mat-checkbox>
          @if (error()) {
            <p class="error" role="alert">{{ error() }}</p>
          }
          <button matButton="filled" type="button" [disabled]="!picked || uploading()" (click)="upload()">
            Subir y seleccionar
          </button>
        </fieldset>
      }
    </div>
  `,
  styles: `
    .picker {
      display: grid;
      gap: 12px;
    }
    .current {
      display: flex;
      gap: 12px;
      align-items: center;
      padding: 12px;
      border: 1px solid var(--mat-sys-outline-variant);
      border-radius: 12px;
    }
    .current img {
      width: 96px;
      height: 72px;
      object-fit: contain;
      background: var(--mat-sys-surface-container);
      border-radius: 8px;
    }
    .current-info {
      display: grid;
      gap: 4px;
      justify-items: start;
    }
    .pdf {
      width: 48px;
      height: 48px;
      font-size: 48px;
      color: var(--mat-sys-error);
    }
    .actions {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }
    .grid {
      list-style: none;
      margin: 0;
      padding: 0;
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
      gap: 8px;
      max-height: 280px;
      overflow: auto;
    }
    .tile {
      width: 100%;
      display: grid;
      gap: 4px;
      justify-items: center;
      padding: 8px;
      border: 1px solid var(--mat-sys-outline-variant);
      border-radius: 12px;
      background: transparent;
      color: inherit;
      cursor: pointer;
      font: inherit;
    }
    .tile.on {
      border-color: var(--mat-sys-primary);
      background: var(--mat-sys-primary-container);
    }
    .tile img {
      width: 100%;
      height: 80px;
      object-fit: contain;
    }
    .name {
      font: var(--mat-sys-label-small);
      overflow-wrap: anywhere;
    }
    .state {
      font: var(--mat-sys-label-small);
      padding: 2px 8px;
      border-radius: 999px;
      background: var(--mat-sys-surface-container-high);
    }
    .state[data-estado='APROBADO'] {
      background: var(--mat-sys-tertiary-container);
    }
    .state[data-estado='RECHAZADO'] {
      background: var(--mat-sys-error-container);
    }
    .upload {
      display: grid;
      gap: 12px;
      border: 1px dashed var(--mat-sys-outline);
      border-radius: 12px;
      padding: 12px;
    }
    .hint {
      color: var(--mat-sys-on-surface-variant);
      font: var(--mat-sys-body-small);
    }
    .error {
      color: var(--mat-sys-error);
      margin: 0;
    }
  `,
})
export class MediaPicker {
  private readonly api = inject(AdminApi);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly auth = inject(AuthService);

  readonly kind = input<MediaKind>('imagen');
  /** Identificador del medio elegido. */
  readonly value = model<string | null>(null);

  protected readonly estado = ESTADO;
  protected readonly showLibrary = signal(false);
  protected readonly showUpload = signal(false);
  protected readonly uploading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly uploaded = signal<Medio[]>([]);
  protected picked: File | null = null;
  protected meta = {
    textoAlternativo: '',
    pie: '',
    credito: '',
    licencia: '',
    derechosConfirmados: false,
    sinDatosPaciente: false,
  };

  protected readonly library = rxResource({ stream: () => this.api.media({}, 0, 50) });
  protected readonly options = computed(() => {
    const prefix = this.kind() === 'imagen' ? 'image/' : 'application/pdf';
    const loaded = this.library.hasValue() ? this.library.value().items : [];
    const known = [...loaded, ...this.uploaded()];
    const unique = new Map(known.map((m) => [m.id, m]));
    return [...unique.values()].filter((m) => m.tipoMime.startsWith(prefix) && m.estado !== 'ARCHIVADO');
  });
  protected readonly selected = computed(() => this.options().find((m) => m.id === this.value()) ?? null);

  protected fileUrl(id: string): string {
    return AdminApi.mediaFileUrl(id);
  }

  protected choose(medio: Medio): void {
    this.value.set(medio.id);
  }

  protected upload(): void {
    if (!this.picked) {
      return;
    }
    this.uploading.set(true);
    this.error.set(null);
    this.api
      .uploadMedia(this.picked, {
        textoAlternativo: this.meta.textoAlternativo.trim() || null,
        pie: this.meta.pie.trim() || null,
        credito: this.meta.credito.trim() || null,
        licencia: this.meta.licencia.trim() || null,
        derechosConfirmados: this.meta.derechosConfirmados,
        sinDatosPaciente: this.meta.sinDatosPaciente,
      })
      .subscribe({
        next: (medio) => {
          this.uploading.set(false);
          this.uploaded.update((list) => [medio, ...list]);
          this.value.set(medio.id);
          this.showUpload.set(false);
          this.picked = null;
        },
        error: (error: unknown) => {
          this.uploading.set(false);
          this.error.set(toApiError(error).message);
        },
      });
  }

  protected approve(medio: Medio): void {
    this.api.approveMedia(medio.id).subscribe({
      next: (updated) => {
        this.uploaded.update((list) => [updated, ...list.filter((m) => m.id !== updated.id)]);
        this.library.reload();
        this.snackBar.open('Archivo aprobado', undefined, { duration: 3000 });
      },
      error: (error: unknown) => this.snackBar.open(toApiError(error).message, 'Cerrar', { duration: 6000 }),
    });
  }
}
