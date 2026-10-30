// Proxy de Cloudflare Pages: reenvía /api/* a la API (Render) para que el navegador hable siempre con el origen del
// frontend. Así la cookie de sesión y el token CSRF son de primera parte y `SameSite=Lax` funciona.
//
// Requiere la variable de entorno `API_ORIGIN` en el proyecto de Pages (por ejemplo https://api-enarmcito-dev.onrender.com,
// sin barra final). Se define en el panel de Cloudflare, nunca en el repositorio.

// Sólo se reenvían las cabeceras que la API necesita. Reenviar todas arrastra las de Cloudflare (CDN-Loop, CF-*,
// X-Forwarded-*) y, como Render también está detrás de Cloudflare, el origen puede rechazar la petición por bucle.
const FORWARDED = ['accept', 'accept-language', 'content-type', 'content-length', 'cookie', 'origin', 'referer', 'user-agent', 'x-xsrf-token'];

function error(status, code, message) {
  return new Response(JSON.stringify({ code, message }), {
    status,
    headers: { 'Content-Type': 'application/json', 'Cache-Control': 'no-store' },
  });
}

export async function onRequest({ request, env }) {
  if (!env.API_ORIGIN) {
    return new Response(JSON.stringify({ code: 'PROXY_NO_CONFIGURADO', message: 'Falta API_ORIGIN.' }), {
      status: 503,
      headers: { 'Content-Type': 'application/json' },
    });
  }

  const url = new URL(request.url);
  let origin;
  try {
    origin = new URL(String(env.API_ORIGIN).trim());
  } catch {
    return error(500, 'PROXY_MAL_CONFIGURADO', 'API_ORIGIN no es una URL válida (debe empezar con https://).');
  }
  // Si apunta al propio sitio, el proxy se llamaría a sí mismo en bucle y Cloudflare respondería con el error 1019.
  if (origin.host === url.host || origin.hostname.endsWith('.pages.dev')) {
    return error(500, 'PROXY_MAL_CONFIGURADO', 'API_ORIGIN apunta al propio frontend; debe ser la URL de la API (Render).');
  }
  const target = new URL(url.pathname + url.search, origin);

  const headers = new Headers();
  for (const name of FORWARDED) {
    const value = request.headers.get(name);
    if (value) {
      headers.set(name, value);
    }
  }
  // Sin Accept-Encoding el origen responde sin comprimir y Cloudflare comprime en el borde.

  let upstream;
  try {
    upstream = await fetch(target, {
      method: request.method,
      headers,
      body: ['GET', 'HEAD'].includes(request.method) ? undefined : request.body,
      redirect: 'manual',
    });
  } catch (error) {
    // La API no respondió (por ejemplo, Render se está despertando): un JSON claro en vez de la página de error genérica.
    return new Response(
      JSON.stringify({ code: 'API_NO_DISPONIBLE', message: 'La API no respondió. Inténtalo de nuevo en unos segundos.' }),
      { status: 502, headers: { 'Content-Type': 'application/json', 'Cache-Control': 'no-store' } },
    );
  }

  // Se devuelve tal cual (incluidas las cabeceras Set-Cookie) sin cachear respuestas autenticadas.
  const response = new Response(upstream.body, upstream);
  response.headers.set('Cache-Control', 'no-store');
  return response;
}
