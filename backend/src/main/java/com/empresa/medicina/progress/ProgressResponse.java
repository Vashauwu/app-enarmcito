package com.empresa.medicina.progress;

import java.time.Instant;
import java.util.UUID;

public record ProgressResponse(UUID contenidoId, EstadoProgreso estado, Instant ultimaConsultaAt,
		Instant completadoAt) {

	public static ProgressResponse from(ProgresoContenido progreso) {
		return new ProgressResponse(progreso.getId().contenidoId(), progreso.getEstado(),
				progreso.getUltimaConsultaAt(), progreso.getCompletadoAt());
	}

}
