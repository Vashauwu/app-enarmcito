package com.empresa.medicina.content;

import java.util.List;
import java.util.UUID;

import com.empresa.medicina.content.bloques.TipoBloque;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

/**
 * Solicitudes de las transiciones del flujo editorial, del acceso y de los bloques.
 */
public final class EditorialRequests {

	private EditorialRequests() {
	}

	/** @param revisorId revisor clínico; puede asignarse aquí o después */
	public record EnviarRevision(UUID revisorId,
			@Size(max = 2000, message = "El comentario admite como máximo 2000 caracteres.") String comentario) {
	}

	public record Revision(@NotNull(message = "Indica la etapa.") ContenidoRevision.Etapa etapa,
			@NotNull(message = "Indica la decisión.") DecisionRevision decision,
			@Size(max = 4000, message = "El comentario admite como máximo 4000 caracteres.") String comentario) {
	}

	public enum DecisionRevision {

		APROBAR, SOLICITAR_CAMBIOS

	}

	public record ConMotivo(@NotBlank(message = "Indica el motivo.") @Size(max = 1000,
			message = "El motivo admite como máximo 1000 caracteres.") String motivo) {
	}

	public record Responsables(UUID autorId, UUID revisorId,
			@Size(max = 1000, message = "El motivo admite como máximo 1000 caracteres.") String motivo) {
	}

	public record Acceso(@NotNull(message = "Elige la política de acceso.") PoliticaAcceso politica, Long tierMinimoId,
			@NotBlank(message = "Indica el motivo.") @Size(max = 1000) String motivo) {
	}

	public record CambioTipo(@NotNull(message = "Elige el tipo.") TipoContenido tipo,
			@NotBlank(message = "Indica el motivo.") @Size(max = 1000) String motivo) {
	}

	public record Bloque(@NotNull(message = "Indica el tipo de bloque.") TipoBloque tipo,
			@NotNull(message = "Faltan los datos del bloque.") JsonNode datos, Integer posicion) {
	}

	public record OrdenBloques(@NotEmpty List<UUID> bloques) {
	}

}
