import { MAT_DATE_LOCALE, provideNativeDateAdapter } from '@angular/material/core';
import { Provider } from '@angular/core';
import { EstadoContenido, EstadoCuenta, Rol } from './admin.models';

/** Selector de fechas en español; se provee sólo en las pantallas que lo usan. */
export const DATE_PROVIDERS: Provider[] = [
  provideNativeDateAdapter(),
  { provide: MAT_DATE_LOCALE, useValue: 'es-MX' },
];

/** Etiqueta + ícono: los estados nunca dependen sólo del color. */
export interface StatusLabel {
  label: string;
  icon: string;
  tone: 'neutral' | 'info' | 'warning' | 'success' | 'danger';
}

export const CONTENT_STATUS: Record<EstadoContenido, StatusLabel> = {
  BORRADOR: { label: 'Borrador', icon: 'edit_note', tone: 'neutral' },
  EN_EDICION: { label: 'En edición', icon: 'edit', tone: 'neutral' },
  EN_REVISION_EDITORIAL: { label: 'Revisión editorial', icon: 'rate_review', tone: 'info' },
  CAMBIOS_SOLICITADOS: { label: 'Cambios solicitados', icon: 'assignment_return', tone: 'warning' },
  EN_REVISION_CLINICA: { label: 'Revisión clínica', icon: 'medical_services', tone: 'info' },
  APROBADO: { label: 'Aprobado', icon: 'verified', tone: 'info' },
  PUBLICADO: { label: 'Publicado', icon: 'public', tone: 'success' },
  REVISION_REQUERIDA: { label: 'Requiere revisión', icon: 'update', tone: 'warning' },
  ARCHIVADO: { label: 'Archivado', icon: 'inventory_2', tone: 'neutral' },
};

export const CONTENT_STATUS_ORDER: EstadoContenido[] = [
  'BORRADOR',
  'EN_EDICION',
  'EN_REVISION_EDITORIAL',
  'CAMBIOS_SOLICITADOS',
  'EN_REVISION_CLINICA',
  'APROBADO',
  'PUBLICADO',
  'REVISION_REQUERIDA',
  'ARCHIVADO',
];

export const ACCOUNT_STATUS: Record<EstadoCuenta, StatusLabel> = {
  ACTIVO: { label: 'Activa', icon: 'check_circle', tone: 'success' },
  BLOQUEADO: { label: 'Bloqueada', icon: 'block', tone: 'danger' },
  PENDIENTE: { label: 'Pendiente de activación', icon: 'schedule_send', tone: 'warning' },
};

export const ROLE_LABEL: Record<Rol, string> = {
  ALUMNO: 'Alumno',
  AUTOR_CONTENIDO: 'Autor de contenido',
  REVISOR_MEDICO: 'Revisor médico',
  EDITOR_CONTENIDO: 'Editor de contenido',
  GESTOR_MULTIMEDIA: 'Gestor multimedia',
  SOPORTE: 'Soporte',
  ADMIN_USUARIOS: 'Administrador de usuarios',
  ADMIN_PLATAFORMA: 'Administrador de plataforma',
  SUPER_ADMIN: 'Superadministrador',
};

/** Fecha local `YYYY-MM-DD` (sin conversión a UTC) para enviar a la API. */
export function toIsoDate(date: Date | null | undefined): string | null {
  if (!date) {
    return null;
  }
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

/** Interpreta `YYYY-MM-DD` como fecha local. */
export function fromIsoDate(value: string | null | undefined): Date | null {
  if (!value) {
    return null;
  }
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}
