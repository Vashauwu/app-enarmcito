import { Acceso, TipoContenido } from './api.models';

export const TIPO_CONTENIDO: Record<TipoContenido, { label: string; corto: string; icon: string }> = {
  ARTICULO: { label: 'Artículo', corto: 'Artículo', icon: 'article' },
  GUIA_PRACTICA_CLINICA: { label: 'Guía de práctica clínica', corto: 'GPC', icon: 'clinical_notes' },
  RECURSO_ESTUDIO: { label: 'Material de estudio', corto: 'Estudio', icon: 'school' },
};

/** Etiqueta de acceso visible para el lector. */
export function accesoLabel(acceso: Acceso): { label: string; icon: string } {
  switch (acceso.politica) {
    case 'REGISTRADO_GRATUITO':
      return { label: 'Gratis con cuenta', icon: 'person' };
    case 'POR_TIER':
      return { label: `Tier ${acceso.tierNombre ?? ''}`.trim(), icon: 'workspace_premium' };
  }
}
