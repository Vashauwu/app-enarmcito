package com.empresa.medicina.content;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/**
 * Instantánea inmutable de una publicación. Las rutas públicas sólo leen de aquí, por eso editar la copia de trabajo
 * no altera lo que ve el público hasta publicar una versión nueva.
 */
@Entity
@Immutable
@Table(name = "contenido_version")
public class ContenidoVersion {

	@Id
	private UUID id;

	@Column(name = "contenido_id", nullable = false)
	private UUID contenidoId;

	@Column(nullable = false)
	private int version;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private TipoContenido tipo;

	@Column(nullable = false, length = 200)
	private String titulo;

	@Column(nullable = false, length = 220)
	private String slug;

	@Column(name = "area_medica", length = 100)
	private String areaMedica;

	private String extracto;

	@Column(name = "tiempo_lectura_minutos")
	private Integer tiempoLecturaMinutos;

	@Column(name = "proxima_revision")
	private LocalDate proximaRevision;

	@Column(nullable = false)
	private String datos;

	@Column(name = "publicado_por")
	private UUID publicadoPor;

	@Column(name = "publicado_at", nullable = false)
	private Instant publicadoAt;

	protected ContenidoVersion() {
	}

	ContenidoVersion(UUID contenidoId, int version, TipoContenido tipo, String titulo, String slug, String areaMedica,
			String extracto, Integer tiempoLecturaMinutos, LocalDate proximaRevision, String datos, UUID publicadoPor,
			Instant publicadoAt) {
		this.id = UUID.randomUUID();
		this.contenidoId = contenidoId;
		this.version = version;
		this.tipo = tipo;
		this.titulo = titulo;
		this.slug = slug;
		this.areaMedica = areaMedica;
		this.extracto = extracto;
		this.tiempoLecturaMinutos = tiempoLecturaMinutos;
		this.proximaRevision = proximaRevision;
		this.datos = datos;
		this.publicadoPor = publicadoPor;
		this.publicadoAt = publicadoAt;
	}

	public int getVersion() {
		return this.version;
	}

	public TipoContenido getTipo() {
		return this.tipo;
	}

	public String getTitulo() {
		return this.titulo;
	}

	public String getDatos() {
		return this.datos;
	}

	public UUID getPublicadoPor() {
		return this.publicadoPor;
	}

	public Instant getPublicadoAt() {
		return this.publicadoAt;
	}

}
