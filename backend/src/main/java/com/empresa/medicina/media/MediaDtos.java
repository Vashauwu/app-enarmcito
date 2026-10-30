package com.empresa.medicina.media;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class MediaDtos {

	private MediaDtos() {
	}

	/** Metadatos declarados al cargar o editar un medio. */
	public record MetadatosRequest(
			@Size(max = 500, message = "El texto alternativo admite como máximo 500 caracteres.") String textoAlternativo,
			@Size(max = 500, message = "El pie admite como máximo 500 caracteres.") String pie,
			@Size(max = 300, message = "El crédito admite como máximo 300 caracteres.") String credito,
			@Size(max = 200, message = "La licencia admite como máximo 200 caracteres.") String licencia,
			boolean derechosConfirmados, boolean sinDatosPaciente, Long version) {
	}

	public record MotivoRequest(@NotBlank(message = "Indica el motivo.") @Size(max = 1000) String motivo) {
	}

	public record Acciones(boolean editar, boolean aprobar, boolean rechazar, boolean archivar) {
	}

	public record Medio(UUID id, String nombreOriginal, String tipoMime, long tamanoBytes, Integer ancho,
			Integer alto, String textoAlternativo, String pie, String credito, String licencia,
			boolean derechosConfirmados, EstadoMedio estado, String motivoRechazo, String subidoPor,
			String revisadoPor, Instant revisadoAt, Instant createdAt, Long version, boolean tieneVariantes,
			Acciones acciones) {
	}

}
