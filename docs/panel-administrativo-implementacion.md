# Panel administrativo — decisiones de implementación

Complementa [panel-administrativo.md](panel-administrativo.md). Registra cómo se resolvieron las decisiones
abiertas de la sección 13 y los puntos que la especificación dejaba a la implementación.

## Decisiones de la sección 13

Se adoptaron las propuestas del documento. **Deben confirmarse con el responsable funcional.**

| Pregunta | Decisión implementada | Dónde cambiarla |
|---|---|---|
| Primeras cuentas `SUPER_ADMIN` | Desarrollo: `superadmin@demo.local` (datos demo). Otros entornos: variables `APP_BOOTSTRAP_SUPERADMIN_EMAIL` / `_PASSWORD`, sólo si no hay ninguno activo. La custodia de credenciales sigue pendiente. | `SuperAdminBootstrap` |
| ¿El revisor puede aprobar su propio contenido? | **No.** Separación obligatoria, también para editores y administradores; la base de datos impide además que autor y revisor sean la misma persona. | `EditorialPolicy.puedeRevisar`, `ck_contenido_autor_revisor` |
| Periodo de «usuario activo» | 30 días (configurable); la pantalla de actividad permite 7, 30 o 90. | `APP_ADMIN_ACTIVE_USER_DAYS` |
| Progreso sobre contenido archivado | El alumno pierde el acceso (404, igual que un borrador) y deja de verlo en «Continuar aprendizaje». Su registro de progreso se conserva. | `ContentAccessPolicy` |
| Áreas médicas y fuentes aprobadas | Las áreas siguen como texto libre con autocompletado de las existentes; no hay catálogo de fuentes aprobadas. | — |
| Invitación y restablecimiento desde la primera versión | **Sí**, con enlaces de un solo uso que se muestran una vez a quien los genera (no hay servidor de correo). | `AccessLinkService` |

## Roles y permisos

- Un rol por cuenta. Los permisos se derivan del rol en `Rol.java` y se exponen a la interfaz en `/auth/me`
  (`permisos`), sólo para decidir qué mostrar.
- `ADMIN` del MVP se migró a `ADMIN_PLATAFORMA` (migración V3).
- Permisos añadidos a la lista orientativa del documento, para expresar la matriz con exactitud:
  `users.read.staff` (ver cuentas del personal), `users.create`, `users.access.reset`, `content.assign`,
  `content.archive.own` y `audit.read.editorial`.
- Jerarquía de gestión (editar, bloquear o cambiar el rol de una cuenta, y roles que se pueden asignar):

| Rol | Gestiona cuentas con rol | Puede crear cuentas con rol |
|---|---|---|
| `SUPER_ADMIN` | Todos | Todos |
| `ADMIN_PLATAFORMA` | Todos salvo `SUPER_ADMIN` | No críticos |
| `ADMIN_USUARIOS` | No críticos | No críticos |
| `SOPORTE` | `ALUMNO` (sólo estado y restablecimiento de acceso) | — |

  Roles críticos: `ADMIN_USUARIOS`, `ADMIN_PLATAFORMA`, `SUPER_ADMIN`.
- Nadie cambia su propio rol ni su propio estado. Para quitar o bloquear un `SUPER_ADMIN` se bloquean las filas de
  los `SUPER_ADMIN` activos (bloqueo pesimista) y se exige que quede al menos otro activo.
- **Autorización por petición:** un filtro relee estado y rol de la cuenta en cada petición autenticada. Una
  cuenta bloqueada pierde la sesión en su siguiente petición; un cambio de rol surte efecto sin volver a iniciar sesión.
- Sin `users.read.staff` (soporte) sólo se ven cuentas `ALUMNO`; una cuenta fuera de alcance responde 404.

## Flujo editorial

- Transiciones mediante endpoints explícitos; el estado nunca se edita libremente. Una transición inválida para el
  estado actual responde **409 `TRANSICION_INVALIDA`**; una permitida por estado pero no por rol o asignación, **403**.
- Un borrador puede guardarse incompleto. Desde `EN_REVISION` los campos obligatorios se exigen en la API y en la
  base de datos (`ck_contenido_completo`).
- Publicar exige contenido completo y fecha de revisión vigente (24 meses, configurable). El tablero avisa de las
  revisiones vencidas y de las que vencen en 60 días.
- Quién edita: el autor sólo en `BORRADOR` y `CAMBIOS_SOLICITADOS`; los editores en cualquier estado salvo `ARCHIVADO`
  (correcciones auditadas con diferencias). El revisor no edita el texto: aprueba o solicita cambios con comentario.
- No se puede cambiar el slug de un artículo publicado.
- Restaurar un archivado lo devuelve a `BORRADOR` y exige una nueva revisión antes de publicarlo.
- Bloqueo optimista (`version`): si dos personas editan a la vez, la segunda recibe 409 `CONFLICTO_EDICION`.
- El contenido publicado antes de V3 recibió una revisión `APROBADO` de migración, para cumplir la regla de
  «mantener una revisión aprobada».

## Auditoría

- Se auditan: sesiones administrativas (inicio, cierre e intentos fallidos sobre cuentas con acceso al panel),
  invitaciones, cambios de datos, tier, rol y estado, enlaces de acceso generados y usados, todas las acciones
  editoriales y el alta del SUPER_ADMIN inicial.
- Las sesiones de alumnos no se registran.
- Cambios estructurados `{campo: {antes, despues}}`. Los textos extensos (resumen, puntos clave…) sólo registran
  que cambiaron y su longitud. Nunca se guardan contraseñas, hashes, cookies ni tokens. No se registra la IP:
  queda pendiente de la política de privacidad.
- La tabla rechaza `UPDATE` y `DELETE` mediante un trigger; la API no expone ninguna operación de escritura.
- Alcance de lectura: `audit.read` ve todo; `audit.read.editorial` ve los eventos de contenido y los propios;
  `audit.read.own` ve los propios y los del contenido del que es autor o revisor.

## Enlaces de acceso

- El token (256 bits aleatorios) sólo se guarda como SHA-256 y caduca a las 72 horas (configurable). Generar uno
  nuevo invalida los anteriores.
- La URL usa el fragmento (`/acceso#token=…`), que el navegador no envía al servidor; la página lo borra de la barra
  de direcciones al leerlo y lo manda en el cuerpo de un `POST`.
- Una cuenta invitada queda `PENDIENTE` (sin contraseña ni acceso) hasta usar el enlace. Restablecer el acceso no
  desbloquea una cuenta bloqueada.

## Migraciones

- Convención: esquema en `V1–V99`, datos demo en `V100+`. `spring.flyway.out-of-order=true` permite aplicar una
  migración de esquema nueva sobre una base que ya tiene los datos demo.

## Fuera de esta entrega

Configuración sensible (`/admin/configuracion`) y gestión de catálogos desde la interfaz: los permisos
`settings.*` y `catalogs.*` existen, pero no hay pantallas ni endpoints porque su alcance no se aprobó. Tampoco
se incluyen exportación de datos, envío de correos, presencia en tiempo real, ni cambio de contraseña o correo
desde el perfil.
