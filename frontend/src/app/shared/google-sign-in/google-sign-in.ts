import { Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { AuthService } from '../../core/auth/auth.service';

/**
 * Botón «Continuar con Google». Es una navegación completa a la API (no una llamada XHR): el backend
 * redirige a Google y, al volver, crea la sesión y redirige al frontend. Sólo aparece si el ambiente tiene
 * Google configurado. Con `modo="vincular"` añade Google a la cuenta con la sesión iniciada.
 */
@Component({
  selector: 'app-google-sign-in',
  template: `
    @if (available()) {
      <a class="google-btn" [href]="href()">
        <svg viewBox="0 0 48 48" width="20" height="20" aria-hidden="true">
          <path fill="#EA4335" d="M24 9.5c3.5 0 6.6 1.2 9.1 3.6l6.8-6.8C35.8 2.4 30.3 0 24 0 14.6 0 6.5 5.4 2.6 13.2l7.9 6.1C12.4 13.6 17.7 9.5 24 9.5z" />
          <path fill="#4285F4" d="M46.1 24.6c0-1.6-.1-3.1-.4-4.6H24v9.1h12.4c-.5 2.9-2.2 5.3-4.6 6.9l7.4 5.7c4.3-4 6.9-9.9 6.9-17.1z" />
          <path fill="#FBBC05" d="M10.5 28.7a14.5 14.5 0 0 1 0-9.4l-7.9-6.1a24 24 0 0 0 0 21.6l7.9-6.1z" />
          <path fill="#34A853" d="M24 48c6.5 0 11.9-2.1 15.9-5.8l-7.4-5.7c-2.1 1.4-4.8 2.3-8.5 2.3-6.3 0-11.6-4.1-13.5-9.8l-7.9 6.1C6.5 42.6 14.6 48 24 48z" />
        </svg>
        <span>{{ label() }}</span>
      </a>
      @if (divider()) {
        <div class="google-divider" role="separator"><span>o con tu correo</span></div>
      }
    }
  `,
  styleUrl: './google-sign-in.scss',
})
export class GoogleSignIn {
  private readonly auth = inject(AuthService);

  readonly modo = input<'acceso' | 'vincular'>('acceso');
  readonly label = input('Continuar con Google');
  /** Separador «o con tu correo» bajo el botón, para usarlo encima de un formulario. */
  readonly divider = input(false);

  /** Si falla la consulta, el botón simplemente no se muestra: el acceso por correo sigue disponible. */
  private readonly providers = rxResource({ stream: () => this.auth.providers() });

  protected readonly available = computed(
    () => this.providers.hasValue() && this.providers.value().google,
  );
  protected readonly href = computed(() =>
    this.modo() === 'vincular'
      ? '/api/v1/auth/google/iniciar?modo=vincular'
      : '/api/v1/auth/google/iniciar',
  );
}
