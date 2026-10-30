import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { AdminApi } from '../../../core/admin/admin-api.service';
import { Rol, UserOptions, WithAccessLink } from '../../../core/admin/admin.models';
import { applyFieldErrors, toApiError } from '../../../core/api-error';
import { noBlank } from '../../../shared/validators';

/** Alta controlada: crea la cuenta pendiente y devuelve el enlace de activación. */
@Component({
  selector: 'app-invite-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  template: `
    <h2 mat-dialog-title>Invitar persona</h2>
    <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
      <mat-dialog-content>
        <p class="intro">
          La cuenta queda pendiente hasta que la persona defina su contraseña con el enlace que verás al terminar.
        </p>
        @if (errorMessage(); as message) {
          <p class="form-alert" role="alert"><mat-icon aria-hidden="true">error</mat-icon>{{ message }}</p>
        }
        <div class="form-row">
          <mat-form-field>
            <mat-label>Nombre</mat-label>
            <input matInput formControlName="nombre" maxlength="80" />
            <mat-error>{{ form.controls.nombre.getError('server') ?? 'Escribe el nombre.' }}</mat-error>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Apellido</mat-label>
            <input matInput formControlName="apellido" maxlength="100" />
            <mat-error>{{ form.controls.apellido.getError('server') ?? 'Escribe el apellido.' }}</mat-error>
          </mat-form-field>
        </div>
        <mat-form-field class="full">
          <mat-label>Correo electrónico</mat-label>
          <input matInput type="email" formControlName="correo" maxlength="254" />
          <mat-error>{{ form.controls.correo.getError('server') ?? 'Escribe un correo válido.' }}</mat-error>
        </mat-form-field>
        <div class="form-row">
          <mat-form-field>
            <mat-label>Rol</mat-label>
            <mat-select formControlName="rol">
              @for (rol of data.rolesCreables; track rol.codigo) {
                <mat-option [value]="rol.codigo">{{ rol.nombre }}</mat-option>
              }
            </mat-select>
            <mat-error>Selecciona un rol.</mat-error>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Tier</mat-label>
            <mat-select formControlName="tierId">
              @for (tier of data.tiers; track tier.id) {
                <mat-option [value]="tier.id">{{ tier.nombre }}</mat-option>
              }
            </mat-select>
            <mat-hint>Contenido de aprendizaje al que accede.</mat-hint>
            <mat-error>Selecciona un tier.</mat-error>
          </mat-form-field>
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button matButton mat-dialog-close type="button">Cancelar</button>
        <button matButton="filled" type="submit" [disabled]="submitting()">
          {{ submitting() ? 'Creando…' : 'Crear invitación' }}
        </button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .intro {
      margin: 0 0 16px;
      color: var(--mat-sys-on-surface-variant);
    }
    .full,
    .form-row mat-form-field {
      width: 100%;
    }
  `,
})
export class InviteDialog {
  protected readonly data = inject<UserOptions>(MAT_DIALOG_DATA);
  private readonly ref = inject<MatDialogRef<InviteDialog, WithAccessLink>>(MatDialogRef);
  private readonly api = inject(AdminApi);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group({
    nombre: ['', [Validators.required, noBlank, Validators.maxLength(80)]],
    apellido: ['', [Validators.required, noBlank, Validators.maxLength(100)]],
    correo: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    rol: [null as Rol | null, Validators.required],
    tierId: [this.data.tiers[0]?.id ?? null, Validators.required],
  });

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.submitting.set(true);
    this.errorMessage.set(null);
    this.api
      .invite({
        nombre: value.nombre.trim(),
        apellido: value.apellido.trim(),
        correo: value.correo.trim(),
        rol: value.rol!,
        tierId: value.tierId!,
      })
      .subscribe({
        next: (result) => this.ref.close(result),
        error: (error: unknown) => {
          this.submitting.set(false);
          const apiError = toApiError(error);
          if (!applyFieldErrors(this.form, apiError)) {
            this.errorMessage.set(apiError.message);
          }
        },
      });
  }
}
