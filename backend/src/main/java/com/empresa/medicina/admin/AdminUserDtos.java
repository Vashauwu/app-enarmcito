package com.empresa.medicina.admin;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.empresa.medicina.auth.AccessLinkService;
import com.empresa.medicina.auth.Rol;
import com.empresa.medicina.tiers.TierResponse;
import com.empresa.medicina.users.UsuarioSistema;
import com.empresa.medicina.users.UsuarioSistema.EstadoCuenta;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Contratos de la gestión de usuarios. Nunca incluyen contraseñas, hashes ni datos de sesión.
 */
public final class AdminUserDtos {

	private AdminUserDtos() {
	}

	public record Fila(UUID id, String nombre, String apellido, String correo, Rol rol, TierResponse tier,
			EstadoCuenta estado, Instant createdAt, Instant lastLoginAt) {

		static Fila of(UsuarioSistema cuenta) {
			var usuario = cuenta.getUsuario();
			return new Fila(usuario.getId(), usuario.getNombre(), usuario.getApellido(), usuario.getCorreo(),
					cuenta.getRol(), TierResponse.from(cuenta.getTier()), cuenta.getEstado(), usuario.getCreatedAt(),
					cuenta.getLastLoginAt());
		}

	}

	public record Actividad(long completados, long enProgreso, Instant ultimaConsultaAt) {
	}

	/** Acciones permitidas a la persona actual sobre esta cuenta. */
	public record Acciones(boolean editarDatos, boolean cambiarTier, boolean cambiarEstado, boolean cambiarRol,
			boolean restablecerAcceso, List<Rol> rolesAsignables) {
	}

	public record Detalle(Fila usuario, String bloqueoMotivo, Instant bloqueadoAt, Actividad actividad,
			Acciones acciones) {
	}

	public record OpcionRol(Rol codigo, String nombre, boolean critico) {

		static OpcionRol of(Rol rol) {
			return new OpcionRol(rol, rol.nombre(), rol.esCritico());
		}

	}

	/** Catálogos para filtros y formularios, limitados a lo que la persona puede ver y asignar. */
	public record Opciones(List<OpcionRol> roles, List<OpcionRol> rolesCreables, List<TierResponse> tiers) {
	}

	public record ActualizarRequest(
			@NotBlank(message = "El nombre es obligatorio.") @Size(max = 80,
					message = "El nombre admite como máximo 80 caracteres.") String nombre,
			@NotBlank(message = "El apellido es obligatorio.") @Size(max = 100,
					message = "El apellido admite como máximo 100 caracteres.") String apellido,
			@NotNull(message = "Selecciona un tier.") Long tierId,
			@Size(max = 1000, message = "El motivo admite como máximo 1000 caracteres.") String motivo) {
	}

	public record MotivoRequest(@NotBlank(message = "Indica el motivo.") @Size(max = 1000,
			message = "El motivo admite como máximo 1000 caracteres.") String motivo) {
	}

	public record RolRequest(@NotNull(message = "Selecciona un rol.") Rol rol,
			@NotBlank(message = "Indica el motivo.") @Size(max = 1000,
					message = "El motivo admite como máximo 1000 caracteres.") String motivo) {
	}

	public record InvitarRequest(
			@NotBlank(message = "El nombre es obligatorio.") @Size(max = 80,
					message = "El nombre admite como máximo 80 caracteres.") String nombre,
			@NotBlank(message = "El apellido es obligatorio.") @Size(max = 100,
					message = "El apellido admite como máximo 100 caracteres.") String apellido,
			@NotBlank(message = "El correo es obligatorio.") @Email(message = "Escribe un correo válido.") @Size(
					max = 254, message = "El correo admite como máximo 254 caracteres.") String correo,
			@NotNull(message = "Selecciona un rol.") Rol rol, @NotNull(message = "Selecciona un tier.") Long tierId) {
	}

	/** Respuesta con el enlace de acceso: se muestra una única vez. */
	public record ConEnlace(Detalle usuario, AccessLinkService.Enlace enlace) {
	}

}
