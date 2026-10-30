package com.empresa.medicina.auth;

import java.time.Instant;
import java.util.UUID;

import com.empresa.medicina.users.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Identidad de un proveedor externo vinculada a una cuenta local. Sólo identifica a la persona: no guarda
 * tokens ni credenciales del proveedor y no concede rol ni tier.
 */
@Entity
@Table(name = "usuario_identidad_externa")
public class IdentidadExterna {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Proveedor proveedor;

	/** Claim {@code sub} del proveedor. */
	@Column(nullable = false)
	private String subject;

	@Column(name = "correo_verificado", length = 254)
	private String correoVerificado;

	@Column(name = "email_verified", nullable = false)
	private boolean emailVerified;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "last_login_at")
	private Instant lastLoginAt;

	protected IdentidadExterna() {
	}

	public IdentidadExterna(Usuario usuario, Proveedor proveedor, String subject, String correoVerificado,
			boolean emailVerified) {
		this.usuario = usuario;
		this.proveedor = proveedor;
		this.subject = subject;
		this.correoVerificado = correoVerificado;
		this.emailVerified = emailVerified;
	}

	public void registrarInicioSesion(String correoVerificado, boolean emailVerified, Instant instante) {
		this.correoVerificado = correoVerificado;
		this.emailVerified = emailVerified;
		this.lastLoginAt = instante;
	}

	public UUID getId() {
		return this.id;
	}

	public Usuario getUsuario() {
		return this.usuario;
	}

	public Proveedor getProveedor() {
		return this.proveedor;
	}

	public String getSubject() {
		return this.subject;
	}

	public String getCorreoVerificado() {
		return this.correoVerificado;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	public Instant getLastLoginAt() {
		return this.lastLoginAt;
	}

	public enum Proveedor {

		GOOGLE

	}

}
