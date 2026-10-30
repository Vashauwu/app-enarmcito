import { BreakpointObserver } from '@angular/cdk/layout';
import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { Router } from '@angular/router';
import { debounceTime, map } from 'rxjs';
import { AdminApi } from '../../../core/admin/admin-api.service';
import { ACCOUNT_STATUS, ROLE_LABEL, StatusLabel } from '../../../core/admin/admin-labels';
import { AdminUserRow, EstadoCuenta, UserFilters, WithAccessLink } from '../../../core/admin/admin.models';
import { toApiError } from '../../../core/api-error';
import { AuthService } from '../../../core/auth/auth.service';
import { AccessLinkDialog } from '../../../shared/access-link-dialog/access-link-dialog';
import { StateMessage } from '../../../shared/state-message/state-message';
import { StatusBadge } from '../../../shared/status-badge/status-badge';
import { InviteDialog } from './invite-dialog';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-users-page',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
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
  templateUrl: './users-page.html',
})
export class UsersPage implements OnInit {
  private readonly api = inject(AdminApi);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  protected readonly auth = inject(AuthService);

  /** Filtros iniciales desde la URL (por ejemplo, desde el tablero). */
  readonly estado = input<EstadoCuenta>();
  readonly orden = input<UserFilters['orden']>();

  protected readonly pageSize = PAGE_SIZE;
  protected readonly page = signal(0);
  protected readonly filters = signal<UserFilters>({});

  protected readonly form = inject(NonNullableFormBuilder).group({
    q: [''],
    rol: [''],
    tierId: [null as number | null],
    estado: [''],
    orden: ['recientes'],
  });

  protected readonly options = rxResource({ stream: () => this.api.userOptions() });
  protected readonly users = rxResource({
    params: () => ({ filters: this.filters(), page: this.page() }),
    stream: ({ params }) => this.api.users(params.filters, params.page, PAGE_SIZE),
  });
  protected readonly error = computed(() => (this.users.error() ? toApiError(this.users.error()).message : null));

  private readonly compact = toSignal(
    inject(BreakpointObserver)
      .observe('(max-width: 719px)')
      .pipe(map((state) => state.matches)),
    { initialValue: false },
  );
  protected readonly columns = computed(() =>
    this.compact() ? ['persona', 'estado'] : ['persona', 'rol', 'tier', 'estado', 'alta', 'acceso'],
  );

  constructor() {
    this.form.valueChanges.pipe(debounceTime(300), takeUntilDestroyed()).subscribe((value) => {
      this.page.set(0);
      this.filters.set(value as UserFilters);
    });
  }

  ngOnInit(): void {
    this.form.patchValue(
      { estado: this.estado() ?? '', orden: this.orden() ?? 'recientes' },
      { emitEvent: false },
    );
    this.filters.set(this.form.getRawValue() as UserFilters);
  }

  protected roleOf(row: AdminUserRow): string {
    return ROLE_LABEL[row.rol];
  }

  protected statusOf(row: AdminUserRow): StatusLabel {
    return ACCOUNT_STATUS[row.estado];
  }

  protected changePage(event: PageEvent): void {
    this.page.set(event.pageIndex);
  }

  protected open(row: AdminUserRow): void {
    void this.router.navigate(['/admin/usuarios', row.id]);
  }

  protected invite(): void {
    const options = this.options.value();
    if (!options) {
      return;
    }
    this.dialog
      .open<InviteDialog, unknown, WithAccessLink>(InviteDialog, {
        data: options,
        width: '560px',
        maxWidth: 'calc(100vw - 32px)',
      })
      .afterClosed()
      .subscribe((result) => {
        if (!result) {
          return;
        }
        this.users.reload();
        this.dialog
          .open(AccessLinkDialog, {
            data: { enlace: result.enlace, nombre: result.usuario.usuario.nombre },
            width: '560px',
            maxWidth: 'calc(100vw - 32px)',
          })
          .afterClosed()
          .subscribe(() => void this.router.navigate(['/admin/usuarios', result.usuario.usuario.id]));
      });
  }
}
