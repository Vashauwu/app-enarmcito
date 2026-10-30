package com.empresa.medicina.content;

import java.time.LocalDate;
import java.util.UUID;

import com.empresa.medicina.progress.ProgressResponse;

/**
 * Contratos del catálogo público.
 */
public final class CatalogDtos {

	private CatalogDtos() {
	}

	/** Política de acceso tal como se muestra al lector. */
	public record Acceso(PoliticaAcceso politica, String tierCodigo, String tierNombre) {
	}

	/** Tarjeta del catálogo: sólo un extracto, nunca el contenido completo. */
	public record Ficha(UUID id, String slug, TipoContenido tipo, String titulo, String areaMedica, String extracto,
			Integer tiempoLecturaMinutos, Acceso acceso, LocalDate proximaRevision, boolean requiereActualizacion) {
	}

	/** Lectura completa de la versión publicada vigente. */
	public record Lectura(PublicacionDatos contenido, Acceso acceso, boolean requiereActualizacion,
			ProgressResponse progreso) {
	}

}
