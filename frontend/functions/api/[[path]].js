// Proxy de Cloudflare Pages: reenvía /api/* a la API (Render) para que el navegador hable siempre con el origen del
// frontend. Así la cookie de sesión y el token CSRF son de primera parte y `SameSite=Lax` funciona.
//
// Requiere la variable de entorno `API_ORIGIN` en el proyecto de Pages (por ejemplo https://api-enarmcito-dev.onrender.com,
// sin barra final). Se define en el panel de Cloudflare, nunca en el repositorio.

const HOP_BY_HOP = ['host', 'connection', 'keep-alive', 'transfer-encoding', 'upgrade', 'cf-connecting-ip'];

export async function onRequest({ request, env }) {
  if (!env.API_ORIGIN) {
    return new Response(JSON.stringify({ code: 'PROXY_NO_CONFIGURADO', message: 'Falta API_ORIGIN.' }), {
      status: 503,
      headers: { 'Content-Type': 'application/json' },
    });
  }

  const url = new URL(request.url);
  const target = new URL(url.pathname + url.search, env.API_ORIGIN);

  const headers = new Headers(request.headers);
  for (const name of HOP_BY_HOP) {
    headers.delete(name);
  }
  // La API valida el origen exacto del frontend (CORS con credenciales).
  headers.set('X-Forwarded-Host', url.host);
  headers.set('X-Forwarded-Proto', 'https');

  const upstream = await fetch(target, {
    method: request.method,
    headers,
    body: ['GET', 'HEAD'].includes(request.method) ? undefined : request.body,
    redirect: 'manual',
  });

  // Se devuelve tal cual (incluidas las cabeceras Set-Cookie) sin cachear respuestas autenticadas.
  const response = new Response(upstream.body, upstream);
  response.headers.set('Cache-Control', 'no-store');
  return response;
}
