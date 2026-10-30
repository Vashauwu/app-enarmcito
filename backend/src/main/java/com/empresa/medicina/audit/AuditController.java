package com.empresa.medicina.audit;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import com.empresa.medicina.auth.AuthenticatedUser;
import com.empresa.medicina.common.PageResponse;
import com.empresa.medicina.common.Paginacion;
import com.empresa.medicina.users.UserService;
import com.empresa.medicina.users.UsuarioSistema;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bitácora de sólo lectura: no existe ningún endpoint para modificar o eliminar eventos.
 */
@RestController
@RequestMapping("/api/v1/admin/auditoria")
@PreAuthorize("hasAnyAuthority('audit.read', 'audit.read.editorial', 'audit.read.own')")
class AuditController {

	private final AuditQueryService auditQueryService;

	private final UserService userService;

	AuditController(AuditQueryService auditQueryService, UserService userService) {
		this.auditQueryService = auditQueryService;
		this.userService = userService;
	}

	@GetMapping
	PageResponse<AuditEventResponse> buscar(@AuthenticationPrincipal AuthenticatedUser user,
			@RequestParam(required = false) AccionAuditoria accion,
			@RequestParam(required = false) TipoRecurso recursoTipo,
			@RequestParam(required = false) String recursoId, @RequestParam(required = false) String actor,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		UsuarioSistema cuenta = this.userService.obtenerCuentaActiva(user.getId());
		AuditQueryService.Filtro filtro = new AuditQueryService.Filtro(accion, recursoTipo,
				Paginacion.busqueda(recursoId, 64), Paginacion.busqueda(actor, 254), desde, hasta);
		return this.auditQueryService.buscar(cuenta.getUsuarioId(), cuenta.getRol(), filtro, page, size);
	}

	/** Catálogo de acciones para los filtros de la interfaz. */
	@GetMapping("/acciones")
	List<AccionResponse> acciones() {
		return Arrays.stream(AccionAuditoria.values())
			.map((accion) -> new AccionResponse(accion.name(), accion.descripcion()))
			.toList();
	}

	record AccionResponse(String codigo, String descripcion) {
	}

}
