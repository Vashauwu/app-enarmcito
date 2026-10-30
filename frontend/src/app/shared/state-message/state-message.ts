import { Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/** Mensaje para estados vacíos, de error o de acceso restringido. Las acciones se proyectan. */
@Component({
  selector: 'app-state-message',
  imports: [MatIconModule],
  template: `
    <div class="state" [class.state--error]="tone() === 'error'" [attr.role]="tone() === 'error' ? 'alert' : null">
      <mat-icon class="state__icon" aria-hidden="true">{{ icon() }}</mat-icon>
      <h3 class="state__title">{{ title() }}</h3>
      @if (message()) {
        <p class="state__message">{{ message() }}</p>
      }
      <div class="state__actions"><ng-content /></div>
    </div>
  `,
  styles: `
    .state {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      padding: 32px 16px;
      text-align: center;
      border-radius: var(--mat-sys-corner-large);
      background: var(--mat-sys-surface-container-low);
    }
    .state__icon {
      width: 40px;
      height: 40px;
      font-size: 40px;
      color: var(--mat-sys-primary);
    }
    .state--error .state__icon {
      color: var(--mat-sys-error);
    }
    .state__title {
      margin: 0;
      font: var(--mat-sys-title-medium);
    }
    .state__message {
      margin: 0;
      max-width: 48ch;
      color: var(--mat-sys-on-surface-variant);
    }
    .state__actions:not(:empty) {
      margin-top: 8px;
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      justify-content: center;
    }
  `,
})
export class StateMessage {
  readonly icon = input('info');
  readonly title = input.required<string>();
  readonly message = input<string>();
  readonly tone = input<'neutral' | 'error'>('neutral');
}
