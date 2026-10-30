package com.empresa.medicina.progress;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import com.empresa.medicina.content.Contenido;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

/**
 * Progreso de un usuario en un artículo. Las escrituras se hacen con upserts en
 * {@link ProgresoContenidoRepository} para evitar carreras al abrir un artículo dos veces.
 */
@Entity
@Table(name = "usuario_contenido_progreso")
public class ProgresoContenido {

	@EmbeddedId
	private Id id;

	@MapsId("contenidoId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "contenido_id")
	private Contenido contenido;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoProgreso estado;

	@Column(name = "ultima_consulta_at", nullable = false)
	private Instant ultimaConsultaAt;

	@Column(name = "completado_at")
	private Instant completadoAt;

	protected ProgresoContenido() {
	}

	public Id getId() {
		return this.id;
	}

	public Contenido getContenido() {
		return this.contenido;
	}

	public EstadoProgreso getEstado() {
		return this.estado;
	}

	public Instant getUltimaConsultaAt() {
		return this.ultimaConsultaAt;
	}

	public Instant getCompletadoAt() {
		return this.completadoAt;
	}

	@Embeddable
	public record Id(@Column(name = "usuario_id") UUID usuarioId,
			@Column(name = "contenido_id") UUID contenidoId) implements Serializable {
	}

}
