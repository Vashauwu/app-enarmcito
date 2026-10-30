import { BlockData, TipoBloque } from '../../../../core/api.models';

export interface BlockTypeInfo {
  tipo: TipoBloque;
  label: string;
  icon: string;
  description: string;
}

export const BLOCK_TYPES: BlockTypeInfo[] = [
  { tipo: 'TEXTO_RICO', label: 'Texto', icon: 'notes', description: 'Párrafos, subtítulos y listas.' },
  { tipo: 'TABLA', label: 'Tabla', icon: 'table_chart', description: 'Comparativos, criterios o dosis.' },
  { tipo: 'CALLOUT', label: 'Llamada', icon: 'campaign', description: 'Punto clave, perla, alerta o contraindicación.' },
  { tipo: 'ALGORITMO', label: 'Algoritmo', icon: 'account_tree', description: 'Decisiones paso a paso con ramas.' },
  { tipo: 'IMAGEN', label: 'Imagen', icon: 'image', description: 'Fotografía, figura o infografía.' },
  { tipo: 'DIAGRAMA', label: 'Diagrama', icon: 'schema', description: 'Imagen o SVG con descripción en texto.' },
  { tipo: 'ACORDEON', label: 'Acordeón', icon: 'expand_circle_down', description: 'Detalle plegable.' },
  { tipo: 'REFERENCIAS', label: 'Referencias', icon: 'menu_book', description: 'Bibliografía y enlaces.' },
  { tipo: 'ADJUNTO', label: 'Adjunto PDF', icon: 'attach_file', description: 'Documento descargable autorizado.' },
];

export const BLOCK_LABEL: Record<TipoBloque, BlockTypeInfo> = Object.fromEntries(
  BLOCK_TYPES.map((info) => [info.tipo, info]),
) as Record<TipoBloque, BlockTypeInfo>;

/** Texto corto que identifica un bloque en la lista del editor. */
export function blockSummary(tipo: TipoBloque, datos: BlockData[TipoBloque]): string {
  const d = datos as never as Record<string, unknown>;
  const text = (value: unknown) => String(value ?? '').replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim();
  switch (tipo) {
    case 'TEXTO_RICO':
      return text(d['html']);
    case 'TABLA':
      return `${text(d['titulo']) || 'Tabla'} · ${(d['encabezados'] as string[]).join(' / ')}`;
    case 'CALLOUT':
      return `${text(d['titulo'])} ${text(d['texto'])}`.trim();
    case 'ALGORITMO':
    case 'DIAGRAMA':
    case 'ADJUNTO':
      return text(d['titulo']);
    case 'IMAGEN':
      return text(d['pie']) || 'Imagen';
    case 'ACORDEON':
      return (d['secciones'] as { titulo: string }[]).map((s) => s.titulo).join(' · ');
    case 'REFERENCIAS':
      return `${(d['referencias'] as unknown[]).length} referencias`;
  }
}

/**
 * Convierte el texto del editor en HTML: cada bloque separado por una línea en blanco se envuelve en un párrafo, salvo
 * que ya empiece con una etiqueta de bloque (subtítulo, lista, tabla). Los fragmentos sin etiquetas se escapan; la API
 * vuelve a sanear el resultado.
 */
export function toHtml(text: string): string {
  return text
    .trim()
    .split(/\n{2,}/)
    .map((chunk) => chunk.trim())
    .filter((chunk) => chunk.length > 0)
    .map((chunk) => {
      const hasTags = /<[a-z][\s\S]*>/i.test(chunk);
      if (/^<(h[1-6]|ul|ol|p|table|blockquote)\b/i.test(chunk)) {
        return chunk;
      }
      const body = hasTags ? chunk : chunk.replace(/&/g, '&amp;').replace(/</g, '&lt;');
      return `<p>${body.replace(/\n/g, '<br>')}</p>`;
    })
    .join('\n');
}
