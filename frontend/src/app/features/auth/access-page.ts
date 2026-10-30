import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { toApiError } from '../../core/api-error';
import { API_URL } from '../../core/api.models';
import { APP_NAME } from '../../core/brand';
import { PASSWORD_MAX_LENGTH, PASSWORD_MIN_LENGTH, matchFields } from '../../shared/validators';

interface LinkInfo {
  tipo: 'INVITACION' | 'RESTABLECIMIENTO';
  nombre: string;
  correo: string;
  expiraAt: string;
}

/**
 * Define la contraseña con un enlace de invitación o restablecimiento (`/acceso#token=…`). El token viaja
 * en el fragmento para no llegar a los logs del servidor, y se retira de la barra de direcciones al leerlo.
 */
@Component({
  selector: 'app-access-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
  ],
  templateUrl: './access-page.html',
})
export class AccessPage implements OnInit {
  private readonly http = inject(HttpClient);

  protected readonly appName = APP_NAME;
  protected readonly minLength = PASSWORD_MIN_LENGTH;
  protected readonly state = signal<'loading' | 'invalid' | 'ready' | 'done'>('loading');
  protected readonly info = signal<LinkInfo | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly submitting = signal(false);
  protected readonly hidePassword = signal(true);
  private token = '';

  protected readonly form = inject(NonNullableFormBuilder).group(
    {
      password: [
        '',
        [Validators.required, Validators.minLength(PASSWORD_MIN_LENGTH), Validators.maxLength(PASSWORD_MAX_LENGTH)],
      ],
      confirmPassword: ['', Validators.required],
    },
    { validators: matchFields('password', 'confirmPassword') },
  );

  ngOnInit(): void {
    this.token = new URLSearchParams(window.location.hash.slice(1)).get('token') ?? '';
    history.replaceState(history.state, '', window.location.pathname);
    if (!this.token) {
      this.state.set('invalid');
      return;
    }
    this.http.post<LinkInfo>(`${API_URL}/auth/acceso/consultar`, { token: this.token }).subscribe({
      next: (info) => {
        this.info.set(info);
        this.state.set('ready');
      },
      error: (error: unknown) => {
        this.errorMessage.set(toApiError(error).message);
        this.state.set('invalid');
      },
    });
  }

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    this.http
      .post(`${API_URL}/auth/acceso/definir-password`, {
        token: this.token,
        password: this.form.controls.password.value,
      })
      .subscribe({
        next: () => this.state.set('done'),
        error: (error: unknown) => {
          this.submitting.set(false);
          this.errorMessage.set(toApiError(error).message);
        },
      });
  }
}
