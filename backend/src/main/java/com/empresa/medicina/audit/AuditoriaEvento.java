package com.empresa.medicina.audit;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/**
 * Evento de la bitácora. Sólo se inserta: la base de datos rechaza actualizaciones y borrados.
 */
@Entity
@Immutable
@Table(name = "auditoria_evento")
public class AuditoriaEvento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "ocurrido_at", nullable = false)
	private Instant ocurridoAt;

	@Column(name = "actor_id")
	private UUID actorId;

	@Column(name = "actor_correo", length = 254)
	private String actorCorreo;

	@Column(name = "actor_rol", length = 30)
	private String actorRol;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 60)
	private AccionAuditoria accion;

	@Column(name = "recurso_tipo", length = 30)
	private String recursoTipo;

	@Column(name = "recurso_id", length = 64)
	private String recursoId;

	@Column(nullable = false)
	private String resumen;

	private String motivo;

	/** JSON {@code {campo: {antes, despues}}}. */
	private String cambios;

	protected AuditoriaEvento() {
	}

	AuditoriaEvento(Instant ocurridoAt, Actor actor, AccionAuditoria accion, TipoRecurso recursoTipo,
			String recursoId, String resumen, String motivo, String cambios) {
		this.ocurridoAt = ocurridoAt;
		if (actor != null) {
			this.actorId = actor.id();
			this.actorCorreo = actor.correo();
			this.actorRol = actor.rol();
		}
		this.accion = accion;
		this.recursoTipo = (recursoTipo != null) ? recursoTipo.name() : null;
		this.recursoId = recursoId;
		this.resumen = resumen;
		this.motivo = motivo;
		this.cambios = cambios;
	}

	public Long getId() {
		return this.id;
	}

	public Instant getOcurridoAt() {
		return this.ocurridoAt;
	}

	public UUID getActorId() {
		return this.actorId;
	}

	public String getActorCorreo() {
		return this.actorCorreo;
	}

	public String getActorRol() {
		return this.actorRol;
	}

	public AccionAuditoria getAccion() {
		return this.accion;
	}

	public String getRecursoTipo() {
		return this.recursoTipo;
	}

	public String getRecursoId() {
		return this.recursoId;
	}

	public String getResumen() {
		return this.resumen;
	}

	public String getMotivo() {
		return this.motivo;
	}

	public String getCambios() {
		return this.cambios;
	}

}
