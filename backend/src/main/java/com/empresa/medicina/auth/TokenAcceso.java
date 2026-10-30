package com.empresa.medicina.auth;

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

/**
 * Enlace de un solo uso para definir contraseña. Sólo se guarda el SHA-256 del token.
 */
@Entity
@Table(name = "token_acceso")
class TokenAcceso {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Tipo tipo;

	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Column(name = "expira_at", nullable = false)
	private Instant expiraAt;

	@Column(name = "usado_at")
	private Instant usadoAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "creado_por")
	private Usuario creadoPor;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected TokenAcceso() {
	}

	TokenAcceso(Usuario usuario, Tipo tipo, String tokenHash, Instant expiraAt, Usuario creadoPor) {
		this.id = UUID.randomUUID();
		this.usuario = usuario;
		this.tipo = tipo;
		this.tokenHash = tokenHash;
		this.expiraAt = expiraAt;
		this.creadoPor = creadoPor;
	}

	boolean esUsable(Instant ahora) {
		return this.usadoAt == null && this.expiraAt.isAfter(ahora);
	}

	void marcarUsado(Instant ahora) {
		this.usadoAt = ahora;
	}

	Usuario getUsuario() {
		return this.usuario;
	}

	Tipo getTipo() {
		return this.tipo;
	}

	Instant getExpiraAt() {
		return this.expiraAt;
	}

	enum Tipo {

		INVITACION, RESTABLECIMIENTO

	}

}
