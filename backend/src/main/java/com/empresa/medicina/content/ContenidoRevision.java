package com.empresa.medicina.content;

import java.time.Instant;
import java.util.UUID;

import com.empresa.medicina.users.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

/**
 * Dictamen editorial o clínico sobre una versión en preparación. Se conserva el historial completo.
 */
@Entity
@Immutable
@Table(name = "contenido_revision")
public class ContenidoRevision {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "contenido_id", nullable = false)
	private Contenido contenido;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "revisor_id")
	private Usuario revisor;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Etapa etapa;

	@Column(name = "version_contenido", nullable = false)
	private int versionContenido;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Decision decision;

	private String comentario;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected ContenidoRevision() {
	}

	ContenidoRevision(Contenido contenido, Usuario revisor, Etapa etapa, Decision decision, String comentario) {
		this.id = UUID.randomUUID();
		this.contenido = contenido;
		this.revisor = revisor;
		this.etapa = etapa;
		this.versionContenido = ((contenido.getVersionPublicada() != null) ? contenido.getVersionPublicada() : 0) + 1;
		this.decision = decision;
		this.comentario = comentario;
	}

	public UUID getId() {
		return this.id;
	}

	public Usuario getRevisor() {
		return this.revisor;
	}

	public Etapa getEtapa() {
		return this.etapa;
	}

	public int getVersionContenido() {
		return this.versionContenido;
	}

	public Decision getDecision() {
		return this.decision;
	}

	public String getComentario() {
		return this.comentario;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	public enum Decision {

		APROBADO, CAMBIOS_SOLICITADOS

	}

	public enum Etapa {

		EDITORIAL, CLINICA

	}

}
