package com.empresa.medicina.content;

import java.time.LocalDate;
import java.util.List;

/**
 * Datos editables de la cabecera de un contenido, ya normalizados (textos sin espacios sobrantes; vacíos como
 * {@code null}). Los campos de GPC y de recurso de estudio se ignoran en los tipos que no los usan.
 */
public record DatosContenido(String titulo, String slug, String areaMedica, String objetivoAprendizaje,
		String resumen, List<String> puntosClave, String aplicacionClinica, String fuenteTitulo, String fuenteAutores,
		String fuenteUrl, String fuenteDoi, LocalDate fechaPublicacionFuente, LocalDate fechaRevision,
		Integer tiempoLecturaMinutos, boolean revisionClinicaRequerida, Guia guia, Recurso recurso) {

	public record Guia(String institucionEmisora, String fuenteEdicion, String poblacionObjetivo,
			String contextoAtencion, String alcanceClinico, LocalDate vigenteHasta, LocalDate proximaRevision,
			List<Recomendacion> recomendaciones) {
	}

	public record Recurso(String procedencia, String titularDerechos, String licenciaUso,
			boolean autorizacionPublicacion, String autorizacionReferencia) {
	}

}
