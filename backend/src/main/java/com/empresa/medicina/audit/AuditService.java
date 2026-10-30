package com.empresa.medicina.audit;

import java.time.Instant;

import tools.jackson.databind.json.JsonMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra eventos de auditoría. Por defecto el evento forma parte de la transacción de la operación
 * auditada: si la operación falla, tampoco queda el evento.
 */
@Service
public class AuditService {

	private final AuditoriaEventoRepository repository;

	private final JsonMapper jsonMapper;

	AuditService(AuditoriaEventoRepository repository, JsonMapper jsonMapper) {
		this.repository = repository;
		this.jsonMapper = jsonMapper;
	}

	@Transactional
	public void registrar(Actor actor, AccionAuditoria accion, TipoRecurso tipo, Object recursoId, String resumen,
			String motivo, Cambios cambios) {
		this.repository.save(new AuditoriaEvento(Instant.now(), actor, accion, tipo,
				(recursoId != null) ? recursoId.toString() : null, resumen, motivo, serializar(cambios)));
	}

	public void registrar(Actor actor, AccionAuditoria accion, TipoRecurso tipo, Object recursoId, String resumen) {
		registrar(actor, accion, tipo, recursoId, resumen, null, null);
	}

	/** Para eventos que deben quedar aunque la operación que los origina falle (por ejemplo, un login fallido). */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void registrarIndependiente(Actor actor, AccionAuditoria accion, TipoRecurso tipo, Object recursoId,
			String resumen) {
		registrar(actor, accion, tipo, recursoId, resumen, null, null);
	}

	private String serializar(Cambios cambios) {
		if (cambios == null || cambios.isEmpty()) {
			return null;
		}
		return this.jsonMapper.writeValueAsString(cambios.comoMapa());
	}

}
