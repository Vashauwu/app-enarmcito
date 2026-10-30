package com.empresa.medicina.users;

import java.util.UUID;

import com.empresa.medicina.common.ApiException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

	private final UsuarioSistemaRepository cuentaRepository;

	UserService(UsuarioSistemaRepository cuentaRepository) {
		this.cuentaRepository = cuentaRepository;
	}

	/**
	 * Carga la cuenta del usuario autenticado con su perfil y tier vigentes en base de datos, de modo que
	 * un cambio de tier o un bloqueo surtan efecto sin esperar a que la sesión expire.
	 */
	public UsuarioSistema obtenerCuentaActiva(UUID usuarioId) {
		UsuarioSistema cuenta = this.cuentaRepository.findConPerfilYTier(usuarioId)
			.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO",
					"Inicia sesión para continuar."));
		if (!cuenta.isActiva()) {
			throw ApiException.forbidden("CUENTA_INACTIVA", "Tu cuenta no está activa. Contacta al soporte.");
		}
		return cuenta;
	}

	public UserResponse perfil(UUID usuarioId) {
		return UserResponse.from(obtenerCuentaActiva(usuarioId));
	}

	@Transactional
	public UserResponse actualizarPerfil(UUID usuarioId, UpdateProfileRequest request) {
		UsuarioSistema cuenta = obtenerCuentaActiva(usuarioId);
		cuenta.getUsuario().actualizarNombre(request.nombre(), request.apellido());
		return UserResponse.from(cuenta);
	}

}
