package com.empresa.medicina.content;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.empresa.medicina.content.bloques.TipoBloque;
import com.empresa.medicina.media.EstadoMedio;
import com.empresa.medicina.tiers.TierResponse;
import com.empresa.medicina.users.Usuario;
import tools.jackson.databind.JsonNode;

/**
 * Vistas administrativas del contenido. Sólo exponen nombre de autor, revisor y editor, nunca datos de contacto.
 */
public final class EditorialResponses {

	private EditorialResponses() {
	}

	public record Persona(UUID id, String nombre) {

		static Persona of(Usuario usuario) {
			return (usuario != null) ? new Persona(usuario.getId(), usuario.getNombre() + " " + usuario.getApellido())
					: null;
		}

	}

	public record Fila(UUID id, TipoContenido tipo, String titulo, String slug, String areaMedica,
			EstadoContenido estado, PoliticaAcceso politicaAcceso, TierResponse tierMinimo, Persona autor,
			Persona revisor, LocalDate fechaRevision, boolean revisionVencida, Integer versionPublicada,
			boolean publicacionActiva, Instant updatedAt) {
	}

	/** Acciones que la persona actual puede realizar, para mostrar sólo los controles válidos. */
	public record Acciones(boolean editar, boolean agregarBloques, boolean reordenarBloques, boolean enviarRevision,
			boolean revisionEditorial, boolean revisionClinica, boolean publicar, boolean marcarRevisionRequerida,
			boolean archivar, boolean restaurar, boolean asignar, boolean cambiarTipo, boolean cambiarAcceso,
			boolean crearAlgoritmos) {
	}

	public record Revision(UUID id, ContenidoRevision.Etapa etapa, int versionContenido,
			ContenidoRevision.Decision decision, String comentario, Persona revisor, Instant createdAt) {
	}

	public record MedioBloque(UUID id, String tipoMime, EstadoMedio estado, String textoAlternativo) {
	}

	public record Bloque(UUID id, TipoBloque tipo, int posicion, JsonNode datos, String autor, Instant updatedAt,
			List<MedioBloque> medios, boolean editable) {
	}

	public record VersionPublicada(int version, Instant publicadoAt, String publicadoPor) {
	}

	public record Detalle(UUID id, TipoContenido tipo, PoliticaAcceso politicaAcceso, TierResponse tierMinimo,
			String titulo, String slug, String areaMedica, String objetivoAprendizaje, String resumen,
			List<String> puntosClave, String aplicacionClinica, String metodoAprendizaje, String fuenteTitulo,
			String fuenteAutores, String fuenteUrl, String fuenteDoi, LocalDate fechaPublicacionFuente,
			LocalDate fechaRevision, Integer tiempoLecturaMinutos, boolean revisionClinicaRequerida,
			boolean revisionClinicaObligatoria, String institucionEmisora, String fuenteEdicion,
			String poblacionObjetivo, String contextoAtencion, String alcanceClinico, LocalDate vigenteHasta,
			LocalDate proximaRevision, List<Recomendacion> recomendaciones, String procedencia,
			String titularDerechos, String licenciaUso, boolean autorizacionPublicacion, String autorizacionReferencia,
			EstadoContenido estado, Persona autor, Persona revisor, Persona editor, Instant enviadoRevisionAt,
			Instant aprobadoAt, Instant publicadoAt, Instant archivadoAt, Instant createdAt, Instant updatedAt,
			Long version, Integer versionPublicada, boolean publicacionActiva, boolean cambiosSinPublicar,
			boolean revisionVencida, LocalDate revisionVenceEl, List<Bloque> bloques, List<Revision> revisiones,
			List<VersionPublicada> versiones, Acciones acciones) {
	}

	public record Candidatos(List<PersonaConRol> autores, List<PersonaConRol> revisores) {
	}

	public record PersonaConRol(UUID id, String nombre, String rol) {
	}

}
