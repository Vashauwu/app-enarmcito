package com.empresa.medicina.users;

import java.time.Instant;
import java.util.UUID;

import com.empresa.medicina.auth.Rol;
import com.empresa.medicina.tiers.Tier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Datos de autenticación y acceso del usuario (relación uno a uno con {@link Usuario}).
 */
@Entity
@Table(name = "usuarios_sistema")
public class UsuarioSistema {

	@Id
	@Column(name = "usuario_id")
	private UUID usuarioId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id")
	private Usuario usuario;

	/** {@code null} sólo mientras la cuenta está {@link EstadoCuenta#PENDIENTE PENDIENTE}. */
	@Column(name = "password_hash")
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Rol rol;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "tier_id", nullable = false)
	private Tier tier;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoCuenta estado;

	@Column(name = "bloqueo_motivo")
	private String bloqueoMotivo;

	@Column(name = "bloqueado_at")
	private Instant bloqueadoAt;

	@Column(name = "last_login_at")
	private Instant lastLoginAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected UsuarioSistema() {
	}

	/** Cuenta de alumno creada por autorregistro. */
	public UsuarioSistema(Usuario usuario, String passwordHash, Tier tier) {
		this.usuario = usuario;
		this.passwordHash = passwordHash;
		this.tier = tier;
		this.rol = Rol.ALUMNO;
		this.estado = EstadoCuenta.ACTIVO;
	}

	/** Cuenta creada desde el panel: queda pendiente hasta que la persona define su contraseña. */
	public static UsuarioSistema invitada(Usuario usuario, Rol rol, Tier tier) {
		UsuarioSistema cuenta = new UsuarioSistema();
		cuenta.usuario = usuario;
		cuenta.rol = rol;
		cuenta.tier = tier;
		cuenta.estado = EstadoCuenta.PENDIENTE;
		return cuenta;
	}

	public boolean isActiva() {
		return this.estado == EstadoCuenta.ACTIVO;
	}

	public void registrarInicioSesion(Instant instante) {
		this.lastLoginAt = instante;
	}

	public void bloquear(String motivo, Instant instante) {
		this.estado = EstadoCuenta.BLOQUEADO;
		this.bloqueoMotivo = motivo;
		this.bloqueadoAt = instante;
	}

	public void desbloquear() {
		this.estado = EstadoCuenta.ACTIVO;
		this.bloqueoMotivo = null;
		this.bloqueadoAt = null;
	}

	/**
	 * Define la contraseña mediante un enlace de acceso. Activa las cuentas invitadas; una cuenta bloqueada
	 * sigue bloqueada.
	 */
	public void definirPassword(String passwordHash) {
		this.passwordHash = passwordHash;
		if (this.estado == EstadoCuenta.PENDIENTE) {
			this.estado = EstadoCuenta.ACTIVO;
		}
	}

	public void cambiarRol(Rol rol) {
		this.rol = rol;
	}

	public void cambiarTier(Tier tier) {
		this.tier = tier;
	}

	public UUID getUsuarioId() {
		return this.usuarioId;
	}

	public Usuario getUsuario() {
		return this.usuario;
	}

	public String getPasswordHash() {
		return this.passwordHash;
	}

	public Rol getRol() {
		return this.rol;
	}

	public Tier getTier() {
		return this.tier;
	}

	public EstadoCuenta getEstado() {
		return this.estado;
	}

	public String getBloqueoMotivo() {
		return this.bloqueoMotivo;
	}

	public Instant getBloqueadoAt() {
		return this.bloqueadoAt;
	}

	public Instant getLastLoginAt() {
		return this.lastLoginAt;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	public enum EstadoCuenta {

		ACTIVO, BLOQUEADO, PENDIENTE

	}

}
