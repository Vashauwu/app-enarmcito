# Plataforma de Medicina — MVP

Aplicación web responsiva para estudiar medicina con **artículos resumidos en formato de lectura guiada**.
Incluye landing pública, registro, inicio/cierre de sesión, perfil, catálogo con búsqueda, control de
acceso por *tier*, detalle del artículo y seguimiento mínimo de progreso («Continuar aprendizaje»).

| Capa | Tecnología |
|---|---|
| Frontend | Angular 22.2 + Angular Material 22.2 (Material 3) |
| Backend | Spring Boot 4.1.2 (Spring Security 7, Spring Data JPA, Flyway) |
| Base de datos | PostgreSQL 18 (Docker Compose) |
| Pruebas | JUnit 5 + Testcontainers (backend), Vitest (frontend) |

Arquitectura: **monolito modular** con API REST (`/api/v1`). Ver [docs/decisiones-mvp.md](docs/decisiones-mvp.md).

## Prerrequisitos

- Git
- **JDK 17 o superior** (se recomienda JDK 25 LTS). El bytecode se compila para Java 17.
- **Node.js `^22.22.3` o `^24.15.0`** (requisito de Angular 22) y npm
- Docker Desktop o Docker Engine con Docker Compose

## Arranque local

```bash
# 1. Variables locales (ajusta POSTGRES_PASSWORD y SPRING_DATASOURCE_PASSWORD con el mismo valor)
cp .env.example .env

# 2. Base de datos
docker compose up -d db

# 3. Backend (aplica las migraciones Flyway al arrancar) → http://localhost:8080
cd backend
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run

# 4. Frontend, en otra terminal → http://localhost:4200
cd frontend
npm ci
npm start
```

Abre <http://localhost:4200>. El servidor de Angular reenvía `/api` al backend (`frontend/proxy.conf.json`),
así navegador, cookies de sesión y token CSRF comparten origen.

### Cuentas demo

Con `APP_DEMO_DATA=true` (valor por defecto) se cargan 9 artículos de ejemplo (uno en borrador) y una
cuenta por tier. Contraseña de las tres: **`Demo1234`**.

| Correo | Tier | Artículos visibles |
|---|---|---|
| `basico@demo.local` | Básico | 4 |
| `intermedio@demo.local` | Intermedio | 6 |
| `premium@demo.local` | Premium | 8 |

Cuentas del panel administrativo (misma contraseña, `Demo1234`), una por rol:

| Correo | Rol |
|---|---|
| `superadmin@demo.local` | Superadministrador |
| `plataforma@demo.local` | Administrador de plataforma |
| `usuarios@demo.local` | Administrador de usuarios |
| `soporte@demo.local` | Soporte |
| `editor@demo.local` | Editor de contenido |
| `revisor@demo.local`, `revisor2@demo.local` | Revisor médico |
| `autor@demo.local` | Autor de contenido |

El panel está en <http://localhost:4200/admin> (también en el menú de usuario como «Panel administrativo»).

> Los datos demo viven en `backend/src/main/resources/db/demo` y sólo se aplican si `APP_DEMO_DATA=true`.
> Decide el valor antes de la primera migración: si lo desactivas después sobre la misma base, Flyway
> detectará migraciones aplicadas que ya no encuentra. Para empezar de cero: `docker compose down -v`.

## Panel administrativo

Zona privada para operar la plataforma sin tocar la base de datos. Especificación en
[docs/panel-administrativo.md](docs/panel-administrativo.md); decisiones de implementación en
[docs/panel-administrativo-implementacion.md](docs/panel-administrativo-implementacion.md).

- **Roles y permisos:** 8 roles con permisos atómicos (`users.read`, `content.publish`…). La API los verifica en
  cada petición; el rol y el estado de la cuenta se releen de la base, así que un bloqueo o cambio de rol
  surte efecto de inmediato.
- **Usuarios:** búsqueda y filtros, ficha, cambio de tier, bloqueo y desbloqueo con motivo, cambio de rol
  según jerarquía, alta por invitación y restablecimiento de acceso mediante enlace de un solo uso.
- **Contenido:** editor completo y flujo `BORRADOR → EN_REVISION → CAMBIOS_SOLICITADOS / APROBADO → PUBLICADO → ARCHIVADO`,
  con separación obligatoria entre autor y revisor.
- **Actividad y auditoría:** usuarios activos por periodo y bitácora de sólo lectura (la base rechaza
  modificaciones).

**Primer SUPER_ADMIN fuera del entorno demo:** define `APP_BOOTSTRAP_SUPERADMIN_EMAIL` y
`APP_BOOTSTRAP_SUPERADMIN_PASSWORD` (12+ caracteres) en `.env`, arranca el backend una vez y retira ambas variables.
Sólo se aplica si no hay ningún SUPER_ADMIN activo.

## Pruebas y build

```bash
cd backend && ./mvnw test        # 40 pruebas; Testcontainers levanta PostgreSQL 18 (requiere Docker)
cd frontend && npm test          # 24 pruebas con Vitest
cd frontend && npm run build     # build de producción en frontend/dist
```

Estos mismos comandos son los que debería ejecutar una futura pipeline de CI.

## Variables de configuración

| Variable | Uso | Valor local |
|---|---|---|
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` / `POSTGRES_PORT` | Contenedor de PostgreSQL | ver `.env.example` |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | Conexión del backend | ver `.env.example` |
| `FRONTEND_ORIGIN` | Origen permitido por CORS | `http://localhost:4200` |
| `APP_DEMO_DATA` | Carga artículos y cuentas demo | `true` |
| `APP_REGISTRATION_ALLOW_TIER_SELECTION` | El usuario elige su tier al registrarse. Con `false` el backend asigna siempre el tier de menor acceso | `true` |
| `SESSION_COOKIE_SECURE` | Cookie de sesión sólo por HTTPS | `false` (usar `true` con HTTPS) |

El backend lee `../.env` al ejecutarse desde `backend/`; las variables de entorno del sistema tienen prioridad.
`.env` está en `.gitignore`: nunca lo subas al repositorio.

## API

| Método | Endpoint | Autenticación | Propósito |
|---|---|---|---|
| GET | `/api/v1/tiers` | Pública | Tiers activos |
| POST | `/api/v1/auth/register` | Pública | Crea la cuenta e inicia sesión (201) |
| POST | `/api/v1/auth/login` | Pública | Inicia sesión |
| POST | `/api/v1/auth/logout` | Sesión | Cierra sesión (204) |
| GET | `/api/v1/auth/me` | Sesión | Usuario autenticado |
| GET | `/api/v1/content?q=&page=&size=` | Sesión | Búsqueda paginada de artículos autorizados |
| GET | `/api/v1/content/{slug}` | Sesión | Detalle; lo registra como último consultado |
| PUT | `/api/v1/me/profile` | Sesión | Actualiza nombre y apellido |
| GET | `/api/v1/me/learning` | Sesión | Último artículo y contadores de progreso |
| PUT | `/api/v1/me/learning/{contenidoId}/complete` | Sesión | Marca como completado |

Endpoints administrativos bajo `/api/v1/admin/**` (exigen `admin.access` y el permiso de cada operación):
`dashboard`, `usuarios` (listar, ficha, `opciones`, invitar, editar, `bloquear`, `desbloquear`, `rol`,
`restablecer-acceso`), `contenido` (listar, crear, editar, `areas`, `responsables`, `enviar-revision`, `revision`,
`publicar`, `archivar`, `restaurar`), `actividad/usuarios` y `auditoria`. Públicos para enlaces de acceso:
`POST /api/v1/auth/acceso/consultar` y `POST /api/v1/auth/acceso/definir-password`.

Errores con formato estable `{ "code", "message", "fieldErrors"? }`. Las peticiones que modifican datos
requieren el encabezado `X-XSRF-TOKEN` con el valor de la cookie `XSRF-TOKEN` (Angular lo envía solo).

## Estructura

```text
├── docker-compose.yml        PostgreSQL 18
├── .env.example
├── docs/                     Decisiones de implementación
├── backend/                  Spring Boot (com.empresa.medicina)
│   └── src/main/java/…/      config · common · auth · users · tiers · content · progress
│   └── src/main/resources/   application.yml · db/migration · db/demo
└── frontend/                 Angular + Material
    └── src/app/              core · shared · features/{landing,auth,shell,dashboard,content,profile}
```
