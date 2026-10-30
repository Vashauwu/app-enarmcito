package com.empresa.medicina.auth;

import com.empresa.medicina.audit.AccionAuditoria;
import com.empresa.medicina.audit.Actor;
import com.empresa.medicina.audit.AuditService;
import com.empresa.medicina.audit.TipoRecurso;
import com.empresa.medicina.users.UsuarioSistema;
import com.empresa.medicina.users.UsuarioSistemaRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

/**
 * Audita inicios, cierres e intentos fallidos de sesión de cuentas con acceso al panel. Las sesiones de
 * alumnos no se registran en la bitácora.
 */
@Component("auditoriaLogoutHandler")
class AuditoriaSesionHandler implements LogoutHandler {

	private final AuditService auditService;

	private final UsuarioSistemaRepository cuentaRepository;

	AuditoriaSesionHandler(AuditService auditService, UsuarioSistemaRepository cuentaRepository) {
		this.auditService = auditService;
		this.cuentaRepository = cuentaRepository;
	}

	@Override
	public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
			this.cuentaRepository.findConPerfilYTier(user.getId())
				.filter(AuditoriaSesionHandler::esAdministrativa)
				.ifPresent((cuenta) -> this.auditService.registrarIndependiente(Actor.of(cuenta),
						AccionAuditoria.SESION_CERRADA, TipoRecurso.USUARIO, cuenta.getUsuarioId(), "Cierre de sesión"));
		}
	}

	void inicioExitoso(UsuarioSistema cuenta) {
		if (esAdministrativa(cuenta)) {
			this.auditService.registrarIndependiente(Actor.of(cuenta), AccionAuditoria.SESION_INICIADA,
					TipoRecurso.USUARIO, cuenta.getUsuarioId(), "Inicio de sesión");
		}
	}

	/** Intento fallido sobre una cuenta administrativa existente. No se guarda la contraseña probada. */
	void inicioFallido(String correo) {
		this.cuentaRepository.findByCorreo(correo)
			.filter(AuditoriaSesionHandler::esAdministrativa)
			.ifPresent((cuenta) -> this.auditService.registrarIndependiente(null, AccionAuditoria.SESION_FALLIDA,
					TipoRecurso.USUARIO, cuenta.getUsuarioId(),
					"Intento fallido de inicio de sesión para " + cuenta.getUsuario().getCorreo()));
	}

	private static boolean esAdministrativa(UsuarioSistema cuenta) {
		return cuenta.getRol().tiene(Permiso.ADMIN_ACCESS);
	}

}
