package com.empresa.medicina.admin;

import com.empresa.medicina.auth.AuthenticatedUser;
import com.empresa.medicina.auth.Rol;
import com.empresa.medicina.users.UserService;
import com.empresa.medicina.users.UsuarioSistema.EstadoCuenta;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
class AdminInsightsController {

	private final AdminInsightsService insightsService;

	private final UserService userService;

	AdminInsightsController(AdminInsightsService insightsService, UserService userService) {
		this.insightsService = insightsService;
		this.userService = userService;
	}

	@GetMapping("/dashboard")
	@PreAuthorize("hasAuthority('admin.access')")
	AdminInsightsService.Dashboard dashboard(@AuthenticationPrincipal AuthenticatedUser user) {
		return this.insightsService.dashboard(this.userService.obtenerCuentaActiva(user.getId()));
	}

	@GetMapping("/actividad/usuarios")
	@PreAuthorize("hasAuthority('users.read')")
	AdminInsightsService.Actividad actividad(@AuthenticationPrincipal AuthenticatedUser user,
			@RequestParam(defaultValue = "30") int dias, @RequestParam(required = false) Rol rol,
			@RequestParam(required = false) Long tierId, @RequestParam(required = false) EstadoCuenta estado,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return this.insightsService.actividad(this.userService.obtenerCuentaActiva(user.getId()), dias, rol, tierId,
				estado, page, size);
	}

}
