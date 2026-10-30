import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ConfirmDialog, ConfirmOptions } from './confirm-dialog';

describe('ConfirmDialog', () => {
  let close: ReturnType<typeof vi.fn>;

  async function render(options: ConfirmOptions) {
    close = vi.fn();
    TestBed.configureTestingModule({
      imports: [ConfirmDialog],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: options },
        { provide: MatDialogRef, useValue: { close } },
      ],
    });
    const fixture = TestBed.createComponent(ConfirmDialog);
    await fixture.whenStable();
    return fixture;
  }

  function confirmButton(element: HTMLElement): HTMLButtonElement {
    return Array.from(element.querySelectorAll('button')).find((b) => b.textContent?.includes('Bloquear'))!;
  }

  it('exige el motivo en las acciones sensibles', async () => {
    const fixture = await render({
      title: 'Bloquear cuenta',
      message: 'La persona no podrá entrar.',
      confirmLabel: 'Bloquear',
      tone: 'danger',
      reason: { label: 'Motivo', required: true },
    });
    const element = fixture.nativeElement as HTMLElement;

    const textarea = element.querySelector('textarea')!;
    textarea.value = '   ';
    textarea.dispatchEvent(new Event('input'));
    confirmButton(element).click();
    await fixture.whenStable();

    expect(close).not.toHaveBeenCalled();
    expect(element.textContent).toContain('Este campo es obligatorio.');

    textarea.value = '  Uso indebido  ';
    textarea.dispatchEvent(new Event('input'));
    confirmButton(element).click();

    expect(close).toHaveBeenCalledWith('Uso indebido');
  });
});
