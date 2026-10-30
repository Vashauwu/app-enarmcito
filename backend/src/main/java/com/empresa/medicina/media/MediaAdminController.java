package com.empresa.medicina.media;

import java.util.UUID;

import com.empresa.medicina.auth.AuthenticatedUser;
import com.empresa.medicina.common.PageResponse;
import com.empresa.medicina.users.UserService;
import com.empresa.medicina.users.UsuarioSistema;
import jakarta.validation.Valid;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Biblioteca de medios del panel. La vista previa de archivos pendientes sólo se sirve aquí, nunca por rutas públicas.
 */
@RestController
@RequestMapping("/api/v1/admin/media")
class MediaAdminController {

	private final MediaService mediaService;

	private final UserService userService;

	MediaAdminController(MediaService mediaService, UserService userService) {
		this.mediaService = mediaService;
		this.userService = userService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('media.upload')")
	PageResponse<MediaDtos.Medio> listar(@AuthenticationPrincipal AuthenticatedUser user,
			@RequestParam(required = false) EstadoMedio estado, @RequestParam(required = false) String tipo,
			@RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "24") int size) {
		return this.mediaService.listar(actor(user), estado, tipo, q, page, size);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('media.upload')")
	MediaDtos.Medio detalle(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
		return this.mediaService.detalle(actor(user), id);
	}

	/** Vista previa autenticada; comprueba que la persona puede ver el medio. */
	@GetMapping("/{id}/archivo")
	@PreAuthorize("hasAnyAuthority('media.upload', 'content.read.admin')")
	ResponseEntity<byte[]> archivo(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@RequestParam(required = false) String variante) {
		UsuarioSistema actor = actor(user);
		if (actor.getRol().tiene(com.empresa.medicina.auth.Permiso.MEDIA_UPLOAD)) {
			this.mediaService.detalle(actor, id);
		}
		MediaService.Archivo archivo = this.mediaService.archivo(id, variante, true);
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(archivo.tipoMime()))
			.cacheControl(CacheControl.noStore())
			.header("X-Content-Type-Options", "nosniff")
			.header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; sandbox")
			.header(HttpHeaders.CONTENT_DISPOSITION, "inline")
			.body(archivo.contenido());
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('media.upload')")
	MediaDtos.Medio cargar(@AuthenticationPrincipal AuthenticatedUser user, @RequestPart("archivo") MultipartFile archivo,
			@Valid @RequestPart("metadatos") MediaDtos.MetadatosRequest metadatos) {
		return this.mediaService.cargar(actor(user), archivo, metadatos);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('media.upload')")
	MediaDtos.Medio actualizar(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@Valid @RequestBody MediaDtos.MetadatosRequest metadatos) {
		return this.mediaService.actualizar(actor(user), id, metadatos);
	}

	@PostMapping("/{id}/aprobar")
	@PreAuthorize("hasAuthority('media.publish')")
	MediaDtos.Medio aprobar(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
		return this.mediaService.aprobar(actor(user), id);
	}

	@PostMapping("/{id}/rechazar")
	@PreAuthorize("hasAuthority('media.review')")
	MediaDtos.Medio rechazar(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@Valid @RequestBody MediaDtos.MotivoRequest request) {
		return this.mediaService.rechazar(actor(user), id, request.motivo());
	}

	@PostMapping("/{id}/archivar")
	@PreAuthorize("hasAuthority('media.archive')")
	MediaDtos.Medio archivar(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@Valid @RequestBody MediaDtos.MotivoRequest request) {
		return this.mediaService.archivar(actor(user), id, request.motivo());
	}

	private UsuarioSistema actor(AuthenticatedUser user) {
		return this.userService.obtenerCuentaActiva(user.getId());
	}

}
