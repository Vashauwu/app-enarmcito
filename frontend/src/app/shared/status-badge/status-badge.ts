import { Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { StatusLabel } from '../../core/admin/admin-labels';

/** Estado con ícono y texto: nunca depende sólo del color. */
@Component({
  selector: 'app-status-badge',
  imports: [MatIconModule],
  template: `
    <span class="badge" [attr.data-tone]="status().tone">
      <mat-icon aria-hidden="true">{{ status().icon }}</mat-icon>
      {{ status().label }}
    </span>
  `,
  styles: `
    .badge {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 2px 10px 2px 6px;
      border-radius: var(--mat-sys-corner-full);
      font: var(--mat-sys-label-medium);
      white-space: nowrap;
      background: var(--mat-sys-surface-container-highest);
      color: var(--mat-sys-on-surface);
    }
    mat-icon {
      width: 16px;
      height: 16px;
      font-size: 16px;
    }
    [data-tone='info'] {
      background: var(--mat-sys-secondary-container);
      color: var(--mat-sys-on-secondary-container);
    }
    [data-tone='success'] {
      background: var(--mat-sys-primary-container);
      color: var(--mat-sys-on-primary-container);
    }
    [data-tone='warning'] {
      background: var(--mat-sys-tertiary-container);
      color: var(--mat-sys-on-tertiary-container);
    }
    [data-tone='danger'] {
      background: var(--mat-sys-error-container);
      color: var(--mat-sys-on-error-container);
    }
  `,
})
export class StatusBadge {
  readonly status = input.required<StatusLabel>();
}
