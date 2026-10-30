package com.empresa.medicina.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
		@NotBlank(message = "El correo es obligatorio.") @Size(max = 254,
				message = "El correo admite como máximo 254 caracteres.") String correo,
		@NotBlank(message = "La contraseña es obligatoria.") @Size(max = 128,
				message = "La contraseña es demasiado larga.") String password) {

	@Override
	public String toString() {
		return "LoginRequest[correo=" + this.correo + "]";
	}

}
