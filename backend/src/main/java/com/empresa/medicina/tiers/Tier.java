package com.empresa.medicina.tiers;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Nivel de acceso al contenido. Un usuario puede leer un artículo cuando el {@code ordenAcceso} de su
 * tier es mayor o igual al del tier mínimo del artículo.
 */
@Entity
@Table(name = "tier")
public class Tier {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 30)
	private String codigo;

	@Column(nullable = false, length = 80)
	private String nombre;

	private String descripcion;

	@Column(name = "orden_acceso", nullable = false, unique = true)
	private int ordenAcceso;

	@Column(nullable = false)
	private boolean activo;

	protected Tier() {
	}

	public boolean permiteAcceso(Tier tierMinimo) {
		return this.ordenAcceso >= tierMinimo.ordenAcceso;
	}

	public Long getId() {
		return this.id;
	}

	public String getCodigo() {
		return this.codigo;
	}

	public String getNombre() {
		return this.nombre;
	}

	public String getDescripcion() {
		return this.descripcion;
	}

	public int getOrdenAcceso() {
		return this.ordenAcceso;
	}

	public boolean isActivo() {
		return this.activo;
	}

}
