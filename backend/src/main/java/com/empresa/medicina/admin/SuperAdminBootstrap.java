package com.empresa.medicina.admin;

import com.empresa.medicina.audit.AccionAuditoria;
import com.empresa.medicina.audit.AuditService;
import com.empresa.medicina.audit.Cambios;
import com.empresa.medicina.audit.TipoRecurso;
import com.empresa.medicina.auth.Rol;
import com.empresa.medicina.config.AppProperties;
import com.empresa.medicina.tiers.TierService;
import com.empresa.medicina.users.Usuario;
import com.empresa.medicina.users.UsuarioRepository;
import com.empresa.medicina.users.UsuarioSistema;
import com.empresa.medicina.users.UsuarioSistema.EstadoCuenta;
import com.empresa.medicina.users.UsuarioSistemaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Garantiza un SUPER_ADMIN inicial fuera del entorno demo. Si no existe ninguno activo y se definieron
 * {@code APP_BOOTSTRAP_SUPERADMIN_EMAIL} y {@code APP_BOOTSTRAP_SUPERADMIN_PASSWORD}, crea la cuenta (o promueve
 * una existente con ese correo). Las variables deben retirarse después del primer arranque.
 */
@Component
class SuperAdminBootstrap implements ApplicationRunner {

	private static final Logger logger = LoggerFactory.getLogger(SuperAdminBootstrap.class);

	private final UsuarioSistemaRepository cuentaRepository;

	private final UsuarioRepository usuarioRepository;

	private final TierService tierService;

	private final PasswordEncoder passwordEncoder;

	private final AuditService auditService;

	private final AppProperties properties;

	SuperAdminBootstrap(UsuarioSistemaRepository cuentaRepository, UsuarioRepository usuarioRepository,
			TierService tierService, PasswordEncoder passwordEncoder, AuditService auditService,
			AppProperties properties) {
		this.cuentaRepository = cuentaRepository;
		this.usuarioRepository = usuarioRepository;
		this.tierService = tierService;
		this.passwordEncoder = passwordEncoder;
		this.auditService = auditService;
		this.properties = properties;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (this.cuentaRepository.countByRolAndEstado(Rol.SUPER_ADMIN, EstadoCuenta.ACTIVO) > 0) {
			return;
		}
		String correo = this.properties.admin().bootstrapSuperAdminEmail();
		String password = this.properties.admin().bootstrapSuperAdminPassword();
		if (correo == null || correo.isBlank() || password == null || password.length() < 12) {
			logger.warn("No hay ningún SUPER_ADMIN activo. Define APP_BOOTSTRAP_SUPERADMIN_EMAIL y "
					+ "APP_BOOTSTRAP_SUPERADMIN_PASSWORD (12 caracteres o más) para crear el primero.");
			return;
		}
		String normalizado = Usuario.normalizarCorreo(correo);
		UsuarioSistema cuenta = this.cuentaRepository.findByCorreo(normalizado).orElseGet(() -> {
			Usuario usuario = this.usuarioRepository.saveAndFlush(new Usuario("Super", "Administrador", normalizado));
			return UsuarioSistema.invitada(usuario, Rol.SUPER_ADMIN, this.tierService.obtenerTierInicial());
		});
		Rol anterior = cuenta.getRol();
		cuenta.cambiarRol(Rol.SUPER_ADMIN);
		cuenta.definirPassword(this.passwordEncoder.encode(password));
		if (cuenta.getEstado() == EstadoCuenta.BLOQUEADO) {
			cuenta.desbloquear();
		}
		this.cuentaRepository.saveAndFlush(cuenta);
		this.auditService.registrar(null, AccionAuditoria.SUPER_ADMIN_INICIAL, TipoRecurso.USUARIO,
				cuenta.getUsuarioId(), "SUPER_ADMIN inicial configurado para " + normalizado, null,
				Cambios.nuevo().campo("rol", (anterior == Rol.SUPER_ADMIN) ? null : anterior, Rol.SUPER_ADMIN));
		logger.info("SUPER_ADMIN inicial configurado. Retira APP_BOOTSTRAP_SUPERADMIN_PASSWORD del entorno.");
	}

}
