package com.empresa.medicina.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Validación uniforme de los parámetros de paginación de la API.
 */
public final class Paginacion {

	public static final int MAX_TAMANO = 50;

	private Paginacion() {
	}

	public static PageRequest de(int pagina, int tamano, Sort orden) {
		if (pagina < 0 || tamano < 1 || tamano > MAX_TAMANO) {
			throw ApiException.badRequest("PAGINACION_INVALIDA",
					"La página debe ser 0 o mayor y el tamaño estar entre 1 y " + MAX_TAMANO + ".");
		}
		return PageRequest.of(pagina, tamano, orden);
	}

	/** Texto de búsqueda recortado y validado; {@code null} si viene vacío. */
	public static String busqueda(String texto, int maximo) {
		if (texto == null || texto.isBlank()) {
			return null;
		}
		String limpio = texto.strip();
		if (limpio.length() > maximo) {
			throw ApiException.badRequest("BUSQUEDA_INVALIDA",
					"La búsqueda admite como máximo " + maximo + " caracteres.");
		}
		return limpio;
	}

	/** Patrón LIKE en minúsculas con los comodines del usuario escapados. */
	public static String patronLike(String texto) {
		String escapado = texto.toLowerCase(java.util.Locale.ROOT)
			.replace("\\", "\\\\")
			.replace("%", "\\%")
			.replace("_", "\\_");
		return "%" + escapado + "%";
	}

}
