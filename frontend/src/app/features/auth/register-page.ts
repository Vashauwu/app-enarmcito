import { Component, computed, inject, signal } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { applyFieldErrors, toApiError } from '../../core/api-error';
import { AuthService } from '../../core/auth/auth.service';
import { APP_NAME } from '../../core/brand';
import { ContentService } from '../../core/content.service';
import {
  PASSWORD_MAX_LENGTH,
  PASSWORD_MIN_LENGTH,
  matchFields,
  noBlank,
} from '../../shared/validators';

@Component({
  selector: 'app-register-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './register-page.html',
})
export class RegisterPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly contentService = inject(ContentService);

  protected readonly appName = APP_NAME;
  protected readonly passwordMinLength = PASSWORD_MIN_LENGTH;
  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly hidePassword = signal(true);

  protected readonly tiers = rxResource({ stream: () => this.contentService.tiers() });

  protected readonly form = inject(NonNullableFormBuilder).group(
    {
      nombre: ['', [Validators.required, noBlank, Validators.maxLength(80)]],
      apellido: ['', [Validators.required, noBlank, Validators.maxLength(100)]],
      correo: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
      password: [
        '',
        [
          Validators.required,
          Validators.minLength(PASSWORD_MIN_LENGTH),
          Validators.maxLength(PASSWORD_MAX_LENGTH),
        ],
      ],
      confirmPassword: ['', Validators.required],
      tierId: [null as number | null, Validators.required],
    },
    { validators: matchFields('password', 'confirmPassword') },
  );

  private readonly selectedTierId = toSignal(this.form.controls.tierId.valueChanges, {
    initialValue: null,
  });
  protected readonly selectedTier = computed(() =>
    this.tiers.value()?.find((tier) => tier.id === this.selectedTierId()),
  );

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    const { nombre, apellido, correo, password, tierId } = this.form.getRawValue();
    this.submitting.set(true);
    this.errorMessage.set(null);
    this.auth
      .register({ nombre: nombre.trim(), apellido: apellido.trim(), correo: correo.trim(), password, tierId: tierId! })
      .subscribe({
        next: () => void this.router.navigateByUrl('/app'),
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
