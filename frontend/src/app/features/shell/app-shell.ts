import { Component, computed, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatToolbarModule } from '@angular/material/toolbar';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { APP_NAME } from '../../core/brand';
import { SEARCH_MAX_LENGTH } from '../../core/content.service';
import { ThemeToggle } from '../../shared/theme-toggle/theme-toggle';

@Component({
  selector: 'app-shell',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    RouterOutlet,
    MatButtonModule,
    MatDividerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatMenuModule,
    MatToolbarModule,
    ThemeToggle,
  ],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.scss',
})
export class AppShell {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly appName = APP_NAME;
  protected readonly searchMaxLength = SEARCH_MAX_LENGTH;
  protected readonly user = this.auth.user;
  protected readonly canAdmin = computed(() => this.auth.can('admin.access'));

  protected readonly initials = computed(() => {
    const user = this.user();
    return user ? `${user.nombre.charAt(0)}${user.apellido.charAt(0)}`.toUpperCase() : '';
  });

  protected readonly search = inject(NonNullableFormBuilder).control('', [
    Validators.maxLength(SEARCH_MAX_LENGTH),
  ]);

  constructor() {
    // Mantiene el buscador sincronizado con `?q=` (por ejemplo, al usar atrás/adelante).
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.syncSearchFromUrl());
    this.syncSearchFromUrl();
  }

  protected submitSearch(): void {
    const q = this.search.value.trim();
    void this.router.navigate(['/app'], { queryParams: q ? { q } : {} });
  }

  protected clearSearch(): void {
    this.search.setValue('');
    void this.router.navigate(['/app']);
  }

  protected logout(): void {
    this.auth.logout().subscribe({
      next: () => void this.router.navigateByUrl('/'),
      // Aunque falle la llamada, la sesión local ya se limpió: se vuelve a la landing.
      error: () => {
        void this.router.navigateByUrl('/');
        this.snackBar.open('No pudimos confirmar el cierre de sesión con el servidor.', 'Cerrar', {
          duration: 5000,
        });
      },
    });
  }

  private syncSearchFromUrl(): void {
    const q = this.router.parseUrl(this.router.url).queryParamMap.get('q') ?? '';
    if (this.search.value !== q) {
      this.search.setValue(q);
    }
  }
}
