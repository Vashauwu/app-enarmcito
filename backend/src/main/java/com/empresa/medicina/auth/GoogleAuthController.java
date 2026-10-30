package com.empresa.medicina.auth;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import com.empresa.medicina.auth.GoogleCuentaService.IdentidadesResponse;
import com.empresa.medicina.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inicio de sesión y vinculación con Google. {@code iniciar} y {@code callback} son navegaciones del
 * navegador (GET con redirección), no llamadas XHR: por eso responden con redirecciones al frontend.
 */
@RestController
class GoogleAuthController {

	private final GoogleLoginService loginService;

	private final GoogleCuentaService cuentaService;

	private final String frontendOrigin;

	GoogleAuthController(GoogleLoginService loginService, GoogleCuentaService cuentaService,
			AppProperties properties) {
		this.loginService = loginService;
		this.cuentaService = cuentaService;
		this.frontendOrigin = properties.frontendOrigin();
	}

	/** Qué proveedores están configurados en este ambiente (para mostrar u ocultar el botón). */
	@GetMapping("/api/v1/auth/proveedores")
	Map<String, Boolean> proveedores() {
		return Map.of("google", this.loginService.habilitado());
	}

	/**
	 * {@code modo=vincular} exige sesión iniciada y añade Google a esa cuenta; sin él, inicia sesión o crea la
	 * cuenta.
	 */
	@GetMapping("/api/v1/auth/google/iniciar")
	ResponseEntity<Void> iniciar(@RequestParam(required = false) String modo, HttpServletRequest request,
			Authentication authentication) {
		boolean vincular = "vincular".equals(modo);
		UUID usuarioId = usuarioId(authentication);
		if (!this.loginService.habilitado()) {
			return redirigir(this.frontendOrigin + (vincular ? "/app/perfil" : "/login") + "?google=GOOGLE_NO_DISPONIBLE");
		}
		if (vincular && usuarioId == null) {
			return redirigir(this.frontendOrigin + "/login?google=GOOGLE_ESTADO_INVALIDO");
		}
		return redirigir(this.loginService.iniciar(request.getSession(true), vincular ? usuarioId : null).toString());
	}

	@GetMapping("/api/v1/auth/google/callback")
	ResponseEntity<Void> callback(@RequestParam(required = false) String code,
			@RequestParam(required = false) String state, @RequestParam(required = false) String error,
			HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
		return redirigir(this.loginService.callback(request, response, code, state, error,
				usuarioId(authentication)));
	}

	@GetMapping("/api/v1/me/identidades")
	IdentidadesResponse identidades(@AuthenticationPrincipal AuthenticatedUser user) {
		return this.cuentaService.identidades(user.getId(), this.loginService.habilitado());
	}

	@PostMapping("/api/v1/me/identidades/google/desvincular")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void desvincular(@AuthenticationPrincipal AuthenticatedUser user) {
		this.cuentaService.desvincular(user.getId());
	}

	private static UUID usuarioId(Authentication authentication) {
		return (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user)
				? user.getId() : null;
	}

	private static ResponseEntity<Void> redirigir(String destino) {
		return ResponseEntity.status(HttpStatus.FOUND)
			.header(HttpHeaders.LOCATION, URI.create(destino).toString())
			.header(HttpHeaders.CACHE_CONTROL, "no-store")
			.build();
	}

}
