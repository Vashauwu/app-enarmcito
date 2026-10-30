package com.empresa.medicina.audit;

import java.util.UUID;

import com.empresa.medicina.users.UsuarioSistema;

/**
 * Quién realiza una acción auditada. Se guarda una copia del correo y el rol de ese momento.
 */
public record Actor(UUID id, String correo, String rol) {

	public static Actor of(UsuarioSistema cuenta) {
		return new Actor(cuenta.getUsuarioId(), cuenta.getUsuario().getCorreo(), cuenta.getRol().name());
	}

}
