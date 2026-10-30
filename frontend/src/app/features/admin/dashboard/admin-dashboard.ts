import { DatePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { AdminApi } from '../../../core/admin/admin-api.service';
import { CONTENT_STATUS, CONTENT_STATUS_ORDER } from '../../../core/admin/admin-labels';
import { AuthService } from '../../../core/auth/auth.service';
import { StateMessage } from '../../../shared/state-message/state-message';
import { StatusBadge } from '../../../shared/status-badge/status-badge';

@Component({
  selector: 'app-admin-dashboard',
  imports: [DatePipe, RouterLink, MatButtonModule, MatIconModule, MatProgressBarModule, StateMessage, StatusBadge],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.scss',
})
export class AdminDashboard {
  private readonly api = inject(AdminApi);
  protected readonly auth = inject(AuthService);
  protected readonly contentStatus = CONTENT_STATUS;
  protected readonly contentOrder = CONTENT_STATUS_ORDER;

  protected readonly dashboard = rxResource({ stream: () => this.api.dashboard() });
}
