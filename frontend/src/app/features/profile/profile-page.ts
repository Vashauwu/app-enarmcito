import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { applyFieldErrors, toApiError } from '../../core/api-error';
import { AuthService } from '../../core/auth/auth.service';
import { googleMessage } from '../../core/google-auth';
import { GoogleSignIn } from '../../shared/google-sign-in/google-sign-in';
import { noBlank } from '../../shared/validators';

@Component({
  selector: 'app-profile-page',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    GoogleSignIn,
  ],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.scss',
})
export class ProfilePage {
  private readonly auth = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);

  /** Código con el que el backend vuelve de Google (`?google=vinculada`…). */
  readonly google = input<string>();

  protected readonly user = this.auth.user;
  protected readonly googleResult = computed(() => googleMessage(this.google()));
  protected readonly identities = rxResource({ stream: () => this.auth.identities() });
  protected readonly unlinking = signal(false);
  protected readonly unlinkError = signal<string | null>(null);
  protected readonly saving = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group({
    nombre: [this.user()?.nombre ?? '', [Validators.required, noBlank, Validators.maxLength(80)]],
    apellido: [this.user()?.apellido ?? '', [Validators.required, noBlank, Validators.maxLength(100)]],
  });

  protected save(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }
    const { nombre, apellido } = this.form.getRawValue();
    this.saving.set(true);
    this.errorMessage.set(null);
    this.auth.updateProfile({ nombre: nombre.trim(), apellido: apellido.trim() }).subscribe({
      next: (user) => {
        this.saving.set(false);
        this.form.reset({ nombre: user.nombre, apellido: user.apellido });
        this.snackBar.open('Perfil actualizado', undefined, { duration: 3000 });
      },
      error: (error: unknown) => {
        this.saving.set(false);
        const apiError = toApiError(error);
        if (!applyFieldErrors(this.form, apiError)) {
          this.errorMessage.set(apiError.message);
        }
      },
    });
  }

  protected unlinkGoogle(): void {
    if (this.unlinking()) {
      return;
    }
    this.unlinking.set(true);
    this.unlinkError.set(null);
    this.auth.unlinkGoogle().subscribe({
      next: () => {
        this.unlinking.set(false);
        this.identities.reload();
        this.snackBar.open('Google se desvinculó de tu cuenta', undefined, { duration: 3000 });
      },
      error: (error: unknown) => {
        this.unlinking.set(false);
        this.unlinkError.set(toApiError(error).message);
      },
    });
  }

  protected discard(): void {
    this.form.reset({ nombre: this.user()?.nombre ?? '', apellido: this.user()?.apellido ?? '' });
    this.errorMessage.set(null);
  }
}
