package com.empresa.medicina.content;

import java.util.UUID;

import com.empresa.medicina.auth.AuthenticatedUser;
import com.empresa.medicina.common.ApiException;
import com.empresa.medicina.common.PageResponse;
import com.empresa.medicina.media.MediaService;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catálogo y medios públicos. Funcionan con o sin sesión; la autorización depende de la política de cada contenido.
 */
@RestController
@RequestMapping("/api/v1")
class CatalogController {

	private final CatalogService catalogService;

	private final MediaService mediaService;

	CatalogController(CatalogService catalogService, MediaService mediaService) {
		this.catalogService = catalogService;
		this.mediaService = mediaService;
	}

	@GetMapping("/content")
	PageResponse<CatalogDtos.Ficha> buscar(@AuthenticationPrincipal AuthenticatedUser user,
			@RequestParam(name = "q", required = false) String q,
			@RequestParam(name = "tipo", required = false) TipoContenido tipo,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "12") int size) {
		return this.catalogService.buscar(this.catalogService.lector(user), q, tipo, page, size);
	}

	@GetMapping("/content/{slug}")
	CatalogDtos.Lectura detalle(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String slug) {
		return this.catalogService.abrir(this.catalogService.lector(user), slug);
	}

	/**
	 * Entrega un medio sólo si está aprobado y lo usa una publicación vigente que el visitante puede leer. Los
	 * archivos pendientes nunca salen por aquí.
	 */
	@GetMapping("/media/{id}")
	ResponseEntity<byte[]> medio(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@RequestParam(required = false) String variante) {
		if (!this.mediaService.estaAprobado(id) || !this.catalogService.puedeVerMedio(this.catalogService.lector(user), id)) {
			throw ApiException.notFound("MEDIO_NO_ENCONTRADO", "El archivo no existe o no está disponible.");
		}
		MediaService.Archivo archivo = this.mediaService.archivo(id, variante, false);
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(archivo.tipoMime()))
			.cacheControl(CacheControl.maxAge(java.time.Duration.ofMinutes(10)).cachePrivate())
			.header("X-Content-Type-Options", "nosniff")
			.header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; sandbox")
			.body(archivo.contenido());
	}

}
