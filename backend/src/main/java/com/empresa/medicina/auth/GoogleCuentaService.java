package com.empresa.medicina.auth;

import java.time.Instant;
import java.util.UUID;

import com.empresa.medicina.audit.AccionAuditoria;
import com.empresa.medicina.audit.Actor;
import com.empresa.medicina.audit.AuditService;
import com.empresa.medicina.audit.TipoRecurso;
import com.empresa.medicina.auth.GoogleOidcClient.GoogleIdentidad;
import com.empresa.medicina.auth.IdentidadExterna.Proveedor;
import com.empresa.medicina.common.ApiException;
import com.empresa.medicina.tiers.TierService;
import com.empresa.medicina.users.UserService;
import com.empresa.medicina.users.Usuario;
import com.empresa.medicina.users.UsuarioRepository;
import com.empresa.medicina.users.UsuarioSistema;
import com.empresa.medicina.users.UsuarioSistemaRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operaciones de base de datos del acceso con Google. Se mantienen en transacciones cortas, separadas de las
 * llamadas de red a Google. Google sólo identifica: la cuenta nueva es siempre un {@code ALUMNO} activo con el
 * tier inicial, y una cuenta existente conserva el rol, el tier y el estado que ya tenía.
 */
@Service
class GoogleCuentaService {

	private static final int NOMBRE_MAX = 80;

	private static final int APELLIDO_MAX = 100;

	private final IdentidadExternaRepository identidadRepository;

	private final UsuarioRepository usuarioRepository;

	private final UsuarioSistemaRepository cuentaRepository;

	private final TierService tierService;

	private final UserService userService;

	private final AuditService auditService;

	GoogleCuentaService(IdentidadExternaRepository identidadRepository, UsuarioRepository usuarioRepository,
			UsuarioSistemaRepository cuentaRepository, TierService tierService, UserService userService,
			AuditService auditService) {
		this.identidadRepository = identidadRepository;
		this.usuarioRepository = usuarioRepository;
		this.cuentaRepository = cuentaRepository;
		this.tierService = tierService;
		this.userService = userService;
		this.auditService = auditService;
	}

	/** Resultado de intentar iniciar sesión con una identidad de Google. */
	sealed interface Acceso {

		/** La cuenta (existente o recién creada) puede iniciar sesión. */
		record Permitido(UsuarioSistema cuenta, boolean cuentaNueva) implements Acceso {
		}

		/** La identidad existe pero la cuenta está bloqueada o pendiente. */
		record CuentaInactiva() implements Acceso {
		}

		/** No hay identidad vinculada, pero el correo ya pertenece a una cuenta: no se vincula en silencio. */
		record CorreoExistente() implements Acceso {
		}

	}

	@Transactional
	Acceso acceder(GoogleIdentidad google) {
		var existente = this.identidadRepository.findByProveedorAndSubject(Proveedor.GOOGLE, google.subject());
		if (existente.isPresent()) {
			IdentidadExterna identidad = existente.get();
			UsuarioSistema cuenta = this.cuentaRepository.findConPerfilYTier(identidad.getUsuario().getId())
				.orElseThrow(() -> new IllegalStateException("Identidad sin cuenta asociada"));
			if (!cuenta.isActiva()) {
				rechazar(cuenta, "Cuenta no activa");
				return new Acceso.CuentaInactiva();
			}
			identidad.registrarInicioSesion(Usuario.normalizarCorreo(google.correo()), google.correoVerificado(),
					Instant.now());
			return new Acceso.Permitido(cuenta, false);
		}

		String correo = Usuario.normalizarCorreo(google.correo());
		if (this.usuarioRepository.existsByCorreo(correo)) {
			// El correo coincide, pero eso no prueba que sea la misma persona que controla la cuenta local.
			this.auditService.registrarIndependiente(null, AccionAuditoria.IDENTIDAD_RECHAZADA, TipoRecurso.USUARIO,
					null, "Google intentó acceder con un correo que ya tiene cuenta local; no se vinculó");
			return new Acceso.CorreoExistente();
		}

		Usuario usuario = this.usuarioRepository
			.saveAndFlush(new Usuario(nombre(google, correo), apellido(google), correo));
		UsuarioSistema cuenta = this.cuentaRepository
			.saveAndFlush(new UsuarioSistema(usuario, null, this.tierService.obtenerTierInicial()));
		IdentidadExterna identidad = new IdentidadExterna(usuario, Proveedor.GOOGLE, google.subject(), correo,
				google.correoVerificado());
		identidad.registrarInicioSesion(correo, google.correoVerificado(), Instant.now());
		this.identidadRepository.save(identidad);
		this.auditService.registrar(Actor.of(cuenta), AccionAuditoria.IDENTIDAD_CREADA, TipoRecurso.USUARIO,
				cuenta.getUsuarioId(), "Cuenta de alumno creada con Google");
		return new Acceso.Permitido(cuenta, true);
	}

	/** Vincula Google a la cuenta que tiene la sesión iniciada (así demuestra que controla la cuenta local). */
	@Transactional
	void vincular(UUID usuarioId, GoogleIdentidad google) {
		UsuarioSistema cuenta = this.userService.obtenerCuentaActiva(usuarioId);
		var porSubject = this.identidadRepository.findByProveedorAndSubject(Proveedor.GOOGLE, google.subject());
		if (porSubject.isPresent()) {
			if (porSubject.get().getUsuario().getId().equals(usuarioId)) {
				return;
			}
			throw new GoogleFlujoException("GOOGLE_YA_VINCULADA");
		}
		if (this.identidadRepository.findByUsuarioAndProveedor(usuarioId, Proveedor.GOOGLE).isPresent()) {
			throw new GoogleFlujoException("GOOGLE_OTRA_CUENTA");
		}
		this.identidadRepository.save(new IdentidadExterna(cuenta.getUsuario(), Proveedor.GOOGLE, google.subject(),
				Usuario.normalizarCorreo(google.correo()), google.correoVerificado()));
		this.auditService.registrar(Actor.of(cuenta), AccionAuditoria.IDENTIDAD_VINCULADA, TipoRecurso.USUARIO,
				usuarioId, "Google vinculado a la cuenta");
	}

	/** Sólo si la cuenta conserva contraseña: no puede quedar sin ningún método de acceso. */
	@Transactional
	void desvincular(UUID usuarioId) {
		UsuarioSistema cuenta = this.userService.obtenerCuentaActiva(usuarioId);
		IdentidadExterna identidad = this.identidadRepository.findByUsuarioAndProveedor(usuarioId, Proveedor.GOOGLE)
			.orElseThrow(() -> ApiException.notFound("IDENTIDAD_NO_ENCONTRADA", "Tu cuenta no tiene Google vinculado."));
		if (cuenta.getPasswordHash() == null) {
			throw new ApiException(HttpStatus.CONFLICT, "SIN_OTRO_METODO_DE_ACCESO",
					"Tu cuenta no tiene contraseña. Define una antes de desvincular Google.");
		}
		this.identidadRepository.delete(identidad);
		this.auditService.registrar(Actor.of(cuenta), AccionAuditoria.IDENTIDAD_DESVINCULADA, TipoRecurso.USUARIO,
				usuarioId, "Google desvinculado de la cuenta");
	}

	@Transactional(readOnly = true)
	IdentidadesResponse identidades(UUID usuarioId, boolean googleDisponible) {
		UsuarioSistema cuenta = this.userService.obtenerCuentaActiva(usuarioId);
		var google = this.identidadRepository.findByUsuarioAndProveedor(usuarioId, Proveedor.GOOGLE);
		return new IdentidadesResponse(googleDisponible, google.isPresent(),
				google.map(IdentidadExterna::getCorreoVerificado).orElse(null), cuenta.getPasswordHash() != null);
	}

	/**
	 * Métodos de acceso de la cuenta. No incluye tokens ni identificadores del proveedor.
	 *
	 * @param googleDisponible si el ambiente tiene Google configurado
	 * @param googleVinculado  si la cuenta ya tiene una identidad de Google
	 * @param googleCorreo     correo con el que Google la identificó
	 * @param tienePassword    si la cuenta puede iniciar sesión con contraseña
	 */
	record IdentidadesResponse(boolean googleDisponible, boolean googleVinculado, String googleCorreo,
			boolean tienePassword) {
	}

	private void rechazar(UsuarioSistema cuenta, String motivo) {
		this.auditService.registrarIndependiente(Actor.of(cuenta), AccionAuditoria.IDENTIDAD_RECHAZADA,
				TipoRecurso.USUARIO, cuenta.getUsuarioId(), "Inicio de sesión con Google rechazado: " + motivo);
	}

	private static String nombre(GoogleIdentidad google, String correo) {
		String valor = limpio(google.nombre());
		if (valor == null) {
			valor = correo.substring(0, correo.indexOf('@'));
		}
		return recortar(valor, NOMBRE_MAX);
	}

	private static String apellido(GoogleIdentidad google) {
		String valor = limpio(google.apellido());
		// El perfil exige apellido; la persona puede completarlo en «Mi perfil».
		return recortar((valor != null) ? valor : "-", APELLIDO_MAX);
	}

	private static String limpio(String valor) {
		return (valor == null || valor.isBlank()) ? null : valor.strip();
	}

	private static String recortar(String valor, int max) {
		return (valor.length() <= max) ? valor : valor.substring(0, max).strip();
	}

	/** Falla controlada del flujo; el código viaja al frontend como {@code ?google=<código>}. */
	static class GoogleFlujoException extends RuntimeException {

		private final String codigo;

		GoogleFlujoException(String codigo) {
			super(codigo);
			this.codigo = codigo;
		}

		String codigo() {
			return this.codigo;
		}

	}

}
