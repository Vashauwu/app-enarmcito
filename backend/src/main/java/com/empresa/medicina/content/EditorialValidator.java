package com.empresa.medicina.content;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import com.empresa.medicina.common.ApiError;
import com.empresa.medicina.common.ApiException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Reglas editoriales: formato de URL y DOI, fechas, completitud por tipo antes de revisar y vigencia y derechos
 * antes de publicar.
 */
@Component
class EditorialValidator {

	static final Pattern DOI = Pattern.compile("^10\\.\\d{4,9}/\\S+$");

	/** Reglas de formato que se aplican siempre, también a un borrador. */
	void validarFormato(DatosContenido datos) {
		List<ApiError.FieldError> errores = new ArrayList<>();
		if (datos.fuenteUrl() != null && !esUrlValida(datos.fuenteUrl())) {
			errores.add(new ApiError.FieldError("fuenteUrl", "Escribe una URL completa que empiece con https://."));
		}
		if (datos.fuenteDoi() != null && !DOI.matcher(datos.fuenteDoi()).matches()) {
			errores.add(new ApiError.FieldError("fuenteDoi", "El DOI debe tener la forma 10.xxxx/identificador."));
		}
		LocalDate hoy = LocalDate.now();
		if (datos.fechaPublicacionFuente() != null && datos.fechaPublicacionFuente().isAfter(hoy)) {
			errores.add(new ApiError.FieldError("fechaPublicacionFuente", "La fecha no puede estar en el futuro."));
		}
		if (datos.fechaRevision() != null && datos.fechaRevision().isAfter(hoy)) {
			errores.add(new ApiError.FieldError("fechaRevision", "La fecha no puede estar en el futuro."));
		}
		lanzarSiHay(errores, "VALIDACION", "Revisa los datos marcados en el formulario.");
	}

	/** Campos obligatorios para enviar a revisión, según el tipo de contenido. */
	void validarCompleto(Contenido c, int bloquesActivos, int recomendaciones) {
		List<ApiError.FieldError> errores = new ArrayList<>();
		requerido(errores, "areaMedica", c.getAreaMedica(), "Indica el área médica o especialidad.");
		requerido(errores, "resumen", c.getResumen(),
				(c.getTipo() == TipoContenido.GUIA_PRACTICA_CLINICA) ? "Escribe el resumen ejecutivo." : "Escribe el resumen.");
		requerido(errores, "fuenteTitulo", c.getFuenteTitulo(), "Indica la fuente bibliográfica.");
		requerida(errores, "fechaRevision", c.getFechaRevision(), "Indica la fecha de revisión.");
		if (c.getTiempoLecturaMinutos() == null) {
			errores.add(new ApiError.FieldError("tiempoLecturaMinutos", "Indica el tiempo de lectura."));
		}
		switch (c.getTipo()) {
			case ARTICULO -> {
				if (c.getPuntosClaveLista().isEmpty()) {
					errores.add(new ApiError.FieldError("puntosClave", "Agrega al menos un punto clave."));
				}
			}
			case GUIA_PRACTICA_CLINICA -> {
				requerido(errores, "institucionEmisora", c.getInstitucionEmisora(), "Indica la institución emisora.");
				requerido(errores, "fuenteEdicion", c.getFuenteEdicion(), "Indica la edición o versión de la guía.");
				requerida(errores, "fechaPublicacionFuente", c.getFechaPublicacionFuente(),
						"Indica la fecha de publicación de la guía.");
				if (blanco(c.getFuenteUrl()) && blanco(c.getFuenteDoi())) {
					errores.add(new ApiError.FieldError("fuenteUrl", "Enlaza la fuente original (URL o DOI)."));
				}
				requerido(errores, "poblacionObjetivo", c.getPoblacionObjetivo(), "Describe la población objetivo.");
				requerido(errores, "contextoAtencion", c.getContextoAtencion(), "Describe el contexto de atención.");
				requerido(errores, "alcanceClinico", c.getAlcanceClinico(), "Describe el alcance clínico.");
				requerida(errores, "vigenteHasta", c.getVigenteHasta(), "Indica la fecha de vigencia.");
				requerida(errores, "proximaRevision", c.getProximaRevision(), "Indica la próxima revisión.");
				if (recomendaciones == 0) {
					errores.add(new ApiError.FieldError("recomendaciones", "Agrega al menos una recomendación."));
				}
			}
			case RECURSO_ESTUDIO -> {
				requerido(errores, "procedencia", c.getProcedencia(), "Indica la autoría o procedencia del material.");
				requerido(errores, "titularDerechos", c.getTitularDerechos(), "Indica el titular de los derechos.");
				requerido(errores, "licenciaUso", c.getLicenciaUso(), "Indica la licencia o autorización de uso.");
				if (bloquesActivos == 0) {
					errores.add(new ApiError.FieldError("bloques", "Agrega al menos un bloque de contenido."));
				}
			}
		}
		lanzarSiHay(errores, "CONTENIDO_INCOMPLETO", "Completa los campos obligatorios antes de continuar.");
	}

	/** Lo publicado requiere contenido completo, revisión vigente y, en recursos, autorización de publicación. */
	void validarPublicable(Contenido c, int bloquesActivos, int recomendaciones, LocalDate proximaRevision) {
		validarCompleto(c, bloquesActivos, recomendaciones);
		List<ApiError.FieldError> errores = new ArrayList<>();
		LocalDate hoy = LocalDate.now();
		if (proximaRevision == null || proximaRevision.isBefore(hoy)) {
			errores.add(new ApiError.FieldError("fechaRevision",
					"La revisión o la vigencia venció. Actualiza las fechas tras revisar el contenido contra la fuente."));
		}
		if (c.getTipo() == TipoContenido.RECURSO_ESTUDIO
				&& (!c.isAutorizacionPublicacion() || blanco(c.getAutorizacionReferencia()))) {
			errores.add(new ApiError.FieldError("autorizacionPublicacion",
					"Registra la autorización escrita para reproducir y publicar este material."));
		}
		lanzarSiHay(errores, "NO_PUBLICABLE", "El contenido aún no cumple los requisitos de publicación.");
	}

	/** Slug de URL a partir del título: minúsculas, sin acentos y separado por guiones. */
	static String slugDesde(String titulo) {
		String base = Normalizer.normalize(titulo.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
			.replaceAll("\\p{M}", "")
			.replaceAll("[^a-z0-9]+", "-")
			.replaceAll("(^-+|-+$)", "");
		if (base.length() > 200) {
			base = base.substring(0, 200).replaceAll("-+$", "");
		}
		return base.isEmpty() ? "contenido" : base;
	}

	private static boolean esUrlValida(String url) {
		try {
			URI uri = new URI(url);
			String esquema = uri.getScheme();
			return ("https".equalsIgnoreCase(esquema) || "http".equalsIgnoreCase(esquema)) && uri.getHost() != null;
		}
		catch (URISyntaxException ex) {
			return false;
		}
	}

	private static void requerido(List<ApiError.FieldError> errores, String campo, String valor, String mensaje) {
		if (blanco(valor)) {
			errores.add(new ApiError.FieldError(campo, mensaje));
		}
	}

	private static void requerida(List<ApiError.FieldError> errores, String campo, LocalDate valor, String mensaje) {
		if (valor == null) {
			errores.add(new ApiError.FieldError(campo, mensaje));
		}
	}

	private static boolean blanco(String valor) {
		return valor == null || valor.isBlank();
	}

	private static void lanzarSiHay(List<ApiError.FieldError> errores, String codigo, String mensaje) {
		if (!errores.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, codigo, mensaje, errores);
		}
	}

}
