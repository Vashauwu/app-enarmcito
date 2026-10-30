import { Routes } from '@angular/router';
import { authGuard, guestGuard, permissionGuard } from './core/auth/auth.guards';
import { APP_NAME } from './core/brand';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: `${APP_NAME} · Medicina en lectura guiada`,
    loadComponent: () => import('./features/landing/landing-page').then((m) => m.LandingPage),
  },
  {
    path: 'login',
    title: `Iniciar sesión · ${APP_NAME}`,
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login-page').then((m) => m.LoginPage),
  },
  {
    path: 'registro',
    title: `Crear cuenta · ${APP_NAME}`,
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/register-page').then((m) => m.RegisterPage),
  },
  {
    path: 'acceso',
    title: `Definir contraseña · ${APP_NAME}`,
    loadComponent: () => import('./features/auth/access-page').then((m) => m.AccessPage),
  },
  {
    path: 'admin',
    canActivate: [authGuard, permissionGuard('admin.access')],
    loadComponent: () => import('./features/admin/admin-shell').then((m) => m.AdminShell),
    children: [
      {
        path: '',
        title: `Panel administrativo · ${APP_NAME}`,
        loadComponent: () =>
          import('./features/admin/dashboard/admin-dashboard').then((m) => m.AdminDashboard),
      },
      {
        path: 'usuarios',
        title: `Usuarios · ${APP_NAME}`,
        canActivate: [permissionGuard('users.read')],
        loadComponent: () => import('./features/admin/users/users-page').then((m) => m.UsersPage),
      },
      {
        path: 'usuarios/:id',
        title: `Ficha de usuario · ${APP_NAME}`,
        canActivate: [permissionGuard('users.read')],
        loadComponent: () =>
          import('./features/admin/users/user-detail-page').then((m) => m.UserDetailPage),
      },
      {
        path: 'contenido',
        title: `Contenido · ${APP_NAME}`,
        canActivate: [permissionGuard('content.read.admin')],
        loadComponent: () =>
          import('./features/admin/content/content-admin-page').then((m) => m.ContentAdminPage),
      },
      {
        path: 'contenido/nuevo',
        title: `Nuevo contenido · ${APP_NAME}`,
        canActivate: [permissionGuard('content.create')],
        loadComponent: () =>
          import('./features/admin/content/content-editor-page').then((m) => m.ContentEditorPage),
      },
      {
        path: 'contenido/:id/editar',
        title: `Editar contenido · ${APP_NAME}`,
        canActivate: [permissionGuard('content.read.admin')],
        loadComponent: () =>
          import('./features/admin/content/content-editor-page').then((m) => m.ContentEditorPage),
      },
      {
        path: 'actividad',
        title: `Actividad · ${APP_NAME}`,
        canActivate: [permissionGuard('users.read')],
        loadComponent: () =>
          import('./features/admin/activity/activity-page').then((m) => m.ActivityPage),
      },
      {
        path: 'auditoria',
        title: `Auditoría · ${APP_NAME}`,
        canActivate: [permissionGuard('audit.read', 'audit.read.editorial', 'audit.read.own')],
        loadComponent: () => import('./features/admin/audit/audit-page').then((m) => m.AuditPage),
      },
    ],
  },
  {
    path: 'app',
    canActivate: [authGuard],
    loadComponent: () => import('./features/shell/app-shell').then((m) => m.AppShell),
    children: [
      {
        path: '',
        title: `Mi aprendizaje · ${APP_NAME}`,
        loadComponent: () =>
          import('./features/dashboard/dashboard-page').then((m) => m.DashboardPage),
      },
      {
        path: 'contenido/:slug',
        title: `Lectura guiada · ${APP_NAME}`,
        loadComponent: () =>
          import('./features/content/content-detail-page').then((m) => m.ContentDetailPage),
      },
      {
        path: 'perfil',
        title: `Mi perfil · ${APP_NAME}`,
        loadComponent: () => import('./features/profile/profile-page').then((m) => m.ProfilePage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
