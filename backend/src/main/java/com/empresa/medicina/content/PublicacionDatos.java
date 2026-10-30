package com.empresa.medicina.content;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.empresa.medicina.content.bloques.TipoBloque;
import tools.jackson.databind.JsonNode;

/**
 * Contenido de una versión publicada, tal como se guarda en {@code contenido_version.datos} y se entrega al lector.
 * Incluye responsables y fuentes para conservar la trazabilidad de cada publicación.
 */
public record PublicacionDatos(UUID id, String slug, TipoContenido tipo, String titulo, String areaMedica,
		int version, Instant publicadoAt, LocalDate fechaRevision, LocalDate proximaRevision,
		Integer tiempoLecturaMinutos, String objetivoAprendizaje, String resumen, List<String> puntosClave,
		String aplicacionClinica, Fuente fuente, Guia guia, Recurso recurso, List<Bloque> bloques,
		Responsables responsables) {

	public record Fuente(String titulo, String autores, String url, String doi, LocalDate fechaPublicacion,
			String institucion, String edicion) {
	}

	public record Guia(String poblacionObjetivo, String contextoAtencion, String alcanceClinico, LocalDate vigenteHasta,
			List<Recomendacion> recomendaciones) {
	}

	public record Recurso(String procedencia, String titularDerechos, String licenciaUso) {
	}

	/** Bloque saneado con los datos públicos de los medios que usa. */
	public record Bloque(UUID id, TipoBloque tipo, JsonNode datos, List<Medio> medios) {
	}

	public record Medio(UUID id, String tipoMime, String textoAlternativo, String pie, String credito, String licencia,
			Integer ancho, Integer alto, boolean variantes) {
	}

	public record Responsables(String autor, String revisorClinico, String editor) {
	}

}
