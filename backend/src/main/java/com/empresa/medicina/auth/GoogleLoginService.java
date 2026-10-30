package com.empresa.medicina.auth;

import java.io.Serializable;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import com.empresa.medicina.auth.GoogleCuentaService.Acceso;
import com.empresa.medicina.auth.GoogleCuentaService.GoogleFlujoException;
import com.empresa.medicina.auth.GoogleOidcClient.GoogleIdentidad;
import com.empresa.medicina.auth.GoogleOidcClient.GoogleOidcException;
import com.empresa.medicina.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

/**
 * Flujo Authorization Code con PKCE. El estado del intento ({@code state}, {@code nonce} y verificador PKCE) vive
 * en la sesión HTTP del navegador que lo inició y se consume una sola vez; Google nunca recibe ni devuelve
 * datos que el backend no pueda comprobar. No se registran códigos, tokens ni el cuerpo de las respuestas.
 */
@Service
class GoogleLoginService {

	private static final Logger log = LoggerFactory.getLogger(GoogleLoginService.class);

	private static final String ATTRIBUTE = GoogleLoginService.class.getName() + ".FLUJO";

	private static final Duration VIGENCIA = Duration.ofMinutes(10);

	private static final SecureRandom RANDOM = new SecureRandom();

	private final AppProperties.Google config;

	private final GoogleOidcClient oidcClient;

	private final GoogleCuentaService cuentaService;

	private final AuthService authService;

	private final SessionAuthenticator sessionAuthenticator;

	private final String frontendOrigin;

	GoogleLoginService(AppProperties properties, GoogleOidcClient oidcClient, GoogleCuentaService cuentaService,
			AuthService authService, SessionAuthenticator sessionAuthenticator) {
		this.config = properties.google();
		this.oidcClient = oidcClient;
		this.cuentaService = cuentaService;
		this.authService = authService;
		this.sessionAuthenticator = sessionAuthenticator;
		this.frontendOrigin = properties.frontendOrigin();
	}

	boolean habilitado() {
		return this.config.habilitado();
	}

	/** Estado de un intento en curso. {@code usuarioId} sólo existe al vincular desde una sesión iniciada. */
	private record Flujo(String state, String nonce, String verifier, UUID usuarioId, Instant expira)
			implements Serializable {
	}

	/**
	 * Prepara el intento y devuelve a dónde redirigir. {@code usuarioId} es la persona con sesión iniciada que
	 * pide vincular Google, o {@code null} para iniciar sesión.
	 */
	URI iniciar(HttpSession session, UUID usuarioId) {
		String state = aleatorio();
		String nonce = aleatorio();
		String verifier = aleatorio();
		session.setAttribute(ATTRIBUTE, new Flujo(state, nonce, verifier, usuarioId, Instant.now().plus(VIGENCIA)));
		return this.oidcClient.urlAutorizacion(state, nonce, desafio(verifier));
	}

	/**
	 * Atiende el retorno de Google y devuelve la ruta del frontend a la que redirigir. Cualquier falla se
	 * convierte en {@code /login?google=<código>}; nunca se expone el detalle ni se deja una sesión a medias.
	 */
	String callback(HttpServletRequest request, HttpServletResponse response, String code, String state,
			String errorGoogle, UUID usuarioActual) {
		HttpSession session = request.getSession(false);
		Flujo flujo = (session != null) ? (Flujo) session.getAttribute(ATTRIBUTE) : null;
		if (session != null) {
			// De un solo uso: un state repetido (replay) ya no encuentra el intento.
			session.removeAttribute(ATTRIBUTE);
		}
		String destinoError = (flujo != null && flujo.usuarioId() != null) ? "/app/perfil" : "/login";

		if (flujo == null || Instant.now().isAfter(flujo.expira()) || !iguales(flujo.state(), state)) {
			return error(destinoError, "GOOGLE_ESTADO_INVALIDO");
		}
		if (errorGoogle != null) {
			// access_denied: la persona canceló en Google.
			return error(destinoError, "access_denied".equals(errorGoogle) ? "GOOGLE_CANCELADO" : "GOOGLE_ERROR");
		}
		if (code == null || code.isBlank()) {
			return error(destinoError, "GOOGLE_ERROR");
		}

		GoogleIdentidad identidad;
		try {
			identidad = this.oidcClient.canjear(code, flujo.verifier(), flujo.nonce());
		}
		catch (GoogleOidcException ex) {
			log.warn("Inicio de sesión con Google rechazado: {}", ex.getMessage());
			return error(destinoError, "GOOGLE_ERROR");
		}
		if (!identidad.correoVerificado()) {
			return error(destinoError, "GOOGLE_CORREO_NO_VERIFICADO");
		}

		try {
			return (flujo.usuarioId() != null) ? vincular(flujo, identidad, usuarioActual)
					: iniciarSesion(request, response, identidad);
		}
		catch (GoogleFlujoException ex) {
			return error(destinoError, ex.codigo());
		}
		catch (DataIntegrityViolationException ex) {
			// Dos intentos simultáneos con la misma identidad o correo: decide la restricción única.
			return error(destinoError, "GOOGLE_CORREO_EXISTENTE");
		}
	}

	private String vincular(Flujo flujo, GoogleIdentidad identidad, UUID usuarioActual) {
		// La sesión debe seguir siendo de quien pidió vincular; si cambió, no se vincula a otra cuenta.
		if (usuarioActual == null || !usuarioActual.equals(flujo.usuarioId())) {
			return error("/login", "GOOGLE_ESTADO_INVALIDO");
		}
		this.cuentaService.vincular(usuarioActual, identidad);
		return this.frontendOrigin + "/app/perfil?google=vinculada";
	}

	private String iniciarSesion(HttpServletRequest request, HttpServletResponse response,
			GoogleIdentidad identidad) {
		Acceso acceso = this.cuentaService.acceder(identidad);
		if (acceso instanceof Acceso.CuentaInactiva) {
			return error("/login", "GOOGLE_CUENTA_INACTIVA");
		}
		if (acceso instanceof Acceso.Permitido permitido) {
			AuthenticatedUser principal = AuthenticatedUser.from(permitido.cuenta());
			principal.eraseCredentials();
			this.sessionAuthenticator.iniciarSesion(
					UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()),
					request, response);
			this.authService.registrarInicioSesion(principal.getId());
			return this.frontendOrigin + "/app";
		}
		return error("/login", "GOOGLE_CORREO_EXISTENTE");
	}

	private String error(String ruta, String codigo) {
		return this.frontendOrigin + ruta + "?google=" + codigo;
	}

	private static String aleatorio() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static String desafio(String verifier) {
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static boolean iguales(String esperado, String recibido) {
		return recibido != null && MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8),
				recibido.getBytes(StandardCharsets.UTF_8));
	}

}
