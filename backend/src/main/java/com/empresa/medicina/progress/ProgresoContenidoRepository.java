package com.empresa.medicina.progress;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ProgresoContenidoRepository extends JpaRepository<ProgresoContenido, ProgresoContenido.Id> {

	/**
	 * Registra la consulta. Un artículo nuevo queda {@code EN_PROGRESO}; uno ya completado conserva su
	 * estado y sólo actualiza la fecha de última consulta.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			INSERT INTO usuario_contenido_progreso (usuario_id, contenido_id, estado, ultima_consulta_at)
			VALUES (:usuarioId, :contenidoId, 'EN_PROGRESO', now())
			ON CONFLICT (usuario_id, contenido_id)
			DO UPDATE SET ultima_consulta_at = EXCLUDED.ultima_consulta_at
			""", nativeQuery = true)
	void registrarConsulta(UUID usuarioId, UUID contenidoId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			INSERT INTO usuario_contenido_progreso (usuario_id, contenido_id, estado, ultima_consulta_at, completado_at)
			VALUES (:usuarioId, :contenidoId, 'COMPLETADO', now(), now())
			ON CONFLICT (usuario_id, contenido_id)
			DO UPDATE SET estado = 'COMPLETADO',
			              ultima_consulta_at = EXCLUDED.ultima_consulta_at,
			              completado_at = COALESCE(usuario_contenido_progreso.completado_at, EXCLUDED.completado_at)
			""", nativeQuery = true)
	void marcarCompletado(UUID usuarioId, UUID contenidoId);

	long countByIdUsuarioIdAndEstado(UUID usuarioId, EstadoProgreso estado);

	@Query("select max(p.ultimaConsultaAt) from ProgresoContenido p where p.id.usuarioId = :usuarioId")
	Optional<Instant> findUltimaConsulta(UUID usuarioId);

}
