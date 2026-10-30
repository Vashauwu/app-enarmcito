import { Component, computed, input } from '@angular/core';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { BlockData, BlockMedia, PublicBlock } from '../../core/api.models';
import { ContentService } from '../../core/content.service';

const CALLOUT: Record<BlockData['CALLOUT']['variante'], { icon: string; label: string }> = {
  INFORMACION: { icon: 'info', label: 'Información' },
  PERLA_CLINICA: { icon: 'lightbulb', label: 'Perla clínica' },
  ALERTA: { icon: 'warning', label: 'Alerta' },
  CONTRAINDICACION: { icon: 'block', label: 'Contraindicación' },
  PUNTO_CLAVE: { icon: 'star', label: 'Punto clave' },
};

const NODO: Record<BlockData['ALGORITMO']['nodos'][number]['tipo'], string> = {
  INICIO: 'Inicio',
  DECISION: 'Decisión',
  ACCION: 'Acción',
  FIN: 'Fin',
};

interface Paso {
  numero: number;
  id: string;
  tipo: BlockData['ALGORITMO']['nodos'][number]['tipo'];
  texto: string;
  salidas: { etiqueta: string | null; numero: number; texto: string }[];
}

/**
 * Renderiza un bloque publicado con un componente específico por tipo. El HTML ya llega saneado por la API y Angular
 * lo vuelve a sanear al insertarlo; el resto son textos planos interpolados.
 */
@Component({
  selector: 'app-block-renderer',
  imports: [MatExpansionModule, MatIconModule],
  templateUrl: './block-renderer.html',
  styleUrl: './block-renderer.scss',
})
export class BlockRenderer {
  readonly block = input.required<PublicBlock>();

  protected readonly callout: Record<string, { icon: string; label: string }> = CALLOUT;
  protected readonly nodo = NODO;
  protected readonly datos = computed(() => this.block().datos as never);
  protected readonly medio = computed<BlockMedia | null>(() => this.block().medios[0] ?? null);

  /** Pasos del algoritmo en orden de recorrido desde el inicio, con sus salidas numeradas. */
  protected readonly pasos = computed<Paso[]>(() => {
    if (this.block().tipo !== 'ALGORITMO') {
      return [];
    }
    const algoritmo = this.block().datos as BlockData['ALGORITMO'];
    const orden: string[] = [];
    const inicio = algoritmo.nodos.find((n) => n.tipo === 'INICIO');
    const cola = inicio ? [inicio.id] : [];
    while (cola.length) {
      const actual = cola.shift()!;
      if (!orden.includes(actual)) {
        orden.push(actual);
        algoritmo.conexiones.filter((c) => c.desde === actual).forEach((c) => cola.push(c.hacia));
      }
    }
    algoritmo.nodos.filter((n) => !orden.includes(n.id)).forEach((n) => orden.push(n.id));
    const numero = (id: string) => orden.indexOf(id) + 1;
    return orden.map((id) => {
      const n = algoritmo.nodos.find((x) => x.id === id)!;
      return {
        numero: numero(id),
        id,
        tipo: n.tipo,
        texto: n.texto,
        salidas: algoritmo.conexiones
          .filter((c) => c.desde === id)
          .map((c) => ({
            etiqueta: c.etiqueta,
            numero: numero(c.hacia),
            texto: algoritmo.nodos.find((x) => x.id === c.hacia)?.texto ?? '',
          })),
      };
    });
  });

  protected url(id: string, variante: 'movil' | 'escritorio' = 'escritorio'): string {
    return ContentService.mediaUrl(id, variante);
  }

  protected srcset(medio: BlockMedia): string | null {
    return medio.variantes ? `${this.url(medio.id, 'movil')} 640w, ${this.url(medio.id, 'escritorio')} 1280w` : null;
  }
}
