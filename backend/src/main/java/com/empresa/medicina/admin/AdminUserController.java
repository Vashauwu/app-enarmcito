package com.empresa.medicina.admin;

import java.time.LocalDate;
import java.util.UUID;

import com.empresa.medicina.auth.AuthenticatedUser;
import com.empresa.medicina.auth.Rol;
import com.empresa.medicina.common.PageResponse;
import com.empresa.medicina.users.UserService;
import com.empresa.medicina.users.UsuarioSistema;
import com.empresa.medicina.users.UsuarioSistema.EstadoCuenta;
import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/usuarios")
class AdminUserController {

	private final AdminUserService adminUserService;

	private final UserService userService;

	AdminUserController(AdminUserService adminUserService, UserService userService) {
		this.adminUserService = adminUserService;
		this.userService = userService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('users.read')")
	PageResponse<AdminUserDtos.Fila> listar(@AuthenticationPrincipal AuthenticatedUser user,
			@RequestParam(required = false) String q, @RequestParam(required = false) Rol rol,
			@RequestParam(required = false) Long tierId, @RequestParam(required = false) EstadoCuenta estado,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate altaDesde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate altaHasta,
			@RequestParam(required = false) String orden, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return this.adminUserService.listar(actor(user),
				new AdminUserService.Filtro(q, rol, tierId, estado, altaDesde, altaHasta, null, orden), page, size);
	}

	@GetMapping("/opciones")
	@PreAuthorize("hasAuthority('users.read')")
	AdminUserDtos.Opciones opciones(@AuthenticationPrincipal AuthenticatedUser user) {
		return this.adminUserService.opciones(actor(user));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('users.read')")
	AdminUserDtos.Detalle detalle(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
		return this.adminUserService.detalle(actor(user), id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('users.create')")
	AdminUserDtos.ConEnlace invitar(@AuthenticationPrincipal AuthenticatedUser user,
			@Valid @RequestBody AdminUserDtos.InvitarRequest request) {
		return this.adminUserService.invitar(actor(user), request);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyAuthority('users.update', 'users.tier.assign')")
	AdminUserDtos.Detalle actualizar(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@Valid @RequestBody AdminUserDtos.ActualizarRequest request) {
		return this.adminUserService.actualizar(actor(user), id, request);
	}

	@PostMapping("/{id}/bloquear")
	@PreAuthorize("hasAuthority('users.status.update')")
	AdminUserDtos.Detalle bloquear(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@Valid @RequestBody AdminUserDtos.MotivoRequest request) {
		return this.adminUserService.bloquear(actor(user), id, request.motivo());
	}

	@PostMapping("/{id}/desbloquear")
	@PreAuthorize("hasAuthority('users.status.update')")
	AdminUserDtos.Detalle desbloquear(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@Valid @RequestBody AdminUserDtos.MotivoRequest request) {
		return this.adminUserService.desbloquear(actor(user), id, request.motivo());
	}

	@PutMapping("/{id}/rol")
	@PreAuthorize("hasAuthority('users.role.assign')")
	AdminUserDtos.Detalle cambiarRol(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@Valid @RequestBody AdminUserDtos.RolRequest request) {
		return this.adminUserService.cambiarRol(actor(user), id, request);
	}

	@PostMapping("/{id}/restablecer-acceso")
	@PreAuthorize("hasAuthority('users.access.reset')")
	AdminUserDtos.ConEnlace restablecerAcceso(@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable UUID id, @Valid @RequestBody AdminUserDtos.MotivoRequest request) {
		return this.adminUserService.restablecerAcceso(actor(user), id, request.motivo());
	}

	private UsuarioSistema actor(AuthenticatedUser user) {
		return this.userService.obtenerCuentaActiva(user.getId());
	}

}
