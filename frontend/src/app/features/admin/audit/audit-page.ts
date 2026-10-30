import { DatePipe, KeyValuePipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { debounceTime } from 'rxjs';
import { AdminApi } from '../../../core/admin/admin-api.service';
import { DATE_PROVIDERS, ROLE_LABEL, toIsoDate } from '../../../core/admin/admin-labels';
import { AuditFilters, Rol } from '../../../core/admin/admin.models';
import { AuthService } from '../../../core/auth/auth.service';
import { StateMessage } from '../../../shared/state-message/state-message';

const PAGE_SIZE = 20;

/** Bitácora de sólo lectura con filtros. */
@Component({
  selector: 'app-audit-page',
  imports: [
    DatePipe,
    KeyValuePipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatDatepickerModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSelectModule,
    StateMessage,
  ],
  providers: [DATE_PROVIDERS],
  templateUrl: './audit-page.html',
  styleUrl: './audit-page.scss',
})
export class AuditPage implements OnInit {
  private readonly api = inject(AdminApi);
  protected readonly auth = inject(AuthService);

  /** Filtro inicial desde la URL (por ejemplo, desde la ficha de un usuario). */
  readonly recursoId = input<string>();

  protected readonly pageSize = PAGE_SIZE;
  protected readonly page = signal(0);
  protected readonly filters = signal<AuditFilters>({});

  protected readonly form = inject(NonNullableFormBuilder).group({
    accion: [''],
    recursoTipo: ['' as AuditFilters['recursoTipo']],
    actor: [''],
    desde: [null as Date | null],
    hasta: [null as Date | null],
    recursoId: [''],
  });

  protected readonly actions = rxResource({ stream: () => this.api.auditActions() });
  protected readonly events = rxResource({
    params: () => ({ filters: this.filters(), page: this.page() }),
    stream: ({ params }) => this.api.audit(params.filters, params.page, PAGE_SIZE),
  });

  constructor() {
    this.form.valueChanges.pipe(debounceTime(300), takeUntilDestroyed()).subscribe(() => this.apply());
  }

  ngOnInit(): void {
    this.form.patchValue({ recursoId: this.recursoId() ?? '' }, { emitEvent: false });
    this.apply();
  }

  protected roleLabel(rol: string | undefined): string {
    return rol ? (ROLE_LABEL[rol as Rol] ?? rol) : 'Sistema';
  }

  protected resourceLink(tipo: string | null, id: string | null): string[] | null {
    if (!id) {
      return null;
    }
    if (tipo === 'USUARIO' && this.auth.can('users.read')) {
      return ['/admin/usuarios', id];
    }
    if (tipo === 'CONTENIDO' && this.auth.can('content.read.admin')) {
      return ['/admin/contenido', id, 'editar'];
    }
    return null;
  }

  protected clear(): void {
    this.form.reset();
  }

  protected changePage(event: PageEvent): void {
    this.page.set(event.pageIndex);
  }

  /** Muestra valores de la bitácora como texto legible. */
  protected show(value: unknown): string {
    if (value === null || value === undefined || value === '') {
      return '—';
    }
    return String(value);
  }

  private apply(): void {
    const v = this.form.getRawValue();
    this.page.set(0);
    this.filters.set({
      accion: v.accion,
      recursoTipo: v.recursoTipo,
      actor: v.actor.trim(),
      recursoId: v.recursoId.trim(),
      desde: toIsoDate(v.desde),
      hasta: toIsoDate(v.hasta),
    });
  }
}
