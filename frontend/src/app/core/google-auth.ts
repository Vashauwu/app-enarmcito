/** Mensajes para los códigos que el backend devuelve en `?google=<código>` al volver de Google. */
const GOOGLE_MESSAGES: Record<string, { text: string; ok: boolean }> = {
  vinculada: { text: 'Google quedó vinculado a tu cuenta.', ok: true },
  GOOGLE_CANCELADO: {
    text: 'Cancelaste el acceso con Google. Puedes intentarlo de nuevo cuando quieras.',
    ok: false,
  },
  GOOGLE_ESTADO_INVALIDO: {
    text: 'El intento de acceso expiró o no se pudo comprobar. Inténtalo de nuevo.',
    ok: false,
  },
  GOOGLE_ERROR: {
    text: 'Google no pudo confirmar tu identidad. Inténtalo de nuevo en unos segundos.',
    ok: false,
  },
  GOOGLE_CORREO_NO_VERIFICADO: {
    text: 'El correo de tu cuenta de Google no está verificado, así que no podemos usarlo.',
    ok: false,
  },
  GOOGLE_CORREO_EXISTENTE: {
    text: 'Ya existe una cuenta con ese correo. Inicia sesión con tu contraseña y vincula Google desde Mi perfil.',
    ok: false,
  },
  GOOGLE_CUENTA_INACTIVA: { text: 'Tu cuenta no está activa. Contacta a soporte.', ok: false },
  GOOGLE_NO_DISPONIBLE: {
    text: 'El acceso con Google no está disponible en este momento.',
    ok: false,
  },
  GOOGLE_YA_VINCULADA: {
    text: 'Esa cuenta de Google ya está vinculada a otra cuenta.',
    ok: false,
  },
  GOOGLE_OTRA_CUENTA: {
    text: 'Tu cuenta ya tiene otra cuenta de Google vinculada. Desvincúlala primero.',
    ok: false,
  },
};

/** Mensaje para un código de retorno de Google; `null` si no hay código o no se reconoce. */
export function googleMessage(code: string | undefined): { text: string; ok: boolean } | null {
  return (code && GOOGLE_MESSAGES[code]) || null;
}
