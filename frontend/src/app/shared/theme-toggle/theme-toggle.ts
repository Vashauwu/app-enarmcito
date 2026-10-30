import { Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { ThemePreference, ThemeService } from '../../core/theme.service';

const OPTIONS: { value: ThemePreference; label: string; icon: string }[] = [
  { value: 'light', label: 'Claro', icon: 'light_mode' },
  { value: 'dark', label: 'Oscuro', icon: 'dark_mode' },
  { value: 'system', label: 'Según el sistema', icon: 'brightness_auto' },
];

@Component({
  selector: 'app-theme-toggle',
  imports: [MatButtonModule, MatIconModule, MatMenuModule],
  template: `
    <button matIconButton type="button" [matMenuTriggerFor]="menu" [attr.aria-label]="'Tema: ' + current().label + '. Cambiar tema'">
      <mat-icon>{{ current().icon }}</mat-icon>
    </button>
    <mat-menu #menu="matMenu" xPosition="before">
      @for (option of options; track option.value) {
        <button
          mat-menu-item
          type="button"
          role="menuitemradio"
          [attr.aria-checked]="option.value === theme.preference()"
          (click)="theme.set(option.value)"
        >
          <mat-icon>{{ option.icon }}</mat-icon>
          <span class="label">{{ option.label }}</span>
          @if (option.value === theme.preference()) {
            <mat-icon class="check" aria-hidden="true">check</mat-icon>
          }
        </button>
      }
    </mat-menu>
  `,
  styles: `
    .label {
      flex: 1;
      order: 1;
    }
    .check {
      order: 2;
      margin: 0 0 0 16px;
      color: var(--mat-sys-primary);
    }
  `,
})
export class ThemeToggle {
  protected readonly theme = inject(ThemeService);
  protected readonly options = OPTIONS;
  protected readonly current = computed(
    () => OPTIONS.find((option) => option.value === this.theme.preference()) ?? OPTIONS[2],
  );
}
