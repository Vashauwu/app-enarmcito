package com.empresa.medicina.content;

import java.time.Instant;
import java.util.UUID;

import com.empresa.medicina.content.bloques.TipoBloque;
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
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Bloque de contenido enriquecido. {@code datos} es JSON ya validado y saneado para su tipo.
 */
@Entity
@Table(name = "contenido_bloque")
public class ContenidoBloque {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "contenido_id", nullable = false)
	private Contenido contenido;

	@Column(nullable = false)
	private int posicion;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TipoBloque tipo;

	@Column(nullable = false)
	private String datos;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Estado estado;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "autor_id")
	private Usuario autor;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ContenidoBloque() {
	}

	ContenidoBloque(Contenido contenido, int posicion, TipoBloque tipo, String datos, Usuario autor) {
		this.id = UUID.randomUUID();
		this.contenido = contenido;
		this.posicion = posicion;
		this.tipo = tipo;
		this.datos = datos;
		this.estado = Estado.ACTIVO;
		this.autor = autor;
	}

	void actualizar(String datos) {
		this.datos = datos;
	}

	void mover(int posicion) {
		this.posicion = posicion;
	}

	void retirar() {
		this.estado = Estado.RETIRADO;
	}

	public boolean esDe(UUID usuarioId) {
		return this.autor != null && this.autor.getId().equals(usuarioId);
	}

	public UUID getId() {
		return this.id;
	}

	public int getPosicion() {
		return this.posicion;
	}

	public TipoBloque getTipo() {
		return this.tipo;
	}

	public String getDatos() {
		return this.datos;
	}

	public Estado getEstado() {
		return this.estado;
	}

	public Usuario getAutor() {
		return this.autor;
	}

	public Instant getUpdatedAt() {
		return this.updatedAt;
	}

	public enum Estado {

		ACTIVO, RETIRADO

	}

}
