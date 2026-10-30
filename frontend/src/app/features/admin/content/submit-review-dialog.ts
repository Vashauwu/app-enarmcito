import { Component, inject } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ROLE_LABEL } from '../../../core/admin/admin-labels';
import { PersonaConRol } from '../../../core/admin/admin.models';

export interface SubmitReviewData {
  revisores: PersonaConRol[];
  revisorActual: string | null;
}

export interface SubmitReviewResult {
  revisorId: string;
  comentario: string | null;
}

@Component({
  selector: 'app-submit-review-dialog',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  template: `
    <h2 mat-dialog-title>Enviar a revisión</h2>
    <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
      <mat-dialog-content>
        <p class="intro">El texto queda bloqueado hasta que el revisor apruebe o solicite cambios.</p>
        @if (data.revisores.length === 0) {
          <p class="form-alert" role="alert">No hay revisores activos disponibles. Pide a un administrador que asigne el rol.</p>
        }
        <mat-form-field class="full">
          <mat-label>Revisor</mat-label>
          <mat-select formControlName="revisorId">
            @for (persona of data.revisores; track persona.id) {
              <mat-option [value]="persona.id">{{ persona.nombre }} · {{ roleLabel[persona.rol] }}</mat-option>
            }
          </mat-select>
          <mat-hint>Debe ser una persona distinta del autor.</mat-hint>
          <mat-error>Selecciona un revisor.</mat-error>
        </mat-form-field>
        <mat-form-field class="full">
          <mat-label>Nota para el revisor (opcional)</mat-label>
          <textarea matInput formControlName="comentario" rows="3" maxlength="2000"></textarea>
        </mat-form-field>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button matButton mat-dialog-close type="button">Cancelar</button>
        <button matButton="filled" type="submit" [disabled]="data.revisores.length === 0">Enviar</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .intro {
      margin: 0 0 16px;
      color: var(--mat-sys-on-surface-variant);
    }
    .full {
      width: 100%;
    }
  `,
})
export class SubmitReviewDialog {
  protected readonly data = inject<SubmitReviewData>(MAT_DIALOG_DATA);
  private readonly ref = inject<MatDialogRef<SubmitReviewDialog, SubmitReviewResult>>(MatDialogRef);
  protected readonly roleLabel = ROLE_LABEL;

  protected readonly form = inject(NonNullableFormBuilder).group({
    revisorId: [this.data.revisorActual ?? '', Validators.required],
    comentario: [''],
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { revisorId, comentario } = this.form.getRawValue();
    this.ref.close({ revisorId, comentario: comentario.trim() || null });
  }
}
