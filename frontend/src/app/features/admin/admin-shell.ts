import { BreakpointObserver } from '@angular/cdk/layout';
import { Component, computed, inject, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { APP_NAME } from '../../core/brand';
import { ThemeToggle } from '../../shared/theme-toggle/theme-toggle';

interface NavItem {
  path: string;
  label: string;
  icon: string;
  permisos: string[];
  exact?: boolean;
}

const NAV: NavItem[] = [
  { path: '/admin', label: 'Inicio', icon: 'dashboard', permisos: ['admin.access'], exact: true },
  { path: '/admin/usuarios', label: 'Usuarios', icon: 'group', permisos: ['users.read'] },
  { path: '/admin/contenido', label: 'Contenido', icon: 'library_books', permisos: ['content.read.admin'] },
  { path: '/admin/actividad', label: 'Actividad', icon: 'monitoring', permisos: ['users.read'] },
  {
    path: '/admin/auditoria',
    label: 'Auditoría',
    icon: 'history',
    permisos: ['audit.read', 'audit.read.editorial', 'audit.read.own'],
  },
];

@Component({
  selector: 'app-admin-shell',
  imports: [
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    MatButtonModule,
    MatIconModule,
    MatListModule,
    MatSidenavModule,
    MatToolbarModule,
    ThemeToggle,
  ],
  templateUrl: './admin-shell.html',
  styleUrl: './admin-shell.scss',
})
export class AdminShell {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly sidenav = viewChild(MatSidenav);

  protected readonly appName = APP_NAME;
  protected readonly user = this.auth.user;
  protected readonly nav = computed(() => NAV.filter((item) => this.auth.can(...item.permisos)));
  protected readonly compact = toSignal(
    inject(BreakpointObserver)
      .observe('(max-width: 959px)')
      .pipe(map((state) => state.matches)),
    { initialValue: false },
  );

  protected closeOnMobile(): void {
    if (this.compact()) {
      void this.sidenav()?.close();
    }
  }

  protected logout(): void {
    this.auth.logout().subscribe({
      next: () => void this.router.navigateByUrl('/'),
      error: () => void this.router.navigateByUrl('/'),
    });
  }
}
