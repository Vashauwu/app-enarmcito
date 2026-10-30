import { BreakpointObserver } from '@angular/cdk/layout';
import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { Router, RouterLink } from '@angular/router';
import { debounceTime, map } from 'rxjs';
import { AdminApi } from '../../../core/admin/admin-api.service';
import { CONTENT_STATUS, CONTENT_STATUS_ORDER, StatusLabel } from '../../../core/admin/admin-labels';
import { EditorialFilters, EditorialRow, EstadoContenido } from '../../../core/admin/admin.models';
import { toApiError } from '../../../core/api-error';
import { AuthService } from '../../../core/auth/auth.service';
import { accesoLabel } from '../../../core/content-labels';
import { ContentService } from '../../../core/content.service';
import { StateMessage } from '../../../shared/state-message/state-message';
import { StatusBadge } from '../../../shared/status-badge/status-badge';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-content-admin-page',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSelectModule,
    MatTableModule,
    StateMessage,
    StatusBadge,
  ],
  templateUrl: './content-admin-page.html',
})
export class ContentAdminPage implements OnInit {
  private readonly api = inject(AdminApi);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);

  /** Filtros iniciales desde la URL. */
  readonly estado = input<EstadoContenido>();
  readonly revisionVencida = input<string>();

  protected readonly pageSize = PAGE_SIZE;
  protected readonly status = CONTENT_STATUS;
  protected readonly statusOrder = CONTENT_STATUS_ORDER;
  protected readonly page = signal(0);
  protected readonly filters = signal<EditorialFilters>({});

  protected readonly form = inject(NonNullableFormBuilder).group({
    q: [''],
    estado: [''],
    area: [''],
    tierId: [null as number | null],
    revisionVencida: [false],
  });

  protected readonly areas = rxResource({ stream: () => this.api.areas() });
  private readonly contentService = inject(ContentService);
  protected readonly tiers = rxResource({ stream: () => this.contentService.tiers() });
  protected readonly contents = rxResource({
    params: () => ({ filters: this.filters(), page: this.page() }),
    stream: ({ params }) => this.api.contents(params.filters, params.page, PAGE_SIZE),
  });
  protected readonly error = computed(() =>
    this.contents.error() ? toApiError(this.contents.error()).message : null,
  );

  private readonly compact = toSignal(
    inject(BreakpointObserver)
      .observe('(max-width: 719px)')
      .pipe(map((state) => state.matches)),
    { initialValue: false },
  );
  protected readonly columns = computed(() =>
    this.compact()
      ? ['titulo', 'estado']
      : ['titulo', 'estado', 'tier', 'responsables', 'revision', 'actualizado'],
  );

  constructor() {
    this.form.valueChanges.pipe(debounceTime(300), takeUntilDestroyed()).subscribe(() => {
      this.page.set(0);
      this.filters.set(this.form.getRawValue() as EditorialFilters);
    });
  }

  ngOnInit(): void {
    this.form.patchValue(
      { estado: this.estado() ?? '', revisionVencida: this.revisionVencida() === 'true' },
      { emitEvent: false },
    );
    this.filters.set(this.form.getRawValue() as EditorialFilters);
  }

  protected statusOf(row: EditorialRow): StatusLabel {
    return CONTENT_STATUS[row.estado];
  }

  protected accesoOf(row: EditorialRow): string {
    return accesoLabel({
      politica: row.politicaAcceso,
      tierCodigo: row.tierMinimo?.codigo ?? null,
      tierNombre: row.tierMinimo?.nombre ?? null,
    }).label;
  }

  protected changePage(event: PageEvent): void {
    this.page.set(event.pageIndex);
  }

  protected open(row: EditorialRow): void {
    void this.router.navigate(['/admin/contenido', row.id, 'editar']);
  }
}
