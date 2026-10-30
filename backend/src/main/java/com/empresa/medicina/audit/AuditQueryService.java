package com.empresa.medicina.audit;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.empresa.medicina.auth.Permiso;
import com.empresa.medicina.auth.Rol;
import com.empresa.medicina.common.ApiException;
import com.empresa.medicina.common.PageResponse;
import com.empresa.medicina.common.Paginacion;
import com.empresa.medicina.content.Contenido;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.hibernate.query.criteria.JpaExpression;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta de la bitácora con alcance según permisos:
 * <ul>
 * <li>{@code audit.read}: toda la bitácora;</li>
 * <li>{@code audit.read.editorial}: eventos de contenido y los propios;</li>
 * <li>{@code audit.read.own}: eventos propios y los del contenido del que la persona es autora o revisora.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class AuditQueryService {

	static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

	private final AuditoriaEventoRepository repository;

	private final JsonMapper jsonMapper;

	AuditQueryService(AuditoriaEventoRepository repository, JsonMapper jsonMapper) {
		this.repository = repository;
		this.jsonMapper = jsonMapper;
	}

	public PageResponse<AuditEventResponse> buscar(UUID actorId, Rol rol, Filtro filtro, int pagina, int tamano) {
		return PageResponse.of(this.repository.findAll(especificacion(actorId, rol, filtro),
				Paginacion.de(pagina, tamano, Sort.by(Sort.Direction.DESC, "ocurridoAt", "id"))), this::aRespuesta);
	}

	/** Últimos eventos visibles para la persona, para el tablero. */
	public List<AuditEventResponse> recientes(UUID actorId, Rol rol, int cantidad) {
		if (!puedeLeer(rol)) {
			return List.of();
		}
		return this.repository
			.findAll(especificacion(actorId, rol, Filtro.VACIO),
					Paginacion.de(0, cantidad, Sort.by(Sort.Direction.DESC, "ocurridoAt", "id")))
			.map(this::aRespuesta)
			.getContent();
	}

	public static boolean puedeLeer(Rol rol) {
		return rol.tiene(Permiso.AUDIT_READ) || rol.tiene(Permiso.AUDIT_READ_EDITORIAL)
				|| rol.tiene(Permiso.AUDIT_READ_OWN);
	}

	private Specification<AuditoriaEvento> especificacion(UUID actorId, Rol rol, Filtro filtro) {
		if (!puedeLeer(rol)) {
			throw ApiException.forbidden("ACCESO_DENEGADO", "No tienes permiso para consultar la bitácora.");
		}
		return (root, query, cb) -> {
			List<Predicate> predicados = new ArrayList<>();
			if (!rol.tiene(Permiso.AUDIT_READ)) {
				Predicate propios = cb.equal(root.get("actorId"), actorId);
				Predicate tipoContenido = cb.equal(root.get("recursoTipo"), TipoRecurso.CONTENIDO.name());
				if (rol.tiene(Permiso.AUDIT_READ_EDITORIAL)) {
					predicados.add(cb.or(propios, root.get("recursoTipo")
						.in(TipoRecurso.CONTENIDO.name(), TipoRecurso.MEDIO.name())));
				}
				else {
					Subquery<String> asignado = query.subquery(String.class);
					var contenido = asignado.from(Contenido.class);
					var autor = contenido.join("autor", JoinType.LEFT);
					var revisor = contenido.join("revisor", JoinType.LEFT);
					// recurso_id es texto: el id del contenido se convierte con un CAST real en SQL.
					asignado.select(((JpaExpression<?>) contenido.get("id")).cast(String.class))
						.where(cb.or(cb.equal(autor.get("id"), actorId), cb.equal(revisor.get("id"), actorId)));
					predicados.add(cb.or(propios, cb.and(tipoContenido, root.get("recursoId").in(asignado))));
				}
			}
			if (filtro.accion() != null) {
				predicados.add(cb.equal(root.get("accion"), filtro.accion()));
			}
			if (filtro.recursoTipo() != null) {
				predicados.add(cb.equal(root.get("recursoTipo"), filtro.recursoTipo().name()));
			}
			if (filtro.recursoId() != null) {
				predicados.add(cb.equal(root.get("recursoId"), filtro.recursoId()));
			}
			if (filtro.actor() != null) {
				predicados.add(cb.like(cb.lower(root.get("actorCorreo")), Paginacion.patronLike(filtro.actor())));
			}
			if (filtro.desde() != null) {
				predicados.add(cb.greaterThanOrEqualTo(root.get("ocurridoAt"), inicioDelDia(filtro.desde())));
			}
			if (filtro.hasta() != null) {
				predicados.add(cb.lessThan(root.get("ocurridoAt"), inicioDelDia(filtro.hasta().plusDays(1))));
			}
			return cb.and(predicados.toArray(Predicate[]::new));
		};
	}

	private AuditEventResponse aRespuesta(AuditoriaEvento evento) {
		return new AuditEventResponse(evento.getId(), evento.getOcurridoAt(),
				(evento.getActorCorreo() != null) ? new AuditEventResponse.ActorResponse(evento.getActorId(),
						evento.getActorCorreo(), evento.getActorRol()) : null,
				evento.getAccion().name(), evento.getAccion().descripcion(), evento.getRecursoTipo(),
				evento.getRecursoId(), evento.getResumen(), evento.getMotivo(), leerCambios(evento.getCambios()));
	}

	private Map<String, Object> leerCambios(String json) {
		if (json == null) {
			return null;
		}
		return this.jsonMapper.readValue(json, new TypeReference<Map<String, Object>>() {
		});
	}

	private static Instant inicioDelDia(LocalDate fecha) {
		return fecha.atStartOfDay(ZONA).toInstant();
	}

	public record Filtro(AccionAuditoria accion, TipoRecurso recursoTipo, String recursoId, String actor,
			LocalDate desde, LocalDate hasta) {

		static final Filtro VACIO = new Filtro(null, null, null, null, null, null);

	}

}
