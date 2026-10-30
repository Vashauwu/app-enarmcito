/** Contratos de la API administrativa (`/api/v1/admin`). */
import { BlockData, Page, PoliticaAcceso, Recommendation, Tier, TipoBloque, TipoContenido } from '../api.models';

export type Rol =
  | 'ALUMNO'
  | 'AUTOR_CONTENIDO'
  | 'REVISOR_MEDICO'
  | 'EDITOR_CONTENIDO'
  | 'GESTOR_MULTIMEDIA'
  | 'SOPORTE'
  | 'ADMIN_USUARIOS'
  | 'ADMIN_PLATAFORMA'
  | 'SUPER_ADMIN';

export type EstadoCuenta = 'ACTIVO' | 'BLOQUEADO' | 'PENDIENTE';

export type EstadoContenido =
  | 'BORRADOR'
  | 'EN_EDICION'
  | 'EN_REVISION_EDITORIAL'
  | 'CAMBIOS_SOLICITADOS'
  | 'EN_REVISION_CLINICA'
  | 'APROBADO'
  | 'PUBLICADO'
  | 'REVISION_REQUERIDA'
  | 'ARCHIVADO';

export type EtapaRevision = 'EDITORIAL' | 'CLINICA';

// ---------------------------------------------------------------------------------------------
// Usuarios
// ---------------------------------------------------------------------------------------------

export interface AdminUserRow {
  id: string;
  nombre: string;
  apellido: string;
  correo: string;
  rol: Rol;
  tier: Tier;
  estado: EstadoCuenta;
  createdAt: string;
  lastLoginAt: string | null;
}

export interface AdminUserDetail {
  usuario: AdminUserRow;
  bloqueoMotivo: string | null;
  bloqueadoAt: string | null;
  actividad: { completados: number; enProgreso: number; ultimaConsultaAt: string | null };
  acciones: {
    editarDatos: boolean;
    cambiarTier: boolean;
    cambiarEstado: boolean;
    cambiarRol: boolean;
    restablecerAcceso: boolean;
    rolesAsignables: Rol[];
  };
}

export interface RoleOption {
  codigo: Rol;
  nombre: string;
  critico: boolean;
}

export interface UserOptions {
  roles: RoleOption[];
  rolesCreables: RoleOption[];
  tiers: Tier[];
}

export interface AccessLink {
  url: string;
  tipo: 'INVITACION' | 'RESTABLECIMIENTO';
  expiraAt: string;
}

export interface WithAccessLink {
  usuario: AdminUserDetail;
  enlace: AccessLink;
}

export interface UserFilters {
  q?: string;
  rol?: Rol | '';
  tierId?: number | null;
  estado?: EstadoCuenta | '';
  orden?: 'recientes' | 'nombre' | 'ultimoAcceso';
}

export interface InviteRequest {
  nombre: string;
  apellido: string;
  correo: string;
  rol: Rol;
  tierId: number;
}

// ---------------------------------------------------------------------------------------------
// Contenido editorial
// ---------------------------------------------------------------------------------------------

export interface Persona {
  id: string;
  nombre: string;
}

export interface PersonaConRol extends Persona {
  rol: Rol;
}

export interface EditorialRow {
  id: string;
  tipo: TipoContenido;
  titulo: string;
  slug: string;
  areaMedica: string | null;
  estado: EstadoContenido;
  politicaAcceso: PoliticaAcceso;
  tierMinimo: Tier | null;
  autor: Persona | null;
  revisor: Persona | null;
  fechaRevision: string | null;
  revisionVencida: boolean;
  updatedAt: string;
}

export interface EditorialActions {
  editar: boolean;
  enviarRevision: boolean;
  revisionEditorial: boolean;
  revisionClinica: boolean;
  publicar: boolean;
  archivar: boolean;
  restaurar: boolean;
  asignar: boolean;
  agregarBloques: boolean;
  reordenarBloques: boolean;
  cambiarTipo: boolean;
  cambiarAcceso: boolean;
  crearAlgoritmos: boolean;
  marcarRevisionRequerida: boolean;
}

export interface ReviewEntry {
  id: string;
  etapa: EtapaRevision;
  decision: 'APROBADO' | 'CAMBIOS_SOLICITADOS';
  comentario: string | null;
  revisor: Persona | null;
  createdAt: string;
}

/** Medio asociado a un bloque (vista administrativa). */
export interface AdminBlockMedia {
  id: string;
  tipoMime: string;
  estado: EstadoMedio;
  textoAlternativo: string | null;
}

export interface AdminBlock {
  id: string;
  tipo: TipoBloque;
  posicion: number;
  datos: BlockData[TipoBloque];
  autor: string | null;
  updatedAt: string;
  medios: AdminBlockMedia[];
  editable: boolean;
}

export type EstadoMedio = 'PENDIENTE' | 'APROBADO' | 'RECHAZADO' | 'ARCHIVADO';

export interface Medio {
  id: string;
  nombreOriginal: string;
  tipoMime: string;
  tamanoBytes: number;
  ancho: number | null;
  alto: number | null;
  textoAlternativo: string | null;
  pie: string | null;
  credito: string | null;
  licencia: string | null;
  derechosConfirmados: boolean;
  estado: EstadoMedio;
  motivoRechazo: string | null;
  version: number;
  acciones: { editar: boolean; aprobar: boolean; rechazar: boolean; archivar: boolean };
}

export interface MedioMetadatos {
  textoAlternativo: string | null;
  pie: string | null;
  credito: string | null;
  licencia: string | null;
  derechosConfirmados: boolean;
  sinDatosPaciente: boolean;
  version?: number | null;
}

export interface EditorialDetail {
  id: string;
  tipo: TipoContenido;
  titulo: string;
  slug: string;
  areaMedica: string | null;
  objetivoAprendizaje: string | null;
  resumen: string | null;
  puntosClave: string[];
  aplicacionClinica: string | null;
  metodoAprendizaje: string;
  politicaAcceso: PoliticaAcceso;
  tierMinimo: Tier | null;
  fuenteTitulo: string | null;
  fuenteAutores: string | null;
  fuenteUrl: string | null;
  fuenteDoi: string | null;
  fechaPublicacionFuente: string | null;
  fechaRevision: string | null;
  tiempoLecturaMinutos: number | null;
  revisionClinicaRequerida: boolean;
  revisionClinicaObligatoria: boolean;
  institucionEmisora: string | null;
  fuenteEdicion: string | null;
  poblacionObjetivo: string | null;
  contextoAtencion: string | null;
  alcanceClinico: string | null;
  vigenteHasta: string | null;
  proximaRevision: string | null;
  recomendaciones: Recommendation[];
  procedencia: string | null;
  titularDerechos: string | null;
  licenciaUso: string | null;
  autorizacionPublicacion: boolean;
  autorizacionReferencia: string | null;
  estado: EstadoContenido;
  autor: Persona | null;
  revisor: Persona | null;
  enviadoRevisionAt: string | null;
  aprobadoAt: string | null;
  publicadoAt: string | null;
  archivadoAt: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
  revisionVencida: boolean;
  revisionVenceEl: string | null;
  bloques: AdminBlock[];
  revisiones: ReviewEntry[];
  acciones: EditorialActions;
}

export interface EditorialRequest {
  tipo?: TipoContenido;
  titulo: string;
  slug: string | null;
  areaMedica: string | null;
  objetivoAprendizaje: string | null;
  resumen: string | null;
  puntosClave: string[];
  aplicacionClinica: string | null;
  fuenteTitulo: string | null;
  fuenteAutores: string | null;
  fuenteUrl: string | null;
  fuenteDoi: string | null;
  fechaPublicacionFuente: string | null;
  fechaRevision: string | null;
  tiempoLecturaMinutos: number | null;
  version: number | null;
  revisionClinicaRequerida?: boolean;
  institucionEmisora?: string | null;
  fuenteEdicion?: string | null;
  poblacionObjetivo?: string | null;
  contextoAtencion?: string | null;
  alcanceClinico?: string | null;
  vigenteHasta?: string | null;
  proximaRevision?: string | null;
  recomendaciones?: Recommendation[];
  procedencia?: string | null;
  titularDerechos?: string | null;
  licenciaUso?: string | null;
  autorizacionPublicacion?: boolean;
  autorizacionReferencia?: string | null;
}

export interface EditorialFilters {
  q?: string;
  estado?: EstadoContenido | '';
  area?: string;
  tierId?: number | null;
  revisionVencida?: boolean;
}

export interface Candidates {
  autores: PersonaConRol[];
  revisores: PersonaConRol[];
}

// ---------------------------------------------------------------------------------------------
// Auditoría, tablero y actividad
// ---------------------------------------------------------------------------------------------

export interface AuditEvent {
  id: number;
  ocurridoAt: string;
  actor: { id: string; correo: string; rol: Rol } | null;
  accion: string;
  accionDescripcion: string;
  recursoTipo: 'USUARIO' | 'CONTENIDO' | null;
  recursoId: string | null;
  resumen: string;
  motivo: string | null;
  cambios: Record<string, { antes?: unknown; despues?: unknown; modificado?: boolean; longitud?: number }> | null;
}

export interface AuditFilters {
  accion?: string;
  recursoTipo?: 'USUARIO' | 'CONTENIDO' | '';
  recursoId?: string;
  actor?: string;
  desde?: string | null;
  hasta?: string | null;
}

export interface AuditAction {
  codigo: string;
  descripcion: string;
}

export interface Dashboard {
  rol: Rol;
  rolNombre: string;
  usuarios: {
    periodoDias: number;
    activos: number;
    nuevos: number;
    bloqueados: number;
    pendientes: number;
  } | null;
  contenido: {
    porEstado: Record<EstadoContenido, number>;
    pendientesRevision: number;
    revisionVencida: number;
    revisionPorVencer: number;
  } | null;
  actividadReciente: AuditEvent[];
}

export interface Activity {
  periodoDias: number;
  desde: string;
  totalActivos: number;
  porRol: Record<string, number> | null;
  porTier: Record<string, number>;
  porEstado: Record<string, number>;
  usuarios: Page<AdminUserRow>;
}
