/** Contratos de la API REST (`/api/v1`). Reflejan los DTO del backend. */

export const API_URL = '/api/v1';

export interface Tier {
  id: number;
  codigo: string;
  nombre: string;
  descripcion: string | null;
  ordenAcceso: number;
}

export interface User {
  id: string;
  nombre: string;
  apellido: string;
  correo: string;
  rol: string;
  rolNombre: string;
  /** Permisos atómicos del rol (`admin.access`, `users.read`…). La API los vuelve a verificar. */
  permisos: string[];
  tier: Tier;
  createdAt: string;
}

export interface RegisterRequest {
  nombre: string;
  apellido: string;
  correo: string;
  password: string;
  tierId: number;
}

export interface LoginRequest {
  correo: string;
  password: string;
}

export interface UpdateProfileRequest {
  nombre: string;
  apellido: string;
}

export type TipoContenido = 'ARTICULO' | 'GUIA_PRACTICA_CLINICA' | 'RECURSO_ESTUDIO';

/** Todo el catálogo exige cuenta: artículos gratis con cuenta; guías y recursos por tier. */
export type PoliticaAcceso = 'REGISTRADO_GRATUITO' | 'POR_TIER';

/** Política de acceso de un contenido tal como se muestra al lector. */
export interface Acceso {
  politica: PoliticaAcceso;
  tierCodigo: string | null;
  tierNombre: string | null;
}

/** Tarjeta del catálogo público: sólo un extracto. */
export interface ContentSummary {
  id: string;
  slug: string;
  tipo: TipoContenido;
  titulo: string;
  areaMedica: string | null;
  extracto: string | null;
  tiempoLecturaMinutos: number | null;
  acceso: Acceso;
  proximaRevision: string | null;
  requiereActualizacion: boolean;
}

export type ProgressState = 'EN_PROGRESO' | 'COMPLETADO';

export interface Progress {
  contenidoId: string;
  estado: ProgressState;
  ultimaConsultaAt: string;
  completadoAt: string | null;
}

export interface Source {
  titulo: string | null;
  autores: string | null;
  url: string | null;
  doi: string | null;
  fechaPublicacion: string | null;
  institucion: string | null;
  edicion: string | null;
}

export interface Recommendation {
  texto: string;
  fuerza: string | null;
  nivelEvidencia: string | null;
  referencia: string | null;
}

export type TipoBloque =
  | 'TEXTO_RICO'
  | 'IMAGEN'
  | 'DIAGRAMA'
  | 'TABLA'
  | 'CALLOUT'
  | 'ALGORITMO'
  | 'ACORDEON'
  | 'REFERENCIAS'
  | 'ADJUNTO';

export interface BlockMedia {
  id: string;
  tipoMime: string;
  textoAlternativo: string | null;
  pie: string | null;
  credito: string | null;
  licencia: string | null;
  ancho: number | null;
  alto: number | null;
  variantes: boolean;
}

/** Datos de cada tipo de bloque (validados y saneados por la API). */
export interface BlockData {
  TEXTO_RICO: { html: string };
  IMAGEN: { activoId: string; pie: string | null };
  DIAGRAMA: { activoId: string; titulo: string; descripcionTextual: string };
  TABLA: { titulo: string | null; encabezados: string[]; filas: string[][]; fuente: string | null; nota: string | null };
  CALLOUT: {
    variante: 'INFORMACION' | 'PERLA_CLINICA' | 'ALERTA' | 'CONTRAINDICACION' | 'PUNTO_CLAVE';
    titulo: string | null;
    texto: string;
  };
  ALGORITMO: {
    titulo: string;
    version: string;
    fuente: string;
    descripcionTextual: string;
    nodos: { id: string; tipo: 'INICIO' | 'DECISION' | 'ACCION' | 'FIN'; texto: string }[];
    conexiones: { desde: string; hacia: string; etiqueta: string | null }[];
  };
  ACORDEON: { secciones: { titulo: string; html: string }[] };
  REFERENCIAS: {
    referencias: {
      titulo: string;
      autores: string | null;
      emisor: string | null;
      url: string | null;
      doi: string | null;
      fecha: string | null;
      etiquetaEvidencia: string | null;
    }[];
  };
  ADJUNTO: { activoId: string; titulo: string; descripcion: string | null };
}

export interface PublicBlock {
  id: string;
  tipo: TipoBloque;
  datos: BlockData[TipoBloque];
  medios: BlockMedia[];
}

/** Versión publicada de un contenido. */
export interface PublicContent {
  id: string;
  slug: string;
  tipo: TipoContenido;
  titulo: string;
  areaMedica: string | null;
  version: number;
  publicadoAt: string;
  fechaRevision: string | null;
  proximaRevision: string | null;
  tiempoLecturaMinutos: number | null;
  objetivoAprendizaje: string | null;
  resumen: string | null;
  puntosClave: string[];
  aplicacionClinica: string | null;
  fuente: Source;
  guia: {
    poblacionObjetivo: string | null;
    contextoAtencion: string | null;
    alcanceClinico: string | null;
    vigenteHasta: string | null;
    recomendaciones: Recommendation[];
  } | null;
  recurso: { procedencia: string | null; titularDerechos: string | null; licenciaUso: string | null } | null;
  bloques: PublicBlock[];
  responsables: { autor: string | null; revisorClinico: string | null; editor: string | null };
}

export interface ContentReading {
  contenido: PublicContent;
  acceso: Acceso;
  requiereActualizacion: boolean;
  progreso: Progress | null;
}

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface Learning {
  ultimo: { contenido: ContentSummary; progreso: Progress } | null;
  completados: number;
  enProgreso: number;
}

export interface ApiFieldError {
  field: string;
  message: string;
}

export interface ApiError {
  status: number;
  code: string;
  message: string;
  fieldErrors?: ApiFieldError[];
}

/** Proveedores de inicio de sesión configurados en el ambiente. */
export interface AuthProviders {
  google: boolean;
}

/** Métodos de acceso de la cuenta. Nunca incluye tokens ni identificadores del proveedor. */
export interface Identities {
  googleDisponible: boolean;
  googleVinculado: boolean;
  googleCorreo: string | null;
  tienePassword: boolean;
}
