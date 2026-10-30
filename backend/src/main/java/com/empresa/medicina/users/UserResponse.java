package com.empresa.medicina.users;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.empresa.medicina.auth.Permiso;
import com.empresa.medicina.tiers.TierResponse;

/**
 * Datos públicos del usuario autenticado. Nunca incluye la contraseña ni su hash. Los permisos sirven a la
 * interfaz para mostrar u ocultar opciones; la API los vuelve a verificar en cada petición.
 */
public record UserResponse(UUID id, String nombre, String apellido, String correo, String rol, String rolNombre,
		List<String> permisos, TierResponse tier, Instant createdAt) {

	public static UserResponse from(UsuarioSistema cuenta) {
		Usuario usuario = cuenta.getUsuario();
		List<String> permisos = cuenta.getRol().permisos().stream().map(Permiso::codigo).sorted().toList();
		return new UserResponse(usuario.getId(), usuario.getNombre(), usuario.getApellido(), usuario.getCorreo(),
				cuenta.getRol().name(), cuenta.getRol().nombre(), permisos, TierResponse.from(cuenta.getTier()),
				usuario.getCreatedAt());
	}

}
