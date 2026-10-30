package com.empresa.medicina.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEventResponse(Long id, Instant ocurridoAt, ActorResponse actor, String accion,
		String accionDescripcion, String recursoTipo, String recursoId, String resumen, String motivo,
		Map<String, Object> cambios) {

	public record ActorResponse(UUID id, String correo, String rol) {
	}

}
