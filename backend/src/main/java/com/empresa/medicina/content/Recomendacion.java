package com.empresa.medicina.content;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Recomendación estructurada de una GPC. Fuerza y nivel de evidencia sólo cuando la fuente los proporciona.
 */
public record Recomendacion(
		@NotBlank(message = "Escribe el texto de la recomendación.") @Size(max = 1500,
				message = "La recomendación admite como máximo 1500 caracteres.") String texto,
		@Size(max = 100, message = "La fuerza admite como máximo 100 caracteres.") String fuerza,
		@Size(max = 100, message = "El nivel de evidencia admite como máximo 100 caracteres.") String nivelEvidencia,
		@Size(max = 500, message = "La referencia admite como máximo 500 caracteres.") String referencia) {
}
