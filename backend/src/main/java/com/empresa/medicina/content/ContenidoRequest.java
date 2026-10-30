package com.empresa.medicina.content;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Cabecera editorial de un contenido. Un borrador admite campos vacíos; la completitud se exige por tipo al enviar a
 * revisión y al publicar. Los campos de guía y de recurso se ignoran en los tipos que no los usan.
 *
 * @param tipo    sólo al crear; para cambiarlo después hace falta {@code content.type.assign}
 * @param version versión que el editor tenía abierta; obligatoria al actualizar para detectar ediciones simultáneas
 */
public record ContenidoRequest(TipoContenido tipo,
		@NotBlank(message = "El título es obligatorio.") @Size(max = 200,
				message = "El título admite como máximo 200 caracteres.") String titulo,
		@Size(max = 220, message = "El slug admite como máximo 220 caracteres.") @Pattern(
				regexp = "^$|^[a-z0-9]+(-[a-z0-9]+)*$",
				message = "Usa sólo minúsculas, números y guiones (por ejemplo, sepsis-definiciones).") String slug,
		@Size(max = 100, message = "El área médica admite como máximo 100 caracteres.") String areaMedica,
		@Size(max = 2000, message = "El objetivo admite como máximo 2000 caracteres.") String objetivoAprendizaje,
		@Size(max = 10000, message = "El resumen admite como máximo 10000 caracteres.") String resumen,
		@Size(max = 20, message = "Incluye como máximo 20 puntos clave.") List<@Size(max = 500,
				message = "Cada punto clave admite como máximo 500 caracteres.") String> puntosClave,
		@Size(max = 4000, message = "La aplicación clínica admite como máximo 4000 caracteres.") String aplicacionClinica,
		@Size(max = 300, message = "El título de la fuente admite como máximo 300 caracteres.") String fuenteTitulo,
		@Size(max = 1000, message = "Los autores admiten como máximo 1000 caracteres.") String fuenteAutores,
		@Size(max = 2000, message = "La URL admite como máximo 2000 caracteres.") String fuenteUrl,
		@Size(max = 150, message = "El DOI admite como máximo 150 caracteres.") String fuenteDoi,
		LocalDate fechaPublicacionFuente, LocalDate fechaRevision,
		@Positive(message = "El tiempo de lectura debe ser mayor a cero.") @Max(value = 240,
				message = "El tiempo de lectura admite como máximo 240 minutos.") Integer tiempoLecturaMinutos,
		Boolean revisionClinicaRequerida,
		// Guía de práctica clínica
		@Size(max = 200, message = "La institución admite como máximo 200 caracteres.") String institucionEmisora,
		@Size(max = 100, message = "La edición admite como máximo 100 caracteres.") String fuenteEdicion,
		@Size(max = 4000) String poblacionObjetivo, @Size(max = 4000) String contextoAtencion,
		@Size(max = 4000) String alcanceClinico, LocalDate vigenteHasta, LocalDate proximaRevision,
		@Size(max = 60, message = "Incluye como máximo 60 recomendaciones.") List<@Valid Recomendacion> recomendaciones,
		// Recurso de estudio
		@Size(max = 4000) String procedencia, @Size(max = 200) String titularDerechos,
		@Size(max = 200) String licenciaUso, Boolean autorizacionPublicacion,
		@Size(max = 2000) String autorizacionReferencia, Long version) {
}
