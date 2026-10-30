import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Router } from '@angular/router';
import { AdminApi } from '../../../core/admin/admin-api.service';
import { ACCOUNT_STATUS, ROLE_LABEL } from '../../../core/admin/admin-labels';
import { AdminUserRow, EstadoCuenta, Rol } from '../../../core/admin/admin.models';
import { StateMessage } from '../../../shared/state-message/state-message';
import { StatusBadge } from '../../../shared/status-badge/status-badge';

const PAGE_SIZE = 20;

interface Bar {
  label: string;
  value: number;
  percent: number;
}

/** Barras horizontales accesibles: el valor siempre aparece como texto. */
function toBars(values: Record<string, number> | null | undefined, label: (key: string) => string): Bar[] {
  if (!values) {
    return [];
  }
  const max = Math.max(1, ...Object.values(values));
  return Object.entries(values).map(([key, value]) => ({
    label: label(key),
    value,
    percent: Math.round((value / max) * 100),
  }));
}

@Component({
  selector: 'app-activity-page',
  imports: [
    DatePipe,
    MatButtonModule,
    MatButtonToggleModule,
    MatIconModule,
    MatPaginatorModule,
    MatProgressBarModule,
    StateMessage,
    StatusBadge,
  ],
  templateUrl: './activity-page.html',
  styleUrl: './activity-page.scss',
})
export class ActivityPage {
  private readonly api = inject(AdminApi);
  private readonly router = inject(Router);

  protected readonly pageSize = PAGE_SIZE;
  protected readonly accountStatus = ACCOUNT_STATUS;
  protected readonly days = signal(30);
  protected readonly page = signal(0);

  protected readonly activity = rxResource({
    params: () => ({ days: this.days(), page: this.page() }),
    stream: ({ params }) => this.api.activity(params.days, {}, params.page, PAGE_SIZE),
  });

  protected readonly byRole = computed(() =>
    toBars(this.activity.value()?.porRol, (key) => ROLE_LABEL[key as Rol] ?? key),
  );
  protected readonly byTier = computed(() => toBars(this.activity.value()?.porTier, (key) => key));
  protected readonly byStatus = computed(() =>
    toBars(this.activity.value()?.porEstado, (key) => ACCOUNT_STATUS[key as EstadoCuenta]?.label ?? key),
  );

  protected setDays(days: number): void {
    this.page.set(0);
    this.days.set(days);
  }

  protected changePage(event: PageEvent): void {
    this.page.set(event.pageIndex);
  }

  protected open(row: AdminUserRow): void {
    void this.router.navigate(['/admin/usuarios', row.id]);
  }
}
