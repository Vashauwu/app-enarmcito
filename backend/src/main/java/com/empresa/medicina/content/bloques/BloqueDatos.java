package com.empresa.medicina.content.bloques;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos estructurados de cada tipo de bloque. Los textos planos se muestran escapados; sólo {@code html} de
 * {@link TextoRico} y {@link Acordeon} admite formato, y siempre se guarda saneado.
 */
public sealed interface BloqueDatos permits BloqueDatos.TextoRico, BloqueDatos.Imagen, BloqueDatos.Diagrama,
		BloqueDatos.Tabla, BloqueDatos.Callout, BloqueDatos.Algoritmo, BloqueDatos.Acordeon, BloqueDatos.Referencias,
		BloqueDatos.Adjunto {

	/** Medios que el bloque referencia. */
	default List<UUID> activos() {
		return List.of();
	}

	record TextoRico(@NotBlank(message = "Escribe el texto.") @Size(max = 20000,
			message = "El texto admite como máximo 20000 caracteres.") String html) implements BloqueDatos {
	}

	record Imagen(@NotNull(message = "Selecciona una imagen.") UUID activoId,
			@Size(max = 500, message = "El pie admite como máximo 500 caracteres.") String pie) implements BloqueDatos {

		@Override
		public List<UUID> activos() {
			return List.of(this.activoId);
		}

	}

	/** Diagrama como imagen o SVG saneado, con equivalente textual obligatorio. */
	record Diagrama(@NotNull(message = "Selecciona el archivo del diagrama.") UUID activoId,
			@NotBlank(message = "Escribe el título del diagrama.") @Size(max = 200) String titulo,
			@NotBlank(message = "Describe el diagrama en texto para lectores de pantalla.") @Size(max = 4000,
					message = "La descripción admite como máximo 4000 caracteres.") String descripcionTextual)
			implements BloqueDatos {

		@Override
		public List<UUID> activos() {
			return List.of(this.activoId);
		}

	}

	record Tabla(@Size(max = 200) String titulo,
			@NotEmpty(message = "La tabla necesita encabezados.") @Size(max = 8,
					message = "La tabla admite como máximo 8 columnas.") List<@NotBlank(
							message = "Ningún encabezado puede quedar vacío.") @Size(max = 200) String> encabezados,
			@NotEmpty(message = "Agrega al menos una fila.") @Size(max = 60,
					message = "La tabla admite como máximo 60 filas.") List<@NotNull List<@Size(max = 600,
							message = "Cada celda admite como máximo 600 caracteres.") String>> filas,
			@Size(max = 300) String fuente, @Size(max = 500) String nota) implements BloqueDatos {
	}

	record Callout(@NotNull(message = "Elige el tipo de llamada.") Variante variante, @Size(max = 200) String titulo,
			@NotBlank(message = "Escribe el texto.") @Size(max = 2000) String texto) implements BloqueDatos {

		public enum Variante {

			INFORMACION, PERLA_CLINICA, ALERTA, CONTRAINDICACION, PUNTO_CLAVE

		}

	}

	/** Algoritmo clínico de nodos y conexiones; requiere revisión clínica. */
	record Algoritmo(@NotBlank(message = "Escribe el título del algoritmo.") @Size(max = 200) String titulo,
			@NotBlank(message = "Indica la versión del algoritmo.") @Size(max = 50) String version,
			@NotBlank(message = "Indica la fuente del algoritmo.") @Size(max = 300) String fuente,
			@NotBlank(message = "Describe el algoritmo en texto para lectores de pantalla.") @Size(max = 6000)
			String descripcionTextual,
			@NotNull @Size(min = 2, max = 40, message = "El algoritmo necesita entre 2 y 40 pasos.") List<@Valid Nodo> nodos,
			@NotNull @Size(max = 80) List<@Valid Conexion> conexiones) implements BloqueDatos {

		public record Nodo(@NotBlank @Pattern(regexp = "^[a-z0-9_-]{1,30}$",
				message = "Usa identificadores cortos en minúsculas.") String id, @NotNull TipoNodo tipo,
				@NotBlank(message = "Cada paso necesita texto.") @Size(max = 300) String texto) {
		}

		public record Conexion(@NotBlank String desde, @NotBlank String hacia, @Size(max = 60) String etiqueta) {
		}

		public enum TipoNodo {

			INICIO, DECISION, ACCION, FIN

		}

	}

	record Acordeon(@NotEmpty(message = "Agrega al menos una sección.") @Size(max = 20) List<@Valid Seccion> secciones)
			implements BloqueDatos {

		public record Seccion(@NotBlank(message = "Cada sección necesita un encabezado.") @Size(max = 200) String titulo,
				@NotBlank(message = "Cada sección necesita contenido.") @Size(max = 10000) String html) {
		}

	}

	record Referencias(@NotEmpty(message = "Agrega al menos una referencia.") @Size(max = 50)
			List<@Valid Referencia> referencias) implements BloqueDatos {

		public record Referencia(@NotBlank(message = "Cada referencia necesita título.") @Size(max = 300) String titulo,
				@Size(max = 500) String autores, @Size(max = 200) String emisor, @Size(max = 2000) String url,
				@Size(max = 150) String doi, @Size(max = 30) String fecha, @Size(max = 100) String etiquetaEvidencia) {
		}

	}

	/** PDF u otro material descargable autorizado; nunca sustituye la versión web. */
	record Adjunto(@NotNull(message = "Selecciona el archivo.") UUID activoId,
			@NotBlank(message = "Escribe el título del adjunto.") @Size(max = 200) String titulo,
			@Size(max = 500) String descripcion) implements BloqueDatos {

		@Override
		public List<UUID> activos() {
			return List.of(this.activoId);
		}

	}

}
