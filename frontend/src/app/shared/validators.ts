import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Política mínima de contraseña del MVP (alineada con el backend). */
export const PASSWORD_MIN_LENGTH = 8;
export const PASSWORD_MAX_LENGTH = 72;

/** Error `noBlank` cuando el valor sólo contiene espacios. */
export const noBlank: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value: unknown = control.value;
  return typeof value === 'string' && value.length > 0 && value.trim().length === 0
    ? { noBlank: true }
    : null;
};

/** Validador de grupo: error `passwordMismatch` en el campo de confirmación. */
export function matchFields(field: string, confirmation: string): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const confirm = group.get(confirmation);
    if (!confirm) {
      return null;
    }
    const mismatch = !!confirm.value && group.get(field)?.value !== confirm.value;
    const { passwordMismatch: _previous, ...others } = confirm.errors ?? {};
    const next = mismatch ? { ...others, passwordMismatch: true } : others;
    confirm.setErrors(Object.keys(next).length ? next : null, { emitEvent: false });
    return null;
  };
}
