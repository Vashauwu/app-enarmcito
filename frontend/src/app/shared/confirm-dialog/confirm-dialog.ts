import { Component, inject } from '@angular/core';
import { AbstractControl, FormControl, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Observable } from 'rxjs';

export interface ConfirmOptions {
  title: string;
  message: string;
  confirmLabel: string;
  /** `danger` para acciones de alto impacto (bloquear, archivar). */
  tone?: 'primary' | 'danger';
  /** Pide un texto (motivo o comentario). */
  reason?: { label: string; hint?: string; required: boolean; minLength?: number };
}

/** Resultado: `undefined` si se cancela; en otro caso, el texto capturado (o `''`). */
export type ConfirmResult = string | undefined;

/** Confirmación explícita para acciones sensibles, con motivo opcional u obligatorio. */
@Component({
  selector: 'app-confirm-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <p class="message">{{ data.message }}</p>
      @if (data.reason; as reason) {
        <mat-form-field class="reason">
          <mat-label>{{ reason.label }}</mat-label>
          <textarea matInput [formControl]="text" rows="3" maxlength="1000" cdkFocusInitial></textarea>
          @if (reason.hint) {
            <mat-hint>{{ reason.hint }}</mat-hint>
          }
          @if (text.hasError('required')) {
            <mat-error>Este campo es obligatorio.</mat-error>
          } @else if (text.hasError('minlength')) {
            <mat-error>Escribe al menos {{ reason.minLength }} caracteres.</mat-error>
          }
        </mat-form-field>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button matButton mat-dialog-close type="button">Cancelar</button>
      <button
        matButton="filled"
        type="button"
        [class.danger]="data.tone === 'danger'"
        (click)="confirm()"
      >
        {{ data.confirmLabel }}
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .message {
      margin: 0 0 16px;
      max-width: 48ch;
    }
    .reason {
      width: 100%;
    }
    .danger {
      --mat-button-filled-container-color: var(--mat-sys-error);
      --mat-button-filled-label-text-color: var(--mat-sys-on-error);
    }
  `,
})
export class ConfirmDialog {
  protected readonly data = inject<ConfirmOptions>(MAT_DIALOG_DATA);
  private readonly ref = inject<MatDialogRef<ConfirmDialog, ConfirmResult>>(MatDialogRef);

  protected readonly text = new FormControl('', {
    nonNullable: true,
    validators: [
      ...(this.data.reason?.required ? [Validators.required, noBlank] : []),
      ...(this.data.reason?.minLength ? [Validators.minLength(this.data.reason.minLength)] : []),
    ],
  });

  protected confirm(): void {
    if (this.text.invalid) {
      this.text.markAsTouched();
      return;
    }
    this.ref.close(this.text.value.trim());
  }
}

function noBlank(control: AbstractControl): ValidationErrors | null {
  return String(control.value ?? '').trim().length === 0 ? { required: true } : null;
}

/** Abre la confirmación y emite el texto capturado, o `undefined` si se cancela. */
export function confirmAction(dialog: MatDialog, options: ConfirmOptions): Observable<ConfirmResult> {
  return dialog
    .open<ConfirmDialog, ConfirmOptions, ConfirmResult>(ConfirmDialog, {
      data: options,
      width: '520px',
      maxWidth: 'calc(100vw - 32px)',
      autoFocus: options.reason ? 'first-tabbable' : 'dialog',
    })
    .afterClosed();
}
