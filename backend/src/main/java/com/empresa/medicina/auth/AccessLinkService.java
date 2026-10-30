package com.empresa.medicina.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import com.empresa.medicina.audit.AccionAuditoria;
import com.empresa.medicina.audit.Actor;
import com.empresa.medicina.audit.AuditService;
import com.empresa.medicina.audit.TipoRecurso;
import com.empresa.medicina.common.ApiError;
import com.empresa.medicina.common.ApiException;
import com.empresa.medicina.config.AppProperties;
import com.empresa.medicina.users.Usuario;
import com.empresa.medicina.users.UsuarioSistema;
import com.empresa.medicina.users.UsuarioSistema.EstadoCuenta;
import com.empresa.medicina.users.UsuarioSistemaRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enlaces de un solo uso para activar una cuenta invitada o restablecer el acceso. El enlace se entrega una
 * sola vez a quien lo genera, que lo hace llegar por un canal propio; el sistema nunca conoce ni reenvía la
 * contraseña elegida. El token va en el fragmento de la URL ({@code #token=}) para que no llegue a los logs de
 * servidores o proxies.
 */
@Service
public class AccessLinkService {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final TokenAccesoRepository tokenRepository;

	private final UsuarioSistemaRepository cuentaRepository;

	private final PasswordEncoder passwordEncoder;

	private final AuditService auditService;

	private final AppProperties properties;

	AccessLinkService(TokenAccesoRepository tokenRepository, UsuarioSistemaRepository cuentaRepository,
			PasswordEncoder passwordEncoder, AuditService auditService, AppProperties properties) {
		this.tokenRepository = tokenRepository;
		this.cuentaRepository = cuentaRepository;
		this.passwordEncoder = passwordEncoder;
		this.auditService = auditService;
		this.properties = properties;
	}

	/** Genera un enlace nuevo e invalida los anteriores. Invitación si la cuenta está pendiente. */
	@Transactional
	public Enlace generar(UsuarioSistema cuenta, Usuario creadoPor) {
		Instant ahora = Instant.now();
		this.tokenRepository.invalidarPendientes(cuenta.getUsuarioId(), ahora);
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		TokenAcceso.Tipo tipo = (cuenta.getEstado() == EstadoCuenta.PENDIENTE) ? TokenAcceso.Tipo.INVITACION
				: TokenAcceso.Tipo.RESTABLECIMIENTO;
		Instant expira = ahora.plus(Duration.ofHours(this.properties.admin().accessLinkHours()));
		this.tokenRepository.save(new TokenAcceso(cuenta.getUsuario(), tipo, hash(token), expira, creadoPor));
		return new Enlace(this.properties.frontendOrigin() + "/acceso#token=" + token, tipo.name(), expira);
	}

	@Transactional(readOnly = true)
	public InfoEnlace consultar(String token) {
		TokenAcceso acceso = usable(token);
		Usuario usuario = acceso.getUsuario();
		return new InfoEnlace(acceso.getTipo().name(), usuario.getNombre(), usuario.getCorreo(), acceso.getExpiraAt());
	}

	@Transactional
	public void definirPassword(String token, String password) {
		TokenAcceso acceso = usable(token);
		UsuarioSistema cuenta = this.cuentaRepository.findConPerfilYTier(acceso.getUsuario().getId())
			.orElseThrow(AccessLinkService::invalido);
		if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDACION", "Revisa los datos marcados en el formulario.",
					java.util.List.of(new ApiError.FieldError("password", "La contraseña es demasiado larga.")));
		}
		Instant ahora = Instant.now();
		acceso.marcarUsado(ahora);
		cuenta.definirPassword(this.passwordEncoder.encode(password));
		this.auditService.registrar(Actor.of(cuenta), AccionAuditoria.ACCESO_ENLACE_USADO, TipoRecurso.USUARIO,
				cuenta.getUsuarioId(), "Contraseña definida mediante enlace de " + acceso.getTipo().name().toLowerCase());
	}

	private TokenAcceso usable(String token) {
		return this.tokenRepository.findByTokenHash(hash(token))
			.filter((acceso) -> acceso.esUsable(Instant.now()))
			.orElseThrow(AccessLinkService::invalido);
	}

	private static ApiException invalido() {
		return new ApiException(HttpStatus.BAD_REQUEST, "ENLACE_INVALIDO",
				"El enlace no es válido o ya expiró. Pide uno nuevo a la persona que te lo envió.");
	}

	static String hash(String token) {
		try {
			return HexFormat.of()
				.formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/** Enlace que se muestra una sola vez. */
	public record Enlace(String url, String tipo, Instant expiraAt) {
	}

	public record InfoEnlace(String tipo, String nombre, String correo, Instant expiraAt) {
	}

}
