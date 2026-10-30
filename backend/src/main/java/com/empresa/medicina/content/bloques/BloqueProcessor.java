package com.empresa.medicina.content.bloques;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import com.empresa.medicina.common.ApiError;
import com.empresa.medicina.common.ApiException;
import com.empresa.medicina.media.EstadoMedio;
import com.empresa.medicina.media.MediaService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Convierte el JSON recibido en los datos tipados del bloque, los valida (estructura, reglas propias del tipo y
 * medios referenciados) y sanea el HTML. El resultado es el único JSON que se guarda.
 */
@Component
public class BloqueProcessor {

	private static final Pattern DOI = Pattern.compile("^10\\.\\d{4,9}/\\S+$");

	private final JsonMapper jsonMapper;

	private final Validator validator;

	private final HtmlSanitizer sanitizer;

	private final MediaService mediaService;

	BloqueProcessor(JsonMapper jsonMapper, Validator validator, HtmlSanitizer sanitizer, MediaService mediaService) {
		this.jsonMapper = jsonMapper;
		this.validator = validator;
		this.sanitizer = sanitizer;
		this.mediaService = mediaService;
	}

	public BloqueDatos procesar(TipoBloque tipo, JsonNode datos) {
		if (datos == null || !datos.isObject()) {
			throw invalido("Los datos del bloque deben ser un objeto.");
		}
		BloqueDatos convertido;
		try {
			convertido = this.jsonMapper.treeToValue(datos, tipo.datos());
		}
		catch (JacksonException ex) {
			throw invalido("Los datos no corresponden a un bloque de tipo " + tipo + ".");
		}
		List<ApiError.FieldError> errores = new ArrayList<>();
		for (ConstraintViolation<BloqueDatos> violacion : this.validator.validate(convertido)) {
			errores.add(new ApiError.FieldError(violacion.getPropertyPath().toString(), violacion.getMessage()));
		}
		lanzar(errores);
		BloqueDatos saneado = sanear(convertido);
		reglasDelTipo(saneado, errores);
		lanzar(errores);
		return saneado;
	}

	public String serializar(BloqueDatos datos) {
		return this.jsonMapper.writeValueAsString(datos);
	}

	public BloqueDatos leer(TipoBloque tipo, String json) {
		return this.jsonMapper.readValue(json, tipo.datos());
	}

	public JsonNode aNodo(String json) {
		return this.jsonMapper.readTree(json);
	}

	private BloqueDatos sanear(BloqueDatos datos) {
		if (datos instanceof BloqueDatos.TextoRico texto) {
			return new BloqueDatos.TextoRico(this.sanitizer.sanear(texto.html()));
		}
		if (datos instanceof BloqueDatos.Acordeon acordeon) {
			return new BloqueDatos.Acordeon(acordeon.secciones()
				.stream()
				.map((s) -> new BloqueDatos.Acordeon.Seccion(s.titulo().strip(), this.sanitizer.sanear(s.html())))
				.toList());
		}
		return datos;
	}

	private void reglasDelTipo(BloqueDatos datos, List<ApiError.FieldError> errores) {
		if (datos instanceof BloqueDatos.TextoRico texto) {
			if (!HtmlSanitizer.tieneTexto(texto.html())) {
				errores.add(new ApiError.FieldError("html", "El texto quedó vacío tras quitar el formato no permitido."));
			}
		}
		else if (datos instanceof BloqueDatos.Acordeon acordeon) {
			for (int i = 0; i < acordeon.secciones().size(); i++) {
				if (!HtmlSanitizer.tieneTexto(acordeon.secciones().get(i).html())) {
					errores.add(new ApiError.FieldError("secciones[" + i + "].html", "La sección quedó vacía."));
				}
			}
		}
		else if (datos instanceof BloqueDatos.Tabla tabla) {
			int columnas = tabla.encabezados().size();
			for (int i = 0; i < tabla.filas().size(); i++) {
				if (tabla.filas().get(i).size() != columnas) {
					errores.add(new ApiError.FieldError("filas[" + i + "]",
							"Cada fila debe tener " + columnas + " celdas, una por encabezado."));
				}
			}
		}
		else if (datos instanceof BloqueDatos.Algoritmo algoritmo) {
			validarAlgoritmo(algoritmo, errores);
		}
		else if (datos instanceof BloqueDatos.Referencias referencias) {
			for (int i = 0; i < referencias.referencias().size(); i++) {
				var ref = referencias.referencias().get(i);
				if (ref.url() != null && !ref.url().isBlank() && !esUrl(ref.url())) {
					errores.add(new ApiError.FieldError("referencias[" + i + "].url", "Escribe una URL https:// válida."));
				}
				if (ref.doi() != null && !ref.doi().isBlank() && !DOI.matcher(ref.doi().strip()).matches()) {
					errores.add(new ApiError.FieldError("referencias[" + i + "].doi",
							"El DOI debe tener la forma 10.xxxx/identificador."));
				}
			}
		}
		else if (datos instanceof BloqueDatos.Imagen imagen) {
			validarMedio(imagen.activoId(), true, errores);
		}
		else if (datos instanceof BloqueDatos.Diagrama diagrama) {
			validarMedio(diagrama.activoId(), true, errores);
		}
		else if (datos instanceof BloqueDatos.Adjunto adjunto) {
			validarMedio(adjunto.activoId(), false, errores);
		}
	}

	private void validarAlgoritmo(BloqueDatos.Algoritmo algoritmo, List<ApiError.FieldError> errores) {
		Set<String> ids = new HashSet<>();
		long inicios = 0;
		long finales = 0;
		for (var nodo : algoritmo.nodos()) {
			if (!ids.add(nodo.id())) {
				errores.add(new ApiError.FieldError("nodos", "El identificador «" + nodo.id() + "» está repetido."));
			}
			if (nodo.tipo() == BloqueDatos.Algoritmo.TipoNodo.INICIO) {
				inicios++;
			}
			if (nodo.tipo() == BloqueDatos.Algoritmo.TipoNodo.FIN) {
				finales++;
			}
		}
		if (inicios != 1) {
			errores.add(new ApiError.FieldError("nodos", "El algoritmo debe tener exactamente un paso de inicio."));
		}
		if (finales == 0) {
			errores.add(new ApiError.FieldError("nodos", "El algoritmo debe tener al menos un paso final."));
		}
		for (var conexion : algoritmo.conexiones()) {
			if (!ids.contains(conexion.desde()) || !ids.contains(conexion.hacia())) {
				errores.add(new ApiError.FieldError("conexiones",
						"La conexión " + conexion.desde() + " → " + conexion.hacia() + " usa un paso inexistente."));
			}
		}
	}

	/** El medio debe existir, no estar archivado ni rechazado y ser del tipo adecuado. */
	private void validarMedio(UUID activoId, boolean imagen, List<ApiError.FieldError> errores) {
		var referencia = this.mediaService.referencia(activoId).orElse(null);
		if (referencia == null) {
			errores.add(new ApiError.FieldError("activoId", "El archivo no existe."));
			return;
		}
		if (referencia.estado() == EstadoMedio.ARCHIVADO || referencia.estado() == EstadoMedio.RECHAZADO) {
			errores.add(new ApiError.FieldError("activoId", "El archivo está archivado o rechazado."));
		}
		if (imagen != referencia.esImagen()) {
			errores.add(new ApiError.FieldError("activoId", imagen ? "Selecciona una imagen o SVG." : "Selecciona un PDF."));
		}
	}

	private static boolean esUrl(String url) {
		try {
			URI uri = new URI(url.strip());
			return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
					&& uri.getHost() != null;
		}
		catch (URISyntaxException ex) {
			return false;
		}
	}

	private static void lanzar(List<ApiError.FieldError> errores) {
		if (!errores.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "BLOQUE_INVALIDO", "Revisa los datos del bloque.",
					List.copyOf(errores));
		}
	}

	private static ApiException invalido(String mensaje) {
		return ApiException.badRequest("BLOQUE_INVALIDO", mensaje);
	}

}
