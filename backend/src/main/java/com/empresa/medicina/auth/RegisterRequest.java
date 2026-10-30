package com.empresa.medicina.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank(message = "El nombre es obligatorio.") @Size(max = 80,
				message = "El nombre admite como máximo 80 caracteres.") String nombre,
		@NotBlank(message = "El apellido es obligatorio.") @Size(max = 100,
				message = "El apellido admite como máximo 100 caracteres.") String apellido,
		@NotBlank(message = "El correo es obligatorio.") @Email(message = "Escribe un correo válido.") @Size(max = 254,
				message = "El correo admite como máximo 254 caracteres.") String correo,
		@NotBlank(message = "La contraseña es obligatoria.") @Size(min = AuthService.PASSWORD_MIN_LENGTH,
				max = AuthService.PASSWORD_MAX_LENGTH,
				message = "La contraseña debe tener entre 8 y 72 caracteres.") String password,
		@NotNull(message = "Selecciona un tier.") Long tierId) {

	@Override
	public String toString() {
		return "RegisterRequest[correo=" + this.correo + ", tierId=" + this.tierId + "]";
	}

}
