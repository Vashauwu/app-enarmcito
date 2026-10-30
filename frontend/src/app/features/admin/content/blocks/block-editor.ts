import { Component, computed, inject, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { filter, switchMap } from 'rxjs';
import { AdminApi } from '../../../../core/admin/admin-api.service';
import { AdminBlock, EditorialDetail } from '../../../../core/admin/admin.models';
import { toApiError } from '../../../../core/api-error';
import { BlockData, TipoBloque } from '../../../../core/api.models';
import { confirmAction } from '../../../../shared/confirm-dialog/confirm-dialog';
import { BlockDialog, BlockDialogData } from './block-dialog';
import { BLOCK_LABEL, BLOCK_TYPES, blockSummary } from './block-types';

/** Lista ordenada de bloques de un contenido: agregar, editar, mover y retirar. */
@Component({
  selector: 'app-block-editor',
  imports: [MatButtonModule, MatCardModule, MatIconModule],
  templateUrl: './block-editor.html',
  styleUrl: './block-editor.scss',
})
export class BlockEditor {
  private readonly api = inject(AdminApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly detail = input.required<EditorialDetail>();
  readonly changed = output<EditorialDetail>();

  protected readonly busy = signal(false);
  protected readonly label = BLOCK_LABEL;
  protected readonly types = computed(() =>
    BLOCK_TYPES.filter((t) => t.tipo !== 'ALGORITMO' || this.detail().acciones.crearAlgoritmos),
  );
  protected readonly blocks = computed(() => [...this.detail().bloques].sort((a, b) => a.posicion - b.posicion));

  protected summary(block: AdminBlock): string {
    return blockSummary(block.tipo, block.datos);
  }

  protected pending(block: AdminBlock): boolean {
    return block.medios.some((m) => m.estado !== 'APROBADO');
  }

  protected add(tipo: TipoBloque): void {
    this.open({
      tipo,
      save: (datos) => this.api.addBlock(this.detail().id, tipo, datos),
    });
  }

  protected edit(block: AdminBlock): void {
    this.open({
      tipo: block.tipo,
      datos: block.datos,
      save: (datos: BlockData[TipoBloque]) => this.api.updateBlock(this.detail().id, block.id, block.tipo, datos),
    });
  }

  protected remove(block: AdminBlock): void {
    confirmAction(this.dialog, {
      title: 'Retirar bloque',
      message: `Se retirará «${this.label[block.tipo].label}» del contenido. El bloque deja de mostrarse al publicar.`,
      confirmLabel: 'Retirar',
      tone: 'danger',
    })
      .pipe(
        filter((result) => result !== undefined),
        switchMap(() => {
          this.busy.set(true);
          return this.api.removeBlock(this.detail().id, block.id);
        }),
      )
      .subscribe({ next: (updated) => this.applied(updated), error: (e: unknown) => this.failed(e) });
  }

  protected move(block: AdminBlock, delta: -1 | 1): void {
    const ids = this.blocks().map((b) => b.id);
    const from = ids.indexOf(block.id);
    const to = from + delta;
    if (to < 0 || to >= ids.length) {
      return;
    }
    [ids[from], ids[to]] = [ids[to], ids[from]];
    this.busy.set(true);
    this.api.reorderBlocks(this.detail().id, ids).subscribe({
      next: (updated) => this.applied(updated),
      error: (e: unknown) => this.failed(e),
    });
  }

  private open(data: BlockDialogData): void {
    this.dialog
      .open<BlockDialog, BlockDialogData, EditorialDetail>(BlockDialog, {
        data,
        width: '820px',
        maxWidth: 'calc(100vw - 24px)',
        autoFocus: 'dialog',
      })
      .afterClosed()
      .pipe(filter((updated): updated is EditorialDetail => !!updated))
      .subscribe((updated) => this.applied(updated));
  }

  private applied(updated: EditorialDetail): void {
    this.busy.set(false);
    this.changed.emit(updated);
  }

  private failed(error: unknown): void {
    this.busy.set(false);
    this.snackBar.open(toApiError(error).message, 'Cerrar', { duration: 6000 });
  }
}
