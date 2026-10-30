# Ambientes `dev` / `prod` y CI/CD gratuito

**Repositorio:** [Vashauwu/app-enarmcito](https://github.com/Vashauwu/app-enarmcito)  
**Fecha de revisión:** 29 de octubre de 2026  
**Objetivo:** publicar dos ambientes independientes y automatizar validación, despliegue y reversión básica sin depender de un plan de pago.

> **Límite importante:** un conjunto de tiers gratuitos sirve para demostración, pruebas de usuarios iniciales y una primera versión pública. No ofrece disponibilidad garantizada, respaldo administrado ni soporte adecuado para una plataforma médica. El ambiente llamado `prod` en este documento significa “el ambiente público controlado”; no equivale aún a producción con SLA.

## 1. Arquitectura recomendada

| Componente | `dev` | `prod` | Servicio gratuito recomendado |
|---|---|---|---|
| Frontend Angular | `dev-enarmcito` | `enarmcito` | Cloudflare Pages |
| API Spring Boot | `api-enarmcito-dev` | `api-enarmcito-prod` | Render Web Service |
| PostgreSQL | Proyecto Neon `enarmcito-dev` | Proyecto Neon `enarmcito-prod` | Neon Postgres |
| Código, ramas y CI/CD | `develop` | `main` | GitHub + GitHub Actions |

Cloudflare Pages es apropiado para el frontend estático: su plan gratuito incluye sitios y solicitudes de assets estáticos sin límite publicado, con un límite de 500 despliegues mensuales por proyecto. [Cloudflare Pages](https://developers.cloudflare.com/pages/)  

Render permite servicios web gratuitos, pero los detiene tras 15 minutos sin tráfico y el siguiente acceso puede tardar aproximadamente un minuto en arrancar. Por ese motivo es adecuado para `dev` y demostraciones, no para atención clínica con expectativa de disponibilidad inmediata. [Render Free](https://render.com/docs/free)  

Neon anunció en octubre de 2026 hasta 100 proyectos gratuitos con 1 GB de PostgreSQL por proyecto; se usarán dos proyectos separados para impedir que pruebas contaminen datos públicos. [Neon Free Plan](https://neon.com/blog/neon-free-plan-1-gb-per-project)  

No se recomienda usar PostgreSQL gratuito de Render para datos persistentes: expira a los 30 días y no incluye respaldos. [Limitaciones de Render Postgres](https://render.com/docs/free)

## 2. Convención de ramas y promoción

```text
feature/* ── Pull Request ──> develop ── CI + despliegue dev ──> main ── CI + aprobación + despliegue prod
bugfix/*  ── Pull Request ──> develop
hotfix/*  ── Pull Request ──> main ── CI + aprobación + despliegue prod
```

- `main`: código publicado en `prod`; sólo se actualiza por Pull Request aprobado.
- `develop`: integración continua y ambiente `dev`.
- `feature/*`, `bugfix/*`, `hotfix/*`: trabajo aislado. Nunca contienen secretos ni datos reales.
- Crear una etiqueta `vX.Y.Z` cada vez que se publique una versión en `prod`.
- Activar protección de ramas en GitHub: exigir Pull Request, al menos una aprobación, checks de CI exitosos, rama actualizada y prohibir `force push`/borrado de `main` y `develop`.

## 3. Crear los dos ambientes en GitHub

En **Settings → Environments** del repositorio crear exactamente:

| Ambiente | Rama permitida | Protección | Finalidad |
|---|---|---|---|
| `dev` | `develop` | Sin aprobador obligatorio. | Validación integrada. |
| `prod` | `main` | Requerir al menos un aprobador responsable. | Publicación pública controlada. |

El job de despliegue debe declarar `environment: dev` o `environment: prod`. GitHub mostrará el historial, secretos y quién autorizó cada despliegue.

GitHub Actions es gratis en repositorios públicos con runners estándar. En repositorios privados, GitHub Free incluye una cuota mensual —actualmente mostrada como USD 12 de cómputo incluido, equivalente históricamente a 2,000 minutos Linux estándar— por lo que las ejecuciones deben ser breves y sin artefactos pesados. [Uso incluido de GitHub](https://docs.github.com/en/billing/reference/product-usage-included)

## 4. Servicios externos a crear

### 4.1 Bases de datos Neon

Crear dos proyectos diferentes en Neon:

1. `enarmcito-dev`, con una base `medicina_dev`.
2. `enarmcito-prod`, con una base `medicina_prod`.

Para cada uno, copiar la cadena de conexión PostgreSQL con SSL y configurar en el servicio Render correspondiente:

```text
SPRING_DATASOURCE_URL=<JDBC URL de Neon>
SPRING_DATASOURCE_USERNAME=<usuario Neon>
SPRING_DATASOURCE_PASSWORD=<contraseña Neon>
```

Nunca guardar estas variables en el repositorio, archivos `.env` versionados, logs o secretos de frontend. `dev` y `prod` no comparten usuario, base, contraseña ni proyecto Neon.

### 4.2 Servicios Render

Crear dos **Web Services** desde el mismo repositorio, con autodespliegue desactivado. Los workflows de GitHub dispararán el despliegue sólo después de que CI concluya correctamente.

| Campo | Servicio dev | Servicio prod |
|---|---|---|
| Nombre | `api-enarmcito-dev` | `api-enarmcito-prod` |
| Branch | `develop` | `main` |
| Root directory | `backend` | `backend` |
| Runtime | Java | Java |
| Build command | `./mvnw --batch-mode clean package -DskipTests` | Igual |
| Start command | `java -Dserver.port=$PORT -jar target/*.jar` | Igual |
| Plan | Free | Free, sólo mientras se acepte el arranque lento |
| Health check | `/actuator/health` cuando el endpoint exista | Igual |

Variables adicionales a definir en Render para cada ambiente:

```text
SPRING_PROFILES_ACTIVE=dev                 # usar prod en producción
APP_FRONTEND_ORIGIN=<URL exacta del frontend de ese ambiente>
APP_COOKIE_SECURE=true
APP_COOKIE_SAME_SITE=<ver sección 6>
```

Conservar las variables sensibles únicamente en Render. Crear un **Deploy Hook** por servicio y guardar su URL solamente como secreto del ambiente GitHub; Render indica expresamente que la URL del hook es un valor secreto. [Render Deploy Hooks](https://render.com/docs/deploy-hooks)

### 4.3 Frontends Cloudflare Pages

Crear dos proyectos Pages conectados al repositorio o desplegados por API:

| Campo | Proyecto dev | Proyecto prod |
|---|---|---|
| Nombre | `enarmcito-dev` | `enarmcito-prod` |
| Rama publicada | `develop` | `main` |
| Directorio raíz | `frontend` | `frontend` |
| Build command | `npm ci && npm run build -- --configuration development` | `npm ci && npm run build -- --configuration production` |
| Directorio de salida | El valor real de `dist/<proyecto>/browser` generado por Angular | Igual |

Antes de automatizar el primer despliegue, ejecutar localmente `npm run build -- --configuration production` y confirmar el directorio creado en `frontend/dist`. No se debe adivinar el nombre del proyecto Angular en un workflow.

## 5. Secretos y variables de GitHub

En cada ambiente de GitHub, añadir los secretos siguientes. No se agregan como secretos globales del repositorio porque `dev` y `prod` deben permanecer aislados.

| Nombre | `dev` | `prod` | Uso |
|---|---|---|---|
| `CLOUDFLARE_API_TOKEN` | Token con permiso mínimo Pages Edit | Token independiente con permiso mínimo Pages Edit | Publicar frontend. |
| `CLOUDFLARE_ACCOUNT_ID` | Cuenta de Cloudflare | Cuenta de Cloudflare | Publicar frontend. |
| `RENDER_DEPLOY_HOOK` | Hook de `api-enarmcito-dev` | Hook de `api-enarmcito-prod` | Publicar backend tras CI. |
| `FRONTEND_API_URL` | URL pública o proxy de API dev | URL pública o proxy de API prod | Compilar configuración del frontend. |

Variables no secretas recomendadas:

| Nombre | `dev` | `prod` |
|---|---|---|
| `CLOUDFLARE_PROJECT` | `enarmcito-dev` | `enarmcito-prod` |
| `FRONTEND_DIST_DIR` | Ruta validada, por ejemplo `frontend/dist/frontend/browser` | Misma forma validada |

El token de Cloudflare debe estar limitado a la cuenta y al permiso de edición de Pages; no usar un token global. Los hooks de Render, cadenas Neon, cookies, credenciales o llaves nunca se imprimen con `echo` en un job.

## 6. Requisito crítico: sesión entre frontend y API

La aplicación actual usa sesión en cookie y protección CSRF. Si el frontend queda en `*.pages.dev` y la API en `*.onrender.com`, son orígenes distintos. Antes de publicar se debe resolver explícitamente este punto; de lo contrario, el login puede funcionar visualmente pero la cookie no viajará de manera fiable en peticiones posteriores.

Hay dos alternativas correctas:

1. **Recomendada para el plan gratuito:** publicar una función/proxy de Cloudflare Pages bajo la ruta `/api/*` que reenvíe a Render. El navegador siempre llama a la misma URL del frontend y mantiene `API_URL=/api/v1`.
2. **Recomendada al comprar dominio:** usar dominios como `app.midominio.mx` y `api.midominio.mx`, configurar CORS con el origen exacto, cookie `Secure` y una política `SameSite` compatible. No usar `*` en CORS cuando se envían credenciales.

La alternativa de dos dominios gratuitos distintos sin proxy no debe considerarse lista para producción debido a restricciones modernas de cookies de terceros. Esta decisión debe probarse con Chrome, Safari y móvil antes de abrir el ambiente público.

También se debe definir configuración explícita por perfil:

```text
# application-dev.yml
app.frontend-origin=https://<frontend-dev>

# application-prod.yml
app.frontend-origin=https://<frontend-prod>
```

La aplicación Angular necesita recibir la URL correcta de API por configuración de build o configuración de ejecución; no debe contener la URL de `prod` hardcodeada en un build de `dev`.

## 7. Workflows de GitHub Actions

Crear estos archivos en `.github/workflows/`. El ejemplo usa Java 25 porque es la línea indicada por el MVP; confirmar que coincide con `backend/pom.xml` antes de fijarlo. Si el `pom.xml` define otra versión, usar esa misma versión en todos los workflows.

### 7.1 `ci.yml`

Se ejecuta en Pull Requests hacia `develop` o `main`, y en cada push a ambas ramas. Sólo valida: nunca despliega.

```yaml
name: CI

on:
  pull_request:
    branches: [develop, main]
  push:
    branches: [develop, main]

permissions:
  contents: read

jobs:
  backend:
    name: Backend — pruebas y build
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: backend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '25'
          cache: maven
      - run: chmod +x mvnw
      - run: ./mvnw --batch-mode verify

  frontend:
    name: Frontend — pruebas y build
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: frontend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '22'
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - run: npm ci
      - run: npm run test -- --watch=false
      - run: npm run build -- --configuration production
```

Si el proyecto Angular utiliza un script de pruebas diferente, se ajusta sólo esa línea tras ejecutar `npm run` en `frontend`; no se omiten pruebas para hacer pasar CI.

### 7.2 `deploy-dev.yml`

Despliega únicamente cuando un commit llega a `develop` y CI finalizó satisfactoriamente. Guardarlo como workflow separado o, de preferencia, integrarlo como job dependiente de CI para evitar carreras.

```yaml
name: Deploy dev

on:
  workflow_run:
    workflows: [CI]
    types: [completed]
    branches: [develop]

permissions:
  contents: read

jobs:
  deploy:
    if: >-
      github.event.workflow_run.conclusion == 'success' &&
      github.event.workflow_run.event == 'push' &&
      github.event.workflow_run.head_branch == 'develop'
    runs-on: ubuntu-latest
    environment: dev
    steps:
      - uses: actions/checkout@v4
        with:
          ref: ${{ github.event.workflow_run.head_sha }}
      - uses: actions/setup-node@v4
        with:
          node-version: '22'
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - name: Build frontend
        working-directory: frontend
        env:
          FRONTEND_API_URL: ${{ secrets.FRONTEND_API_URL }}
        run: |
          npm ci
          npm run build -- --configuration development
      - name: Deploy frontend to Cloudflare Pages
        run: >-
          npx wrangler@4 pages deploy "${{ vars.FRONTEND_DIST_DIR }}"
          --project-name "${{ vars.CLOUDFLARE_PROJECT }}"
          --branch develop
        env:
          CLOUDFLARE_API_TOKEN: ${{ secrets.CLOUDFLARE_API_TOKEN }}
          CLOUDFLARE_ACCOUNT_ID: ${{ secrets.CLOUDFLARE_ACCOUNT_ID }}
      - name: Trigger Render deploy
        run: curl --fail --silent --show-error --request POST "${{ secrets.RENDER_DEPLOY_HOOK }}"
```

El build debe tomar `FRONTEND_API_URL` desde la configuración de Angular. Implementar ese enlace antes de usar el workflow; un secreto de Actions no modifica un bundle estático por sí mismo.

### 7.3 `deploy-prod.yml`

Usa el mismo patrón, pero sólo para `main` y con el ambiente `prod`, que debe requerir aprobación humana.

```yaml
name: Deploy prod

on:
  workflow_run:
    workflows: [CI]
    types: [completed]
    branches: [main]

permissions:
  contents: read

jobs:
  deploy:
    if: >-
      github.event.workflow_run.conclusion == 'success' &&
      github.event.workflow_run.event == 'push' &&
      github.event.workflow_run.head_branch == 'main'
    runs-on: ubuntu-latest
    environment: prod
    steps:
      - uses: actions/checkout@v4
        with:
          ref: ${{ github.event.workflow_run.head_sha }}
      - uses: actions/setup-node@v4
        with:
          node-version: '22'
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - name: Build frontend
        working-directory: frontend
        env:
          FRONTEND_API_URL: ${{ secrets.FRONTEND_API_URL }}
        run: |
          npm ci
          npm run build -- --configuration production
      - name: Deploy frontend to Cloudflare Pages
        run: >-
          npx wrangler@4 pages deploy "${{ vars.FRONTEND_DIST_DIR }}"
          --project-name "${{ vars.CLOUDFLARE_PROJECT }}"
          --branch main
        env:
          CLOUDFLARE_API_TOKEN: ${{ secrets.CLOUDFLARE_API_TOKEN }}
          CLOUDFLARE_ACCOUNT_ID: ${{ secrets.CLOUDFLARE_ACCOUNT_ID }}
      - name: Trigger Render deploy
        run: curl --fail --silent --show-error --request POST "${{ secrets.RENDER_DEPLOY_HOOK }}"
```

> `workflow_run` utiliza el resultado de CI y luego solicita la aprobación configurada en el environment `prod`. No ejecutar un despliegue desde un Pull Request sin una rama protegida y aprobación.

## 8. Configuración que el código debe admitir antes del primer despliegue

1. Perfiles Spring `dev` y `prod`, ambos leyendo base de datos, origen CORS y banderas de cookie desde variables de entorno.
2. URL de API configurable en Angular durante el build, sin URL local o credenciales incorporadas en `dist`.
3. Endpoint de salud sin datos privados para Render.
4. Migraciones Flyway ejecutadas por la aplicación de forma controlada al arrancar, con respaldo previo en `prod` cuando exista uno.
5. CORS limitado al origen exacto de cada frontend y CSRF conservado para peticiones de sesión.
6. Logs sin contraseñas, cookies, tokens, JDBC URL ni datos clínicos identificables.
7. El build Angular debe incluir fallback de SPA en Pages para que `/app/*` y futuras rutas administrativas no devuelvan 404 al recargar.

## 9. Verificación antes y después de publicar

### Lista de salida de `dev`

- [ ] Pull Request aprobado y CI en verde: pruebas backend, pruebas frontend y ambos builds.
- [ ] Frontend dev apunta exclusivamente a API dev.
- [ ] API dev apunta exclusivamente a Neon dev.
- [ ] Registro, login, CSRF, logout, lectura de contenido y una ruta protegida funcionan desde navegador real.
- [ ] No hay secretos en el historial Git ni en logs de GitHub Actions/Render.
- [ ] Las migraciones se ejecutaron una vez y el esquema se verificó.

### Lista de salida de `prod`

- [ ] Los mismos checks de `dev` están aprobados sobre el commit exacto de `main`.
- [ ] Un responsable autorizó el environment `prod`.
- [ ] `APP_FRONTEND_ORIGIN` corresponde al frontend prod, no al de dev.
- [ ] API prod usa sólo Neon prod.
- [ ] Se confirmó el flujo de sesión y cookies en dominio público, escritorio y móvil.
- [ ] Existe una etiqueta de versión y se documentó qué migraciones se aplicaron.
- [ ] Se probó que un usuario no puede acceder a datos o endpoints administrativos sin permiso.

## 10. Reversión y operación

1. Si falla el frontend, volver a desplegar el commit anterior conocido desde Cloudflare Pages o reejecutar el workflow sobre esa etiqueta.
2. Si falla el backend, usar el rollback de Render disponible para los dos despliegues previos o desplegar la etiqueta anterior. Render documenta ese alcance limitado para Web Services Free. [Render Free](https://render.com/docs/free)
3. No revertir migraciones destructivas en caliente. Diseñar migraciones compatibles hacia adelante y probarlas en Neon dev antes de `prod`.
4. Si hay incidente de secreto, revocar el token/hook/credencial comprometido, reemplazarlo en proveedor y en GitHub Environment, y revisar logs/auditoría.
5. Mantener una exportación periódica manual de Neon mientras el proyecto use tiers gratuitos; validar restauración antes de depender de ella.

## 11. Alternativas gratuitas evaluadas

| Opción | Úsala para | Ventaja | Límite relevante |
|---|---|---|---|
| Cloudflare Pages | Frontend Angular | Gratis, CDN, HTTPS y despliegue desde GitHub. | Requiere proxy o dominio propio para que la sesión con API separada sea fiable. |
| Render Web Service | Backend Java/Spring | Ejecuta un proceso Java normal y ofrece HTTPS. | Suspende tras 15 min de inactividad; primer acceso lento. |
| Neon | PostgreSQL de dev/prod | Dos proyectos aislados y tier gratuito de 1 GB por proyecto. | Sin garantías de un servicio administrado de producción; vigilar límites y hacer respaldo. |
| Render Static Site | Alternativa al frontend Pages | Menos proveedores, integración sencilla con Render. | No elimina por sí solo el reto de cookies si API y web terminan en orígenes distintos. |
| GitHub Pages | Documentación o landing sin API | Gratis y muy simple. | No es la opción indicada para la SPA autenticada sin resolver proxy/API. |

No se recomienda Fly.io como opción “todo gratis” para este plan: su oferta actual se basa en prueba/créditos y consumo, no en un hosting gratuito permanente claramente equivalente. [Fly pricing](https://fly.io/pricing/)

## 12. Punto de decisión

Antes de declarar el ambiente `prod` como lanzamiento real, el responsable debe aceptar por escrito una de estas dos decisiones:

- **Demo pública gratuita:** Cloudflare Pages + Render Free + Neon Free, con arranque lento ocasional del backend, límites controlados y plan de migración.
- **Producción operativa:** conservar el CI/CD y los ambientes definidos aquí, pero migrar backend, base de datos, almacenamiento de archivos, monitoreo y respaldos a planes con disponibilidad y recuperación acordadas.

Para contenido educativo médico y futuros módulos de guías clínicas, la segunda opción será necesaria antes de comprometer continuidad, protección de información o tiempos de respuesta a usuarios.

## 13. Estado de implementación en el repositorio

### Hecho en el código (verificado)

| Pieza | Archivo | Verificación |
|---|---|---|
| CI (backend + frontend) | `.github/workflows/ci.yml` | YAML válido; los mismos comandos pasan en local. |
| Despliegue `dev` y `prod` | `.github/workflows/deploy-dev.yml`, `deploy-prod.yml` | YAML válido. No se pueden ejecutar hasta crear los servicios y secretos (sección 5). |
| Perfiles Spring | `application-dev.yml`, `application-prod.yml` | Prueba de integración con perfil `prod`; imagen arrancada con `prod` contra una base vacía. |
| Salud para Render | `spring-boot-starter-actuator`; sólo `health` expuesto | Pruebas: `/actuator/health` sin detalles; `env` y `beans` no accesibles. |
| Imagen del backend | `backend/Dockerfile`, `.dockerignore` | Construida y arrancada; migraciones aplicadas y 0 datos demo. |
| Proxy `/api/*` y SPA | `frontend/functions/api/[[path]].js`, `frontend/public/_routes.json` | Sintaxis válida; el build copia `_routes.json` a `dist`. No se probó en Cloudflare. |
| Variables compatibles con este documento | `APP_FRONTEND_ORIGIN`, `APP_COOKIE_SECURE`, `APP_COOKIE_SAME_SITE` | Con respaldo en los nombres anteriores (`FRONTEND_ORIGIN`, `SESSION_COOKIE_SECURE`). |

### Dónde la implementación difiere del texto de este documento

1. **Render no tiene runtime Java nativo.** El servicio se crea como **Docker** (Root directory `backend`, Dockerfile `Dockerfile`); no se usan *Build command* ni *Start command* de la sección 4.2. El resto de la configuración (autodespliegue desactivado, Deploy Hook, variables) no cambia.
2. **Health check:** usar `/actuator/health/liveness`, que no consulta la base. `/actuator/health` sí la consulta y mantendría despierta la base serverless de Neon, gastando su cuota gratuita. Para verificar la base a mano, usa `/actuator/health`.
3. **URL de la API:** se eligió la alternativa 1 de la sección 6 (proxy de Pages). El frontend llama siempre a `/api/v1` y el bundle no lleva ninguna URL de API; por eso **no existe el secreto `FRONTEND_API_URL`**. En su lugar, cada proyecto de Cloudflare Pages necesita la variable `API_ORIGIN` (por ejemplo `https://api-enarmcito-dev.onrender.com`, sin barra final).
4. **Cloudflare Pages:** `wrangler pages deploy` se ejecuta desde `frontend/` (para incluir `functions/`), así que `FRONTEND_DIST_DIR` vale `dist/frontend/browser` (relativa a `frontend`), no `frontend/dist/frontend/browser`.
5. **Java 17** en CI: es el `java.version` de `backend/pom.xml` (la sección 7 sugería 25 sólo como ejemplo).
6. **Fallback de SPA:** Pages lo aplica solo mientras no exista un `404.html` en la salida; no hay ninguno. No se añadió `_redirects` para no interferir con el proxy.
7. **Logs:** Flyway e Hibernate escribían la URL JDBC al arrancar; se silenciaron en `dev` y `prod`.

### Pasos manuales pendientes (requieren tus cuentas)

1. Subir el repositorio a GitHub, crear `develop`, activar la protección de ramas (sección 2) y los environments `dev` y `prod` (sección 3). **Los workflows `deploy-*.yml` deben estar en `main` para que GitHub dispare `workflow_run`.**
2. Neon: dos proyectos. La URL JDBC debe llevar `?sslmode=require`, por ejemplo `jdbc:postgresql://<host>/medicina_dev?sslmode=require`.
3. Render: dos Web Services **Docker** con las variables de la sección 4.2 más:
   - `SPRING_PROFILES_ACTIVE=dev` o `prod`.
   - `APP_FRONTEND_ORIGIN` con la URL exacta del frontend de ese ambiente.
   - **`prod`:** `APP_BOOTSTRAP_SUPERADMIN_EMAIL` y `APP_BOOTSTRAP_SUPERADMIN_PASSWORD` (12 caracteres o más). Sin ellas el ambiente queda sin administrador; retíralas tras el primer arranque. No definas `APP_DEMO_DATA` en `prod`.
   - **`dev`:** hereda los datos demo (cuentas con contraseña conocida `Demo1234`); si el ambiente es público, define `APP_DEMO_DATA=false`.
4. Cloudflare Pages: dos proyectos (`enarmcito-dev`, `enarmcito-prod`) con la variable `API_ORIGIN`, y un token de API por ambiente con permiso *Pages Edit*.
5. Secretos y variables de cada environment de GitHub (sección 5, sin `FRONTEND_API_URL`).
6. Probar login, CSRF y logout en Chrome, Safari y móvil antes de abrir `prod`.

### Limitación a resolver antes de depender de `prod`

Los medios que suben los editores (imágenes y PDF) se guardan en el disco del contenedor. En Render Free ese disco es **efímero**: se pierde en cada despliegue y cada vez que el servicio se duerme. Mientras no se migre a almacenamiento de objetos, los archivos subidos en `dev` y `prod` no son confiables.

