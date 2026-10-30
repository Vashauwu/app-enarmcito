package com.empresa.medicina.auth;

import com.empresa.medicina.common.ApiException;
import com.empresa.medicina.users.UserResponse;
import com.empresa.medicina.users.UserService;
import com.empresa.medicina.users.Usuario;
import com.empresa.medicina.users.UsuarioSistema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registro, inicio de sesión y usuario actual. El cierre de sesión ({@code POST /api/v1/auth/logout}) lo
 * resuelve el filtro de logout de Spring Security.
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

	private final AuthService authService;

	private final UserService userService;

	private final AuthenticationManager authenticationManager;

	private final SessionAuthenticator sessionAuthenticator;

	AuthController(AuthService authService, UserService userService, AuthenticationManager authenticationManager,
			SessionAuthenticator sessionAuthenticator) {
		this.authService = authService;
		this.userService = userService;
		this.authenticationManager = authenticationManager;
		this.sessionAuthenticator = sessionAuthenticator;
	}

	/**
	 * Crea la cuenta e inicia sesión directamente, para llevar al usuario al dashboard.
	 */
	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	UserResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		UsuarioSistema cuenta = this.authService.registrar(request);
		AuthenticatedUser principal = AuthenticatedUser.from(cuenta);
		principal.eraseCredentials();
		this.sessionAuthenticator.iniciarSesion(
				UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()),
				httpRequest, httpResponse);
		return this.authService.registrarInicioSesion(principal.getId());
	}

	@PostMapping("/login")
	UserResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		Authentication authentication;
		try {
			authentication = this.authenticationManager.authenticate(UsernamePasswordAuthenticationToken
				.unauthenticated(Usuario.normalizarCorreo(request.correo()), request.password()));
		}
		catch (AuthenticationException ex) {
			this.authService.registrarIntentoFallido(request.correo());
			// Mensaje genérico: no revela si el correo existe ni si la cuenta está bloqueada.
			throw new ApiException(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS",
					"Correo o contraseña incorrectos.");
		}
		this.sessionAuthenticator.iniciarSesion(authentication, httpRequest, httpResponse);
		AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
		return this.authService.registrarInicioSesion(principal.getId());
	}

	@GetMapping("/me")
	UserResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
		return this.userService.perfil(user.getId());
	}

}
