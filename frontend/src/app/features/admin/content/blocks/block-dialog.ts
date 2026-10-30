import { SlicePipe } from '@angular/common';
import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Observable } from 'rxjs';
import { EditorialDetail } from '../../../../core/admin/admin.models';
import { toApiError } from '../../../../core/api-error';
import { BlockData, TipoBloque } from '../../../../core/api.models';
import { BLOCK_LABEL, toHtml } from './block-types';
import { MediaPicker } from './media-picker';

export interface BlockDialogData {
  tipo: TipoBloque;
  datos?: BlockData[TipoBloque];
  /** Guarda el bloque y devuelve el contenido actualizado; el diálogo muestra el error si falla. */
  save: (datos: BlockData[TipoBloque]) => Observable<EditorialDetail>;
}

type Callout = BlockData['CALLOUT'];
type Algoritmo = BlockData['ALGORITMO'];
type Referencia = BlockData['REFERENCIAS']['referencias'][number];

const VARIANTES: { value: Callout['variante']; label: string }[] = [
  { value: 'PUNTO_CLAVE', label: 'Punto clave' },
  { value: 'PERLA_CLINICA', label: 'Perla clínica' },
  { value: 'INFORMACION', label: 'Información' },
  { value: 'ALERTA', label: 'Alerta' },
  { value: 'CONTRAINDICACION', label: 'Contraindicación' },
];

const NODOS: { value: Algoritmo['nodos'][number]['tipo']; label: string }[] = [
  { value: 'INICIO', label: 'Inicio' },
  { value: 'DECISION', label: 'Decisión' },
  { value: 'ACCION', label: 'Acción' },
  { value: 'FIN', label: 'Fin' },
];

/** Formulario de un bloque; cada tipo expone sólo los campos que su contrato admite. */
@Component({
  selector: 'app-block-dialog',
  imports: [
    SlicePipe,
    FormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MediaPicker,
  ],
  styleUrl: './block-dialog.scss',
  template: `
    <h2 mat-dialog-title>
      <mat-icon aria-hidden="true">{{ info.icon }}</mat-icon>
      {{ editing ? 'Editar' : 'Agregar' }} · {{ info.label }}
    </h2>
    <mat-dialog-content class="content">
      @switch (data.tipo) {
        @case ('TEXTO_RICO') {
          <div class="toolbar" role="toolbar" aria-label="Formato">
            <button matIconButton type="button" aria-label="Negrita" (click)="wrap('<strong>', '</strong>')">
              <mat-icon>format_bold</mat-icon>
            </button>
            <button matIconButton type="button" aria-label="Cursiva" (click)="wrap('<em>', '</em>')">
              <mat-icon>format_italic</mat-icon>
            </button>
            <button matIconButton type="button" aria-label="Subtítulo" (click)="wrap('<h3>', '</h3>')">
              <mat-icon>title</mat-icon>
            </button>
            <button matIconButton type="button" aria-label="Lista" (click)="wrap('<ul>\\n<li>', '</li>\\n</ul>')">
              <mat-icon>format_list_bulleted</mat-icon>
            </button>
          </div>
          <mat-form-field>
            <mat-label>Texto</mat-label>
            <textarea #area matInput rows="10" [(ngModel)]="texto" name="html" maxlength="20000"></textarea>
            <mat-hint>Separa párrafos con una línea en blanco. Se admite negrita, cursiva, subtítulos y listas.</mat-hint>
          </mat-form-field>
        }

        @case ('CALLOUT') {
          <mat-form-field>
            <mat-label>Tipo de llamada</mat-label>
            <mat-select [(ngModel)]="callout.variante" name="variante">
              @for (v of variantes; track v.value) {
                <mat-option [value]="v.value">{{ v.label }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Título (opcional)</mat-label>
            <input matInput [(ngModel)]="callout.titulo" name="titulo" maxlength="200" />
          </mat-form-field>
          <mat-form-field>
            <mat-label>Texto</mat-label>
            <textarea matInput rows="5" [(ngModel)]="callout.texto" name="texto" maxlength="2000"></textarea>
          </mat-form-field>
        }

        @case ('TABLA') {
          <mat-form-field>
            <mat-label>Título (opcional)</mat-label>
            <input matInput [(ngModel)]="tabla.titulo" name="ttitulo" maxlength="200" />
          </mat-form-field>

          <details class="paste">
            <summary>Pegar desde una hoja de cálculo o PDF</summary>
            <p class="hint">
              Pega las celdas copiadas (separadas por tabulaciones). La primera línea será el encabezado.
            </p>
            <textarea rows="5" [(ngModel)]="pegado" name="pegado" aria-label="Datos pegados"></textarea>
            <button matButton="tonal" type="button" (click)="importTable()">Importar</button>
          </details>

          <div class="table-scroll">
            <table class="grid-edit">
              <thead>
                <tr>
                  @for (h of tabla.encabezados; track $index; let c = $index) {
                    <th>
                      <input
                        [ngModel]="h"
                        (ngModelChange)="tabla.encabezados[c] = $event"
                        [name]="'h' + c"
                        [attr.aria-label]="'Encabezado ' + (c + 1)"
                        maxlength="200"
                      />
                    </th>
                  }
                  <th class="tools">
                    <button matIconButton type="button" aria-label="Agregar columna" (click)="addColumn()" [disabled]="tabla.encabezados.length >= 8">
                      <mat-icon>add</mat-icon>
                    </button>
                  </th>
                </tr>
              </thead>
              <tbody>
                @for (fila of tabla.filas; track $index; let r = $index) {
                  <tr>
                    @for (celda of fila; track $index; let c = $index) {
                      <td>
                        <textarea
                          rows="2"
                          [ngModel]="celda"
                          (ngModelChange)="fila[c] = $event"
                          [name]="'c' + r + '_' + c"
                          [attr.aria-label]="'Fila ' + (r + 1) + ', columna ' + (c + 1)"
                          maxlength="600"
                        ></textarea>
                      </td>
                    }
                    <td class="tools">
                      <button matIconButton type="button" aria-label="Quitar fila" (click)="tabla.filas.splice(r, 1)">
                        <mat-icon>delete</mat-icon>
                      </button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <div class="row-actions">
            <button matButton="tonal" type="button" (click)="addRow()" [disabled]="tabla.filas.length >= 60">
              <mat-icon>add</mat-icon>Fila
            </button>
            @if (tabla.encabezados.length > 1) {
              <button matButton type="button" (click)="removeColumn()">Quitar última columna</button>
            }
          </div>
          <mat-form-field>
            <mat-label>Fuente (opcional)</mat-label>
            <input matInput [(ngModel)]="tabla.fuente" name="tfuente" maxlength="300" />
          </mat-form-field>
          <mat-form-field>
            <mat-label>Nota (opcional)</mat-label>
            <input matInput [(ngModel)]="tabla.nota" name="tnota" maxlength="500" />
          </mat-form-field>
        }

        @case ('ALGORITMO') {
          <mat-form-field>
            <mat-label>Título</mat-label>
            <input matInput [(ngModel)]="algoritmo.titulo" name="atitulo" maxlength="200" />
          </mat-form-field>
          <div class="two">
            <mat-form-field>
              <mat-label>Versión</mat-label>
              <input matInput [(ngModel)]="algoritmo.version" name="aversion" maxlength="50" />
            </mat-form-field>
            <mat-form-field>
              <mat-label>Fuente</mat-label>
              <input matInput [(ngModel)]="algoritmo.fuente" name="afuente" maxlength="300" />
            </mat-form-field>
          </div>
          <mat-form-field>
            <mat-label>Descripción en texto</mat-label>
            <textarea matInput rows="4" [(ngModel)]="algoritmo.descripcionTextual" name="adesc" maxlength="6000"></textarea>
            <mat-hint>Equivalente para lectores de pantalla.</mat-hint>
          </mat-form-field>

          <h3 class="sub">Pasos</h3>
          @for (nodo of algoritmo.nodos; track nodo.id; let i = $index) {
            <div class="node">
              <span class="node-id">{{ nodo.id }}</span>
              <mat-form-field subscriptSizing="dynamic">
                <mat-label>Tipo</mat-label>
                <mat-select [(ngModel)]="nodo.tipo" [name]="'nt' + i">
                  @for (n of nodos; track n.value) {
                    <mat-option [value]="n.value">{{ n.label }}</mat-option>
                  }
                </mat-select>
              </mat-form-field>
              <mat-form-field subscriptSizing="dynamic" class="grow">
                <mat-label>Texto del paso</mat-label>
                <input matInput [(ngModel)]="nodo.texto" [name]="'nx' + i" maxlength="300" />
              </mat-form-field>
              <button matIconButton type="button" aria-label="Quitar paso" (click)="removeNode(i)">
                <mat-icon>delete</mat-icon>
              </button>
            </div>
          }
          <button matButton="tonal" type="button" (click)="addNode()" [disabled]="algoritmo.nodos.length >= 40">
            <mat-icon>add</mat-icon>Paso
          </button>

          <h3 class="sub">Conexiones</h3>
          @for (con of algoritmo.conexiones; track $index; let i = $index) {
            <div class="node">
              <mat-form-field subscriptSizing="dynamic">
                <mat-label>Desde</mat-label>
                <mat-select [(ngModel)]="con.desde" [name]="'cd' + i">
                  @for (n of algoritmo.nodos; track n.id) {
                    <mat-option [value]="n.id">{{ n.id }} · {{ n.texto | slice: 0 : 24 }}</mat-option>
                  }
                </mat-select>
              </mat-form-field>
              <mat-form-field subscriptSizing="dynamic">
                <mat-label>Hacia</mat-label>
                <mat-select [(ngModel)]="con.hacia" [name]="'ch' + i">
                  @for (n of algoritmo.nodos; track n.id) {
                    <mat-option [value]="n.id">{{ n.id }} · {{ n.texto | slice: 0 : 24 }}</mat-option>
                  }
                </mat-select>
              </mat-form-field>
              <mat-form-field subscriptSizing="dynamic" class="grow">
                <mat-label>Etiqueta (Sí / No…)</mat-label>
                <input matInput [(ngModel)]="con.etiqueta" [name]="'ce' + i" maxlength="60" />
              </mat-form-field>
              <button matIconButton type="button" aria-label="Quitar conexión" (click)="algoritmo.conexiones.splice(i, 1)">
                <mat-icon>delete</mat-icon>
              </button>
            </div>
          }
          <button
            matButton="tonal"
            type="button"
            (click)="addConnection()"
            [disabled]="algoritmo.nodos.length < 2 || algoritmo.conexiones.length >= 80"
          >
            <mat-icon>add</mat-icon>Conexión
          </button>
        }

        @case ('IMAGEN') {
          <app-media-picker kind="imagen" [(value)]="activoId" />
          <mat-form-field>
            <mat-label>Pie de figura (opcional)</mat-label>
            <input matInput [(ngModel)]="pie" name="pie" maxlength="500" />
          </mat-form-field>
        }

        @case ('DIAGRAMA') {
          <app-media-picker kind="imagen" [(value)]="activoId" />
          <mat-form-field>
            <mat-label>Título</mat-label>
            <input matInput [(ngModel)]="titulo" name="dtitulo" maxlength="200" />
          </mat-form-field>
          <mat-form-field>
            <mat-label>Descripción en texto</mat-label>
            <textarea matInput rows="4" [(ngModel)]="descripcionTextual" name="ddesc" maxlength="4000"></textarea>
            <mat-hint>Obligatoria: describe el diagrama para lectores de pantalla.</mat-hint>
          </mat-form-field>
        }

        @case ('ACORDEON') {
          @for (s of secciones; track $index; let i = $index) {
            <div class="panel">
              <mat-form-field subscriptSizing="dynamic">
                <mat-label>Encabezado</mat-label>
                <input matInput [(ngModel)]="s.titulo" [name]="'st' + i" maxlength="200" />
              </mat-form-field>
              <mat-form-field subscriptSizing="dynamic">
                <mat-label>Contenido</mat-label>
                <textarea matInput rows="4" [(ngModel)]="s.html" [name]="'sh' + i" maxlength="10000"></textarea>
              </mat-form-field>
              <button matButton type="button" (click)="secciones.splice(i, 1)">
                <mat-icon>delete</mat-icon>Quitar sección
              </button>
            </div>
          }
          <button matButton="tonal" type="button" (click)="secciones.push({ titulo: '', html: '' })" [disabled]="secciones.length >= 20">
            <mat-icon>add</mat-icon>Sección
          </button>
        }

        @case ('REFERENCIAS') {
          @for (r of referencias; track $index; let i = $index) {
            <div class="panel">
              <mat-form-field subscriptSizing="dynamic">
                <mat-label>Título *</mat-label>
                <input matInput [(ngModel)]="r.titulo" [name]="'rt' + i" maxlength="300" />
              </mat-form-field>
              <div class="two">
                <mat-form-field subscriptSizing="dynamic">
                  <mat-label>Autores</mat-label>
                  <input matInput [(ngModel)]="r.autores" [name]="'ra' + i" maxlength="500" />
                </mat-form-field>
                <mat-form-field subscriptSizing="dynamic">
                  <mat-label>Emisor</mat-label>
                  <input matInput [(ngModel)]="r.emisor" [name]="'re' + i" maxlength="200" />
                </mat-form-field>
              </div>
              <div class="two">
                <mat-form-field subscriptSizing="dynamic">
                  <mat-label>URL</mat-label>
                  <input matInput [(ngModel)]="r.url" [name]="'ru' + i" maxlength="2000" />
                </mat-form-field>
                <mat-form-field subscriptSizing="dynamic">
                  <mat-label>DOI</mat-label>
                  <input matInput [(ngModel)]="r.doi" [name]="'rd' + i" maxlength="150" />
                </mat-form-field>
              </div>
              <div class="two">
                <mat-form-field subscriptSizing="dynamic">
                  <mat-label>Fecha</mat-label>
                  <input matInput [(ngModel)]="r.fecha" [name]="'rf' + i" maxlength="30" />
                </mat-form-field>
                <mat-form-field subscriptSizing="dynamic">
                  <mat-label>Nivel de evidencia</mat-label>
                  <input matInput [(ngModel)]="r.etiquetaEvidencia" [name]="'rv' + i" maxlength="100" />
                </mat-form-field>
              </div>
              <button matButton type="button" (click)="referencias.splice(i, 1)">
                <mat-icon>delete</mat-icon>Quitar referencia
              </button>
            </div>
          }
          <button matButton="tonal" type="button" (click)="addReference()" [disabled]="referencias.length >= 50">
            <mat-icon>add</mat-icon>Referencia
          </button>
        }

        @case ('ADJUNTO') {
          <app-media-picker kind="pdf" [(value)]="activoId" />
          <mat-form-field>
            <mat-label>Título</mat-label>
            <input matInput [(ngModel)]="titulo" name="adtitulo" maxlength="200" />
          </mat-form-field>
          <mat-form-field>
            <mat-label>Descripción (opcional)</mat-label>
            <input matInput [(ngModel)]="descripcion" name="addesc" maxlength="500" />
          </mat-form-field>
        }
      }

      @if (error()) {
        <p class="error" role="alert"><mat-icon aria-hidden="true">error</mat-icon>{{ error() }}</p>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button matButton type="button" mat-dialog-close [disabled]="saving()">Cancelar</button>
      <button matButton="filled" type="button" [disabled]="saving()" (click)="submit()">
        {{ saving() ? 'Guardando…' : 'Guardar bloque' }}
      </button>
    </mat-dialog-actions>
  `,
})
export class BlockDialog {
  protected readonly data = inject<BlockDialogData>(MAT_DIALOG_DATA);
  private readonly ref = inject<MatDialogRef<BlockDialog, EditorialDetail>>(MatDialogRef);
  private readonly area = viewChild<ElementRef<HTMLTextAreaElement>>('area');

  protected readonly info = BLOCK_LABEL[this.data.tipo];
  protected readonly editing = !!this.data.datos;
  protected readonly variantes = VARIANTES;
  protected readonly nodos = NODOS;
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  // Borradores por tipo (se inicializan con los datos existentes al editar).
  protected texto = '';
  protected callout: Callout = { variante: 'PUNTO_CLAVE', titulo: '', texto: '' };
  protected tabla: BlockData['TABLA'] = {
    titulo: '',
    encabezados: ['', ''],
    filas: [['', '']],
    fuente: '',
    nota: '',
  };
  protected pegado = '';
  protected algoritmo: Algoritmo = {
    titulo: '',
    version: '1.0',
    fuente: '',
    descripcionTextual: '',
    nodos: [
      { id: 'n1', tipo: 'INICIO', texto: '' },
      { id: 'n2', tipo: 'FIN', texto: '' },
    ],
    conexiones: [{ desde: 'n1', hacia: 'n2', etiqueta: '' }],
  };
  protected activoId: string | null = null;
  protected pie = '';
  protected titulo = '';
  protected descripcionTextual = '';
  protected descripcion = '';
  protected secciones: { titulo: string; html: string }[] = [{ titulo: '', html: '' }];
  protected referencias: Referencia[] = [this.emptyReference()];

  constructor() {
    const d = structuredClone(this.data.datos) as never as Record<string, never> | undefined;
    if (!d) {
      return;
    }
    switch (this.data.tipo) {
      case 'TEXTO_RICO':
        this.texto = d['html'];
        break;
      case 'CALLOUT':
        this.callout = { ...(d as never as Callout), titulo: (d['titulo'] as string | null) ?? '' };
        break;
      case 'TABLA':
        this.tabla = {
          ...(d as never as BlockData['TABLA']),
          titulo: (d['titulo'] as string | null) ?? '',
          fuente: (d['fuente'] as string | null) ?? '',
          nota: (d['nota'] as string | null) ?? '',
        };
        break;
      case 'ALGORITMO':
        this.algoritmo = d as never as Algoritmo;
        this.algoritmo.conexiones.forEach((c) => (c.etiqueta = c.etiqueta ?? ''));
        break;
      case 'IMAGEN':
        this.activoId = d['activoId'];
        this.pie = (d['pie'] as string | null) ?? '';
        break;
      case 'DIAGRAMA':
        this.activoId = d['activoId'];
        this.titulo = d['titulo'];
        this.descripcionTextual = d['descripcionTextual'];
        break;
      case 'ACORDEON':
        this.secciones = d['secciones'];
        break;
      case 'REFERENCIAS':
        this.referencias = d['referencias'];
        break;
      case 'ADJUNTO':
        this.activoId = d['activoId'];
        this.titulo = d['titulo'];
        this.descripcion = (d['descripcion'] as string | null) ?? '';
        break;
    }
  }

  // ----- Texto -----

  protected wrap(open: string, close: string): void {
    const el = this.area()?.nativeElement;
    if (!el) {
      return;
    }
    const start = el.selectionStart;
    const end = el.selectionEnd;
    const selected = this.texto.slice(start, end) || 'texto';
    const insert = `${open.replace(/\\n/g, '\n')}${selected}${close.replace(/\\n/g, '\n')}`;
    this.texto = `${this.texto.slice(0, start)}${insert}${this.texto.slice(end)}`;
  }

  // ----- Tabla -----

  protected addColumn(): void {
    this.tabla.encabezados.push('');
    this.tabla.filas.forEach((f) => f.push(''));
  }

  protected removeColumn(): void {
    this.tabla.encabezados.pop();
    this.tabla.filas.forEach((f) => f.pop());
  }

  protected addRow(): void {
    this.tabla.filas.push(this.tabla.encabezados.map(() => ''));
  }

  protected importTable(): void {
    const lines = this.pegado
      .split(/\r?\n/)
      .map((line) => line.split('\t').map((cell) => cell.trim()))
      .filter((cells) => cells.some((cell) => cell.length > 0));
    if (lines.length < 2) {
      this.error.set('Pega al menos el encabezado y una fila, separados por tabulaciones.');
      return;
    }
    const columns = Math.min(8, Math.max(...lines.map((l) => l.length)));
    const fit = (cells: string[]) => Array.from({ length: columns }, (_, i) => cells[i] ?? '');
    this.tabla.encabezados = fit(lines[0]);
    this.tabla.filas = lines.slice(1, 61).map(fit);
    this.pegado = '';
    this.error.set(null);
  }

  // ----- Algoritmo -----

  protected addNode(): void {
    const used = new Set(this.algoritmo.nodos.map((n) => n.id));
    let n = this.algoritmo.nodos.length + 1;
    while (used.has(`n${n}`)) {
      n++;
    }
    this.algoritmo.nodos.push({ id: `n${n}`, tipo: 'ACCION', texto: '' });
  }

  protected removeNode(index: number): void {
    const [removed] = this.algoritmo.nodos.splice(index, 1);
    this.algoritmo.conexiones = this.algoritmo.conexiones.filter(
      (c) => c.desde !== removed.id && c.hacia !== removed.id,
    );
  }

  protected addConnection(): void {
    const [first, second] = this.algoritmo.nodos;
    this.algoritmo.conexiones.push({ desde: first.id, hacia: second.id, etiqueta: '' });
  }

  // ----- Referencias -----

  protected addReference(): void {
    this.referencias.push(this.emptyReference());
  }

  private emptyReference(): Referencia {
    return { titulo: '', autores: '', emisor: '', url: '', doi: '', fecha: '', etiquetaEvidencia: '' };
  }

  // ----- Guardado -----

  protected submit(): void {
    const datos = this.build();
    if (!datos) {
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.data.save(datos).subscribe({
      next: (detail) => this.ref.close(detail),
      error: (error: unknown) => {
        this.saving.set(false);
        const apiError = toApiError(error);
        const fields = apiError.fieldErrors?.map((f) => f.message).join(' ');
        this.error.set(fields ? `${apiError.message} ${fields}` : apiError.message);
      },
    });
  }

  private build(): BlockData[TipoBloque] | null {
    const blank = (value: string | null | undefined) => (value ?? '').trim() || null;
    switch (this.data.tipo) {
      case 'TEXTO_RICO':
        return { html: toHtml(this.texto) };
      case 'CALLOUT':
        return { ...this.callout, titulo: blank(this.callout.titulo) };
      case 'TABLA':
        return {
          titulo: blank(this.tabla.titulo),
          encabezados: this.tabla.encabezados.map((h) => h.trim()),
          filas: this.tabla.filas.map((f) => f.map((c) => c.trim())),
          fuente: blank(this.tabla.fuente),
          nota: blank(this.tabla.nota),
        };
      case 'ALGORITMO':
        return {
          ...this.algoritmo,
          conexiones: this.algoritmo.conexiones.map((c) => ({ ...c, etiqueta: blank(c.etiqueta) })),
        };
      case 'IMAGEN':
        if (!this.activoId) {
          return this.fail('Selecciona o sube una imagen.');
        }
        return { activoId: this.activoId, pie: blank(this.pie) };
      case 'DIAGRAMA':
        if (!this.activoId) {
          return this.fail('Selecciona o sube el archivo del diagrama.');
        }
        return { activoId: this.activoId, titulo: this.titulo.trim(), descripcionTextual: this.descripcionTextual.trim() };
      case 'ACORDEON':
        return {
          secciones: this.secciones.map((s) => ({ titulo: s.titulo.trim(), html: toHtml(s.html) })),
        };
      case 'REFERENCIAS':
        return {
          referencias: this.referencias.map((r) => ({
            titulo: r.titulo.trim(),
            autores: blank(r.autores),
            emisor: blank(r.emisor),
            url: blank(r.url),
            doi: blank(r.doi),
            fecha: blank(r.fecha),
            etiquetaEvidencia: blank(r.etiquetaEvidencia),
          })),
        };
      case 'ADJUNTO':
        if (!this.activoId) {
          return this.fail('Selecciona o sube el PDF.');
        }
        return { activoId: this.activoId, titulo: this.titulo.trim(), descripcion: blank(this.descripcion) };
    }
  }

  private fail(message: string): null {
    this.error.set(message);
    return null;
  }
}
