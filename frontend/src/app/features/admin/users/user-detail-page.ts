import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, input, linkedSignal, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { Observable, filter, switchMap } from 'rxjs';
import { AdminApi } from '../../../core/admin/admin-api.service';
import { ACCOUNT_STATUS, ROLE_LABEL } from '../../../core/admin/admin-labels';
import { AdminUserDetail, Rol } from '../../../core/admin/admin.models';
import { applyFieldErrors, toApiError } from '../../../core/api-error';
import { AuthService } from '../../../core/auth/auth.service';
import { AccessLinkDialog } from '../../../shared/access-link-dialog/access-link-dialog';
import { confirmAction } from '../../../shared/confirm-dialog/confirm-dialog';
import { StateMessage } from '../../../shared/state-message/state-message';
import { StatusBadge } from '../../../shared/status-badge/status-badge';
import { noBlank } from '../../../shared/validators';

@Component({
  selector: 'app-user-detail-page',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatSelectModule,
    StateMessage,
    StatusBadge,
  ],
  templateUrl: './user-detail-page.html',
  styleUrl: './user-detail-page.scss',
})
export class UserDetailPage {
  private readonly api = inject(AdminApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly auth = inject(AuthService);

  /** Parámetro de ruta `:id`. */
  readonly id = input.required<string>();

  protected readonly accountStatus = ACCOUNT_STATUS;
  protected readonly roleLabel = ROLE_LABEL;
  protected readonly saving = signal(false);
  protected readonly busy = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly resource = rxResource({ params: () => this.id(), stream: ({ params }) => this.api.user(params) });
  protected readonly options = rxResource({ stream: () => this.api.userOptions() });
  /** Ficha mostrada; se reemplaza con la respuesta de cada acción. */
  protected readonly detail = linkedSignal(() => this.resource.value() ?? null);
  protected readonly loadError = computed(() => {
    const error = this.resource.error();
    return error ? toApiError(error) : null;
  });

  protected readonly form = inject(NonNullableFormBuilder).group({
    nombre: ['', [Validators.required, noBlank, Validators.maxLength(80)]],
    apellido: ['', [Validators.required, noBlank, Validators.maxLength(100)]],
    tierId: [0, Validators.required],
    motivo: ['', Validators.maxLength(1000)],
  });
  protected readonly newRole = signal<Rol | null>(null);

  constructor() {
    // Rellena el formulario y lo habilita según las acciones permitidas para esta cuenta.
    effect(() => {
      const d = this.detail();
      if (!d) {
        return;
      }
      this.form.reset({
        nombre: d.usuario.nombre,
        apellido: d.usuario.apellido,
        tierId: d.usuario.tier.id,
        motivo: '',
      });
      toggle(this.form.controls.nombre, d.acciones.editarDatos);
      toggle(this.form.controls.apellido, d.acciones.editarDatos);
      toggle(this.form.controls.tierId, d.acciones.cambiarTier);
      toggle(this.form.controls.motivo, d.acciones.editarDatos || d.acciones.cambiarTier);
      this.newRole.set(null);
    });
  }

  protected save(): void {
    const d = this.detail();
    if (!d || this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.saving.set(true);
    this.errorMessage.set(null);
    this.api
      .updateUser(d.usuario.id, {
        nombre: value.nombre.trim(),
        apellido: value.apellido.trim(),
        tierId: value.tierId,
        motivo: value.motivo.trim() || null,
      })
      .subscribe({
        next: (updated) => this.done(updated, 'Datos actualizados'),
        error: (error: unknown) => {
          this.saving.set(false);
          const apiError = toApiError(error);
          if (!applyFieldErrors(this.form, apiError)) {
            this.errorMessage.set(apiError.message);
          }
        },
      });
  }

  protected toggleBlock(): void {
    const d = this.detail();
    if (!d) {
      return;
    }
    const blocking = d.usuario.estado === 'ACTIVO';
    this.run(
      {
        title: blocking ? 'Bloquear cuenta' : 'Desbloquear cuenta',
        message: blocking
          ? `${d.usuario.nombre} no podrá iniciar sesión y su sesión actual se cerrará.`
          : `${d.usuario.nombre} podrá volver a iniciar sesión.`,
        confirmLabel: blocking ? 'Bloquear' : 'Desbloquear',
        tone: blocking ? 'danger' : 'primary',
        reason: { label: 'Motivo', required: true, hint: 'Queda registrado en la bitácora.' },
      },
      (motivo) => (blocking ? this.api.blockUser(d.usuario.id, motivo) : this.api.unblockUser(d.usuario.id, motivo)),
      blocking ? 'Cuenta bloqueada' : 'Cuenta desbloqueada',
    );
  }

  protected changeRole(): void {
    const d = this.detail();
    const rol = this.newRole();
    if (!d || !rol) {
      return;
    }
    this.run(
      {
        title: 'Cambiar rol',
        message: `${d.usuario.nombre} pasará de «${ROLE_LABEL[d.usuario.rol]}» a «${ROLE_LABEL[rol]}». Sus permisos cambian en su siguiente acción.`,
        confirmLabel: 'Cambiar rol',
        tone: 'danger',
        reason: { label: 'Motivo', required: true, hint: 'Queda registrado en la bitácora.' },
      },
      (motivo) => this.api.changeRole(d.usuario.id, rol, motivo),
      'Rol actualizado',
    );
  }

  protected resetAccess(): void {
    const d = this.detail();
    if (!d) {
      return;
    }
    const pending = d.usuario.estado === 'PENDIENTE';
    confirmAction(this.dialog, {
      title: pending ? 'Generar nueva invitación' : 'Restablecer acceso',
      message: pending
        ? 'Se generará un nuevo enlace de activación; el anterior dejará de servir.'
        : `Se generará un enlace para que ${d.usuario.nombre} defina una contraseña nueva. La actual sigue funcionando hasta que use el enlace.`,
      confirmLabel: 'Generar enlace',
      reason: { label: 'Motivo', required: true },
    })
      .pipe(
        filter((motivo): motivo is string => motivo !== undefined),
        switchMap((motivo) => {
          this.busy.set(true);
          return this.api.resetAccess(d.usuario.id, motivo);
        }),
      )
      .subscribe({
        next: (result) => {
          this.busy.set(false);
          this.detail.set(result.usuario);
          this.dialog.open(AccessLinkDialog, {
            data: { enlace: result.enlace, nombre: result.usuario.usuario.nombre },
            width: '560px',
            maxWidth: 'calc(100vw - 32px)',
          });
        },
        error: (error: unknown) => this.fail(error),
      });
  }

  private run(
    options: Parameters<typeof confirmAction>[1],
    action: (motivo: string) => Observable<AdminUserDetail>,
    successMessage: string,
  ): void {
    confirmAction(this.dialog, options)
      .pipe(
        filter((motivo): motivo is string => motivo !== undefined),
        switchMap((motivo) => {
          this.busy.set(true);
          return action(motivo);
        }),
      )
      .subscribe({
        next: (updated) => this.done(updated, successMessage),
        error: (error: unknown) => this.fail(error),
      });
  }

  private done(updated: AdminUserDetail, message: string): void {
    this.saving.set(false);
    this.busy.set(false);
    this.detail.set(updated);
    this.snackBar.open(message, undefined, { duration: 3000 });
  }

  private fail(error: unknown): void {
    this.busy.set(false);
    this.snackBar.open(toApiError(error).message, 'Cerrar', { duration: 6000 });
  }
}

function toggle(control: { enable(): void; disable(): void }, enabled: boolean): void {
  if (enabled) {
    control.enable();
  } else {
    control.disable();
  }
}
