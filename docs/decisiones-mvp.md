# Decisiones de implementación del MVP

Complementa la especificación funcional (`plataforma_medicina_mvp.md`). Registra las decisiones que la
especificación dejaba abiertas y las desviaciones respecto a la propuesta.

## Versiones

| Componente | Especificación | Implementado | Motivo |
|---|---|---|---|
| Angular / Material | 22.x | 22.2 | — |
| Spring Boot | 4.1.x | 4.1.2 | — |
| Java | 25 LTS | Bytecode 17, compatible con JDK 25 | Spring Boot 4 requiere Java 17 como mínimo; compilar para 17 permite usar cualquier JDK 17+. Para exigir 25, cambia `java.version` en `backend/pom.xml`. |
| PostgreSQL | 18.x | imagen `postgres:18` | — |
| Pruebas frontend | según Angular | Vitest (runner por defecto de Angular 22) | — |

## Sesión y seguridad

- **Sesión de servidor** con cookie `MEDICINA_SESSION` (`HttpOnly`, `SameSite=Lax`, `Secure` configurable).
  Sin JWT: la sesión se invalida en backend al cerrar sesión.
- **CSRF** con el modo SPA de Spring Security 7 (`csrf.spa()`): cookie `XSRF-TOKEN` legible por Angular y
  encabezado `X-XSRF-TOKEN`. El token se regenera al iniciar sesión.
- Protección contra **fijación de sesión**: se cambia el id de sesión al autenticarse.
- Contraseñas con el `DelegatingPasswordEncoder` de Spring Security (BCrypt, prefijo `{bcrypt}`).
  Longitud 8–72: BCrypt sólo procesa 72 bytes.
- **Login con mensaje genérico** («Correo o contraseña incorrectos.») tanto si el correo no existe como si la
  cuenta está bloqueada.
- **CORS** explícito para `FRONTEND_ORIGIN`. En desarrollo no se usa: el proxy de Angular hace que frontend y
  API compartan origen, lo que además permite que `HttpClient` envíe el token XSRF automáticamente.
- El principal de la sesión sólo guarda la identidad; **tier y estado de la cuenta se leen de la base en cada
  petición**, de modo que un cambio de tier o un bloqueo surten efecto de inmediato.

## Tiers

- `orden_acceso` define la jerarquía (Básico 10, Intermedio 20, Premium 30).
- Selección de tier en el registro **permitida sólo para pruebas** (`APP_REGISTRATION_ALLOW_TIER_SELECTION`).
  Con `false`, el backend ignora el tier enviado y asigna el de menor acceso. Antes de abrir registros públicos
  con tiers de pago, la asignación debe venir de una compra verificada, invitación o acción administrativa.

## Contenido y búsqueda

- El listado y la búsqueda **sólo devuelven artículos que el usuario puede leer** (publicados y dentro de su
  tier), con un extracto de ~180 caracteres; nunca el contenido completo.
- Abrir por slug un artículo de tier superior responde **403 `TIER_INSUFICIENTE`**; la interfaz muestra un
  mensaje de contenido no incluido. Un borrador o un slug inexistente responden **404**.
- Búsqueda por título y área médica **sin distinguir mayúsculas ni acentos** (extensión `unaccent` de
  PostgreSQL). Máximo 100 caracteres; los comodines `%` y `_` se escapan. Paginación de 1 a 50 elementos.
- `puntos_clave` se guarda como texto con un punto por línea y la API lo entrega como lista.

## Progreso

- Abrir un artículo hace *upsert* en `usuario_contenido_progreso`: lo crea como `EN_PROGRESO` o actualiza
  `ultima_consulta_at`.
- **Decisión:** volver a abrir un artículo ya `COMPLETADO` no lo regresa a `EN_PROGRESO`; sólo actualiza la
  fecha de consulta. (La regla 9 de la especificación podía leerse como un reinicio; se evitó para no perder
  el estado de completado.)
- «Continuar aprendizaje» muestra el artículo consultado más recientemente **que el usuario aún puede leer**.

## Registro

- Al registrarse se inicia sesión y se redirige al dashboard (propuesta de la especificación).
- El correo se normaliza (sin espacios, minúsculas); la base lo refuerza con un `CHECK` y una restricción única.

## Datos demo

- `db/migration`: esquema y tiers (siempre). `db/demo`: 9 artículos y 3 cuentas (sólo con `APP_DEMO_DATA=true`).
- Los artículos citan guías reales con DOI cuando existe (Sepsis-3, Surviving Sepsis Campaign 2021,
  ADA Standards of Care 2025, ACC/AHA 2017 de hipertensión, AHA/ACC/HFSA 2022 de insuficiencia cardiaca,
  ACC/AHA/ACCP/HRS 2023 de fibrilación auricular, ATS/IDSA 2019 de neumonía, GOLD 2025, GINA).
  Son contenido de ejemplo: **deben pasar por el proceso editorial antes de publicarse**.

## Pendiente (fuera del alcance del MVP)

Pagos y asignación de tiers por compra, panel de administración/CMS, recuperación y cambio de contraseña o
correo, CI/CD, despliegue y política de retención de sesiones.
