import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
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
  ],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.scss',
})
export class ProfilePage {
  private readonly auth = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly user = this.auth.user;
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

  protected discard(): void {
    this.form.reset({ nombre: this.user()?.nombre ?? '', apellido: this.user()?.apellido ?? '' });
    this.errorMessage.set(null);
  }
}
