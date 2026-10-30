package com.empresa.medicina.users;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Datos de perfil de la persona. Las credenciales viven en {@link UsuarioSistema}.
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, length = 80)
	private String nombre;

	@Column(nullable = false, length = 100)
	private String apellido;

	@Column(nullable = false, unique = true, length = 254)
	private String correo;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Usuario() {
	}

	public Usuario(String nombre, String apellido, String correo) {
		this.nombre = nombre.strip();
		this.apellido = apellido.strip();
		this.correo = normalizarCorreo(correo);
	}

	/**
	 * Forma canónica del correo: sin espacios y en minúsculas. Toda comparación usa este valor.
	 */
	public static String normalizarCorreo(String correo) {
		return correo.strip().toLowerCase(Locale.ROOT);
	}

	public void actualizarNombre(String nombre, String apellido) {
		this.nombre = nombre.strip();
		this.apellido = apellido.strip();
	}

	public UUID getId() {
		return this.id;
	}

	public String getNombre() {
		return this.nombre;
	}

	public String getApellido() {
		return this.apellido;
	}

	public String getCorreo() {
		return this.correo;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

}
