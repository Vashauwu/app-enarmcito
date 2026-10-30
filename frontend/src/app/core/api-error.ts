import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormGroup } from '@angular/forms';
import { ApiError } from './api.models';

const GENERIC_MESSAGE = 'Ocurrió un error inesperado. Inténtalo de nuevo.';

/** Convierte cualquier error de HttpClient al formato estable `ApiError`. */
export function toApiError(error: unknown): ApiError {
  if (error instanceof HttpErrorResponse) {
    const body: unknown = error.error;
    if (isApiErrorBody(body)) {
      return { ...body, status: error.status };
    }
    if (error.status === 0) {
      return {
        status: 0,
        code: 'SIN_CONEXION',
        message: 'No pudimos conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.',
      };
    }
    return { status: error.status, code: 'ERROR', message: GENERIC_MESSAGE };
  }
  return { status: -1, code: 'ERROR', message: GENERIC_MESSAGE };
}

/**
 * Marca en el formulario los errores por campo que devolvió la API (`fieldErrors`).
 * Devuelve `true` si al menos un campo recibió un error.
 */
export function applyFieldErrors(form: FormGroup, error: ApiError): boolean {
  let applied = false;
  for (const fieldError of error.fieldErrors ?? []) {
    const control: AbstractControl | null = form.get(fieldError.field);
    if (control) {
      control.setErrors({ ...control.errors, server: fieldError.message });
      control.markAsTouched();
      applied = true;
    }
  }
  return applied;
}

function isApiErrorBody(body: unknown): body is Omit<ApiError, 'status'> {
  return (
    typeof body === 'object' &&
    body !== null &&
    typeof (body as Record<string, unknown>)['code'] === 'string' &&
    typeof (body as Record<string, unknown>)['message'] === 'string'
  );
}
