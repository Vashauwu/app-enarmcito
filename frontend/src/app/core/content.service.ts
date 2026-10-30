import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  API_URL,
  ContentReading,
  ContentSummary,
  Learning,
  Page,
  Progress,
  Tier,
  TipoContenido,
} from './api.models';

export const SEARCH_MAX_LENGTH = 100;

/** Catálogo público: funciona con o sin sesión; la API aplica la política de acceso de cada contenido. */
@Injectable({ providedIn: 'root' })
export class ContentService {
  private readonly http = inject(HttpClient);

  tiers(): Observable<Tier[]> {
    return this.http.get<Tier[]>(`${API_URL}/tiers`);
  }

  search(query: string, page: number, size: number, tipo?: TipoContenido | null): Observable<Page<ContentSummary>> {
    let params = new HttpParams().set('page', page).set('size', size);
    const q = query.trim();
    if (q) {
      params = params.set('q', q);
    }
    if (tipo) {
      params = params.set('tipo', tipo);
    }
    return this.http.get<Page<ContentSummary>>(`${API_URL}/content`, { params });
  }

  /** Abre la versión publicada; con sesión, la API la registra como última consultada. */
  open(slug: string): Observable<ContentReading> {
    return this.http.get<ContentReading>(`${API_URL}/content/${encodeURIComponent(slug)}`);
  }

  learning(): Observable<Learning> {
    return this.http.get<Learning>(`${API_URL}/me/learning`);
  }

  complete(contenidoId: string): Observable<Progress> {
    return this.http.put<Progress>(
      `${API_URL}/me/learning/${encodeURIComponent(contenidoId)}/complete`,
      null,
    );
  }

  /** URL pública de un medio aprobado; la API decide si el lector puede verlo. */
  static mediaUrl(id: string, variante: 'movil' | 'escritorio' = 'escritorio'): string {
    return `${API_URL}/media/${encodeURIComponent(id)}?variante=${variante}`;
  }
}
