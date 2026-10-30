package com.empresa.medicina.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.empresa.medicina.common.ApiError;
import com.empresa.medicina.common.ApiException;
import com.empresa.medicina.config.AppProperties;
import com.empresa.medicina.tiers.Tier;
import com.empresa.medicina.tiers.TierService;
import com.empresa.medicina.users.UserResponse;
import com.empresa.medicina.users.UserService;
import com.empresa.medicina.users.Usuario;
import com.empresa.medicina.users.UsuarioRepository;
import com.empresa.medicina.users.UsuarioSistema;
import com.empresa.medicina.users.UsuarioSistemaRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AuthService {

	static final int PASSWORD_MIN_LENGTH = 8;

	/** BCrypt sólo procesa los primeros 72 bytes; se rechaza cualquier contraseña más larga. */
	static final int PASSWORD_MAX_LENGTH = 72;

	private final UsuarioRepository usuarioRepository;

	private final UsuarioSistemaRepository cuentaRepository;

	private final TierService tierService;

	private final UserService userService;

	private final PasswordEncoder passwordEncoder;

	private final AppProperties properties;

	private final AuditoriaSesionHandler auditoriaSesion;

	AuthService(UsuarioRepository usuarioRepository, UsuarioSistemaRepository cuentaRepository,
			TierService tierService, UserService userService, PasswordEncoder passwordEncoder,
			AppProperties properties, AuditoriaSesionHandler auditoriaSesion) {
		this.usuarioRepository = usuarioRepository;
		this.cuentaRepository = cuentaRepository;
		this.tierService = tierService;
		this.userService = userService;
		this.passwordEncoder = passwordEncoder;
		this.properties = properties;
		this.auditoriaSesion = auditoriaSesion;
	}

	@Transactional
	UsuarioSistema registrar(RegisterRequest request) {
		String correo = Usuario.normalizarCorreo(request.correo());
		if (this.usuarioRepository.existsByCorreo(correo)) {
			throw correoDuplicado();
		}
		if (request.password().getBytes(StandardCharsets.UTF_8).length > PASSWORD_MAX_LENGTH) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDACION", "Revisa los datos marcados en el formulario.",
					List.of(new ApiError.FieldError("password", "La contraseña es demasiado larga.")));
		}
		Tier tier = asignarTier(request.tierId());
		try {
			Usuario usuario = this.usuarioRepository.saveAndFlush(
					new Usuario(request.nombre(), request.apellido(), correo));
			return this.cuentaRepository
				.saveAndFlush(new UsuarioSistema(usuario, this.passwordEncoder.encode(request.password()), tier));
		}
		catch (DataIntegrityViolationException ex) {
			// Dos registros simultáneos con el mismo correo: la restricción única decide.
			throw correoDuplicado();
		}
	}

	/**
	 * El backend es la autoridad final sobre el tier. La selección libre sólo se permite cuando
	 * {@code app.registration.allow-tier-selection=true} (pruebas locales); en caso contrario se asigna el
	 * tier de menor acceso.
	 */
	private Tier asignarTier(Long tierSolicitado) {
		if (this.properties.registration().allowTierSelection()) {
			return this.tierService.obtenerActivo(tierSolicitado);
		}
		return this.tierService.obtenerTierInicial();
	}

	@Transactional
	UserResponse registrarInicioSesion(UUID usuarioId) {
		UsuarioSistema cuenta = this.userService.obtenerCuentaActiva(usuarioId);
		cuenta.registrarInicioSesion(Instant.now());
		this.cuentaRepository.save(cuenta);
		this.auditoriaSesion.inicioExitoso(cuenta);
		return UserResponse.from(cuenta);
	}

	void registrarIntentoFallido(String correo) {
		this.auditoriaSesion.inicioFallido(Usuario.normalizarCorreo(correo));
	}

	private static ApiException correoDuplicado() {
		return new ApiException(HttpStatus.CONFLICT, "CORREO_DUPLICADO", "Ya existe una cuenta con ese correo.",
				List.of(new ApiError.FieldError("correo", "Ya existe una cuenta con ese correo.")));
	}

}
