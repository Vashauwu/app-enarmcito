package com.empresa.medicina.content;

import java.util.List;
import java.util.UUID;

import com.empresa.medicina.auth.AuthenticatedUser;
import com.empresa.medicina.common.PageResponse;
import com.empresa.medicina.users.UserService;
import com.empresa.medicina.users.UsuarioSistema;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestión editorial y clínica. Cada transición es un endpoint explícito: el estado nunca se edita libremente. Las
 * anotaciones exigen el permiso mínimo; {@link EditorialPolicy} añade tipo, autoría, asignación y estado.
 */
@RestController
@RequestMapping("/api/v1/admin/contenido")
class EditorialController {

	private static final String EDITA = "hasAnyAuthority('content.update.own', 'content.update.any', "
			+ "'guides.update.own', 'guides.update.any')";

	private final EditorialService editorialService;

	private final UserService userService;

	EditorialController(EditorialService editorialService, UserService userService) {
		this.editorialService = editorialService;
		this.userService = userService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('content.read.admin')")
	PageResponse<EditorialResponses.Fila> listar(@AuthenticationPrincipal AuthenticatedUser user,
			@RequestParam(required = false) String q, @RequestParam(required = false) TipoContenido tipo,
			@RequestParam(required = false) EstadoContenido estado,
			@RequestParam(required = false) PoliticaAcceso politica, @RequestParam(required = false) String area,
			@RequestParam(required = false) UUID autorId, @RequestParam(required = false) UUID revisorId,
			@RequestParam(required = false) Boolean revisionVencida, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return this.editorialService.listar(actor(user),
				new EditorialService.Filtro(q, tipo, estado, politica, area, autorId, revisorId, revisionVencida), page,
				size);
	}

	@GetMapping("/areas")
	@PreAuthorize("hasAuthority('content.read.admin')")
	List<String> areas() {
		return this.editorialService.areasMedicas();
	}

	@GetMapping("/responsables")
	@PreAuthorize("hasAnyAuthority('content.create', 'guides.create', 'content.assign')")
	EditorialResponses.Candidatos responsables() {
		return this.editorialService.candidatos();
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('content.read.admin')")
	EditorialResponses.Detalle detalle(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
		return this.editorialService.detalle(actor(user), id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyAuthority('content.create', 'guides.create')")
	EditorialResponses.Detalle crear(@AuthenticationPrincipal AuthenticatedUser user,
			@Valid @RequestBody ContenidoRequest request) {
		return this.editorialService.crear(actor(user), request);
	}

	@PutMapping("/{id}")
	@PreAuthorize(EDITA)
	EditorialResponses.Detalle actualizar(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@Valid @RequestBody ContenidoRequest request) {
		return this.editorialService.actualizar(actor(user), id, request);
	}

	@PutMapping("/{id}/tipo")
	@PreAuthorize("hasAuthority('content.type.assign')")
	EditorialResponses.Detalle cambiarTipo(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@Valid @RequestBody EditorialRequests.CambioTipo request) {
		return this.editorialService.cambiarTipo(actor(user), id, request);
	}

	@PutMapping("/{id}/acceso")
	@PreAuthorize("hasAuthority('content.access-policy.assign')")
	EditorialResponses.Detalle cambiarAcceso(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@Valid @RequestBody EditorialRequests.Acceso request) {
		return this.editorialService.cambiarAcceso(actor(user), id, request);
	}

	@PutMapping("/{id}/responsables")
	@PreAuthorize("hasAuthority('content.assign')")
	EditorialResponses.Detalle responsables(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@Valid @RequestBody EditorialRequests.Responsables request) {
		return this.editorialService.asignarResponsables(actor(user), id, request);
	}

	// ----- Bloques -----

	@PostMapping("/{id}/bloques")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('blocks.create')")
	EditorialResponses.Detalle crearBloque(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@Valid @RequestBody EditorialRequests.Bloque request) {
		return this.editorialService.crearBloque(actor(user), id, request);
	}

	@PutMapping("/{id}/bloques/{bloqueId}")
	@PreAuthorize("hasAnyAuthority('blocks.update.own', 'blocks.update.any')")
	EditorialResponses.Detalle actualizarBloque(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@PathVariable UUID bloqueId, @Valid @RequestBody EditorialRequests.Bloque request) {
		return this.editorialService.actualizarBloque(actor(user), id, bloqueId, request);
	}

	@DeleteMapping("/{id}/bloques/{bloqueId}")
	@PreAuthorize("hasAnyAuthority('blocks.update.own', 'blocks.update.any')")
	EditorialResponses.Detalle retirarBloque(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@PathVariable UUID bloqueId) {
		return this.editorialService.retirarBloque(actor(user), id, bloqueId);
	}

	@PutMapping("/{id}/bloques/orden")
	@PreAuthorize("hasAuthority('blocks.reorder')")
	EditorialResponses.Detalle reordenar(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@Valid @RequestBody EditorialRequests.OrdenBloques request) {
		return this.editorialService.reordenarBloques(actor(user), id, request);
	}

	// ----- Revisión y publicación -----

	@PostMapping("/{id}/enviar-revision")
	@PreAuthorize(EDITA)
	EditorialResponses.Detalle enviarARevision(@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable UUID id, @Valid @RequestBody EditorialRequests.EnviarRevision request) {
		return this.editorialService.enviarARevision(actor(user), id, request);
	}

	@GetMapping("/{id}/revisiones")
	@PreAuthorize("hasAuthority('content.read.admin')")
	List<EditorialResponses.Revision> revisiones(@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable UUID id) {
		return this.editorialService.revisiones(actor(user), id);
	}

	@PostMapping("/{id}/revisiones")
	@PreAuthorize("hasAnyAuthority('content.update.any', 'guides.update.any', 'content.review', "
			+ "'guides.clinical-review')")
	EditorialResponses.Detalle revisar(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@Valid @RequestBody EditorialRequests.Revision request) {
		return this.editorialService.revisar(actor(user), id, request);
	}

	@PostMapping("/{id}/publicar")
	@PreAuthorize("hasAnyAuthority('content.publish', 'guides.publish')")
	EditorialResponses.Detalle publicar(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
		return this.editorialService.publicar(actor(user), id);
	}

	@PostMapping("/{id}/revision-requerida")
	@PreAuthorize("hasAnyAuthority('content.update.any', 'guides.update.any')")
	EditorialResponses.Detalle marcarRevisionRequerida(@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable UUID id, @Valid @RequestBody EditorialRequests.ConMotivo request) {
		return this.editorialService.marcarRevisionRequerida(actor(user), id, request.motivo());
	}

	@PostMapping("/{id}/archivar")
	@PreAuthorize("hasAnyAuthority('content.archive', 'content.archive.own', 'guides.archive')")
	EditorialResponses.Detalle archivar(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@Valid @RequestBody EditorialRequests.ConMotivo request) {
		return this.editorialService.archivar(actor(user), id, request.motivo());
	}

	@PostMapping("/{id}/restaurar")
	@PreAuthorize("hasAnyAuthority('content.archive', 'content.archive.own', 'guides.archive')")
	EditorialResponses.Detalle restaurar(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@Valid @RequestBody EditorialRequests.ConMotivo request) {
		return this.editorialService.restaurar(actor(user), id, request.motivo());
	}

	private UsuarioSistema actor(AuthenticatedUser user) {
		return this.userService.obtenerCuentaActiva(user.getId());
	}

}
