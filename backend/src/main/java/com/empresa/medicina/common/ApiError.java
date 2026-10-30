package com.empresa.medicina.common;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Formato estable de error de la API.
 *
 * @param code        código legible por máquina, por ejemplo {@code VALIDACION}
 * @param message     mensaje para mostrar al usuario
 * @param fieldErrors errores por campo, sólo en validaciones
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(String code, String message, List<FieldError> fieldErrors) {

	public ApiError(String code, String message) {
		this(code, message, List.of());
	}

	public record FieldError(String field, String message) {
	}

}
