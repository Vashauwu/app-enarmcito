import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL, PoliticaAcceso, TipoBloque, BlockData, Page } from '../api.models';
import {
  Activity,
  AdminUserDetail,
  AdminUserRow,
  AuditAction,
  AuditEvent,
  AuditFilters,
  Candidates,
  Dashboard,
  EditorialDetail,
  EditorialFilters,
  EditorialRequest,
  EditorialRow,
  EtapaRevision,
  Medio,
  MedioMetadatos,
  InviteRequest,
  Rol,
  UserFilters,
  UserOptions,
  WithAccessLink,
} from './admin.models';

const ADMIN_URL = `${API_URL}/admin`;

/** Convierte un objeto de filtros en parámetros, omitiendo valores vacíos. */
function toParams(values: Record<string, unknown>): HttpParams {
  let params = new HttpParams();
  for (const [key, value] of Object.entries(values)) {
    if (value !== undefined && value !== null && value !== '' && value !== false) {
      params = params.set(key, String(value));
    }
  }
  return params;
}

@Injectable({ providedIn: 'root' })
export class AdminApi {
  private readonly http = inject(HttpClient);

  dashboard(): Observable<Dashboard> {
    return this.http.get<Dashboard>(`${ADMIN_URL}/dashboard`);
  }

  // ----- Usuarios -----

  users(filters: UserFilters, page: number, size: number): Observable<Page<AdminUserRow>> {
    return this.http.get<Page<AdminUserRow>>(`${ADMIN_URL}/usuarios`, {
      params: toParams({ ...filters, page, size }),
    });
  }

  userOptions(): Observable<UserOptions> {
    return this.http.get<UserOptions>(`${ADMIN_URL}/usuarios/opciones`);
  }

  user(id: string): Observable<AdminUserDetail> {
    return this.http.get<AdminUserDetail>(`${ADMIN_URL}/usuarios/${encodeURIComponent(id)}`);
  }

  updateUser(
    id: string,
    data: { nombre: string; apellido: string; tierId: number; motivo: string | null },
  ): Observable<AdminUserDetail> {
    return this.http.put<AdminUserDetail>(`${ADMIN_URL}/usuarios/${encodeURIComponent(id)}`, data);
  }

  blockUser(id: string, motivo: string): Observable<AdminUserDetail> {
    return this.http.post<AdminUserDetail>(`${ADMIN_URL}/usuarios/${encodeURIComponent(id)}/bloquear`, { motivo });
  }

  unblockUser(id: string, motivo: string): Observable<AdminUserDetail> {
    return this.http.post<AdminUserDetail>(`${ADMIN_URL}/usuarios/${encodeURIComponent(id)}/desbloquear`, {
      motivo,
    });
  }

  changeRole(id: string, rol: Rol, motivo: string): Observable<AdminUserDetail> {
    return this.http.put<AdminUserDetail>(`${ADMIN_URL}/usuarios/${encodeURIComponent(id)}/rol`, { rol, motivo });
  }

  invite(data: InviteRequest): Observable<WithAccessLink> {
    return this.http.post<WithAccessLink>(`${ADMIN_URL}/usuarios`, data);
  }

  resetAccess(id: string, motivo: string): Observable<WithAccessLink> {
    return this.http.post<WithAccessLink>(`${ADMIN_URL}/usuarios/${encodeURIComponent(id)}/restablecer-acceso`, {
      motivo,
    });
  }

  activity(
    dias: number,
    filters: { rol?: Rol | ''; tierId?: number | null },
    page: number,
    size: number,
  ): Observable<Activity> {
    return this.http.get<Activity>(`${ADMIN_URL}/actividad/usuarios`, {
      params: toParams({ dias, ...filters, page, size }),
    });
  }

  // ----- Contenido -----

  contents(filters: EditorialFilters, page: number, size: number): Observable<Page<EditorialRow>> {
    return this.http.get<Page<EditorialRow>>(`${ADMIN_URL}/contenido`, {
      params: toParams({ ...filters, page, size }),
    });
  }

  content(id: string): Observable<EditorialDetail> {
    return this.http.get<EditorialDetail>(`${ADMIN_URL}/contenido/${encodeURIComponent(id)}`);
  }

  areas(): Observable<string[]> {
    return this.http.get<string[]>(`${ADMIN_URL}/contenido/areas`);
  }

  candidates(): Observable<Candidates> {
    return this.http.get<Candidates>(`${ADMIN_URL}/contenido/responsables`);
  }

  createContent(data: EditorialRequest): Observable<EditorialDetail> {
    return this.http.post<EditorialDetail>(`${ADMIN_URL}/contenido`, data);
  }

  updateContent(id: string, data: EditorialRequest): Observable<EditorialDetail> {
    return this.http.put<EditorialDetail>(`${ADMIN_URL}/contenido/${encodeURIComponent(id)}`, data);
  }

  assign(id: string, autorId: string | null, revisorId: string | null): Observable<EditorialDetail> {
    return this.http.put<EditorialDetail>(`${ADMIN_URL}/contenido/${encodeURIComponent(id)}/responsables`, {
      autorId,
      revisorId,
    });
  }

  submitForReview(id: string, revisorId: string | null, comentario: string | null): Observable<EditorialDetail> {
    return this.http.post<EditorialDetail>(`${ADMIN_URL}/contenido/${encodeURIComponent(id)}/enviar-revision`, {
      revisorId,
      comentario,
    });
  }

  review(
    id: string,
    etapa: EtapaRevision,
    decision: 'APROBAR' | 'SOLICITAR_CAMBIOS',
    comentario: string | null,
  ): Observable<EditorialDetail> {
    return this.http.post<EditorialDetail>(`${ADMIN_URL}/contenido/${encodeURIComponent(id)}/revisiones`, {
      etapa,
      decision,
      comentario,
    });
  }

  publish(id: string): Observable<EditorialDetail> {
    return this.http.post<EditorialDetail>(`${ADMIN_URL}/contenido/${encodeURIComponent(id)}/publicar`, null);
  }

  archive(id: string, motivo: string): Observable<EditorialDetail> {
    return this.http.post<EditorialDetail>(`${ADMIN_URL}/contenido/${encodeURIComponent(id)}/archivar`, { motivo });
  }

  restore(id: string, motivo: string): Observable<EditorialDetail> {
    return this.http.post<EditorialDetail>(`${ADMIN_URL}/contenido/${encodeURIComponent(id)}/restaurar`, { motivo });
  }

  setAccess(
    id: string,
    politica: PoliticaAcceso,
    tierMinimoId: number | null,
    motivo: string,
  ): Observable<EditorialDetail> {
    return this.http.put<EditorialDetail>(`${ADMIN_URL}/contenido/${encodeURIComponent(id)}/acceso`, {
      politica,
      tierMinimoId,
      motivo,
    });
  }

  // ----- Bloques -----

  addBlock(id: string, tipo: TipoBloque, datos: BlockData[TipoBloque], posicion?: number): Observable<EditorialDetail> {
    return this.http.post<EditorialDetail>(`${ADMIN_URL}/contenido/${encodeURIComponent(id)}/bloques`, {
      tipo,
      datos,
      posicion: posicion ?? null,
    });
  }

  updateBlock(id: string, bloqueId: string, tipo: TipoBloque, datos: BlockData[TipoBloque]): Observable<EditorialDetail> {
    return this.http.put<EditorialDetail>(
      `${ADMIN_URL}/contenido/${encodeURIComponent(id)}/bloques/${encodeURIComponent(bloqueId)}`,
      { tipo, datos },
    );
  }

  removeBlock(id: string, bloqueId: string): Observable<EditorialDetail> {
    return this.http.delete<EditorialDetail>(
      `${ADMIN_URL}/contenido/${encodeURIComponent(id)}/bloques/${encodeURIComponent(bloqueId)}`,
    );
  }

  reorderBlocks(id: string, bloques: string[]): Observable<EditorialDetail> {
    return this.http.put<EditorialDetail>(`${ADMIN_URL}/contenido/${encodeURIComponent(id)}/bloques/orden`, {
      bloques,
    });
  }

  // ----- Medios -----

  media(filters: { estado?: string; tipo?: string; q?: string }, page: number, size: number): Observable<Page<Medio>> {
    return this.http.get<Page<Medio>>(`${ADMIN_URL}/media`, { params: toParams({ ...filters, page, size }) });
  }

  uploadMedia(file: File, metadatos: MedioMetadatos): Observable<Medio> {
    const body = new FormData();
    body.append('archivo', file);
    body.append('metadatos', new Blob([JSON.stringify(metadatos)], { type: 'application/json' }));
    return this.http.post<Medio>(`${ADMIN_URL}/media`, body);
  }

  approveMedia(id: string): Observable<Medio> {
    return this.http.post<Medio>(`${ADMIN_URL}/media/${encodeURIComponent(id)}/aprobar`, null);
  }

  /** URL del archivo original para vista previa administrativa. */
  static mediaFileUrl(id: string): string {
    return `${ADMIN_URL}/media/${encodeURIComponent(id)}/archivo`;
  }

  // ----- Auditoría -----

  audit(filters: AuditFilters, page: number, size: number): Observable<Page<AuditEvent>> {
    return this.http.get<Page<AuditEvent>>(`${ADMIN_URL}/auditoria`, {
      params: toParams({ ...filters, page, size }),
    });
  }

  auditActions(): Observable<AuditAction[]> {
    return this.http.get<AuditAction[]>(`${ADMIN_URL}/auditoria/acciones`);
  }
}
