import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import localeEsMx from '@angular/common/locales/es-MX';
import {
  ApplicationConfig,
  LOCALE_ID,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { MAT_FORM_FIELD_DEFAULT_OPTIONS } from '@angular/material/form-field';
import { MAT_ICON_DEFAULT_OPTIONS } from '@angular/material/icon';
import { MatPaginatorIntl } from '@angular/material/paginator';
import {
  provideRouter,
  withComponentInputBinding,
  withInMemoryScrolling,
} from '@angular/router';
import { routes } from './app.routes';
import { AuthService } from './core/auth/auth.service';
import { sessionExpiredInterceptor } from './core/auth/session-expired.interceptor';
import { SpanishPaginatorIntl } from './core/spanish-paginator-intl';

registerLocaleData(localeEsMx);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(
      routes,
      withComponentInputBinding(),
      withInMemoryScrolling({ scrollPositionRestoration: 'top', anchorScrolling: 'enabled' }),
    ),
    // La protección XSRF de HttpClient (cookie XSRF-TOKEN → encabezado X-XSRF-TOKEN) está activa por defecto.
    provideHttpClient(withFetch(), withInterceptors([sessionExpiredInterceptor])),
    provideAppInitializer(() => inject(AuthService).restoreSession()),
    { provide: LOCALE_ID, useValue: 'es-MX' },
    { provide: MatPaginatorIntl, useClass: SpanishPaginatorIntl },
    { provide: MAT_ICON_DEFAULT_OPTIONS, useValue: { fontSet: 'material-symbols-outlined' } },
    { provide: MAT_FORM_FIELD_DEFAULT_OPTIONS, useValue: { appearance: 'outline' } },
  ],
};
