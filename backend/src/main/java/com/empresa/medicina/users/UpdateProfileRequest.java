package com.empresa.medicina.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
		@NotBlank(message = "El nombre es obligatorio.") @Size(max = 80,
				message = "El nombre admite como máximo 80 caracteres.") String nombre,
		@NotBlank(message = "El apellido es obligatorio.") @Size(max = 100,
				message = "El apellido admite como máximo 100 caracteres.") String apellido) {
}
