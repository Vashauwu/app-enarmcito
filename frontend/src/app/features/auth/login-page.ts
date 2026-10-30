import { Component, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { toApiError } from '../../core/api-error';
import { AuthService } from '../../core/auth/auth.service';
import { APP_NAME } from '../../core/brand';

@Component({
  selector: 'app-login-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  templateUrl: './login-page.html',
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Query params (`?returnUrl=…&sesion=expirada`) enlazados por el router. */
  readonly returnUrl = input<string>();
  readonly sesion = input<string>();

  protected readonly appName = APP_NAME;
  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly hidePassword = signal(true);

  protected readonly form = inject(NonNullableFormBuilder).group({
    correo: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    password: ['', [Validators.required, Validators.maxLength(128)]],
  });

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => void this.router.navigateByUrl(this.safeReturnUrl()),
      error: (error: unknown) => {
        this.submitting.set(false);
        this.errorMessage.set(toApiError(error).message);
      },
    });
  }

  /** Sólo se permite volver a rutas internas de la zona privada. */
  private safeReturnUrl(): string {
    const url = this.returnUrl();
    return url && url.startsWith('/app') ? url : '/app';
  }
}
