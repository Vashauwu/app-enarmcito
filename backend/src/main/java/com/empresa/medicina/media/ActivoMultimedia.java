package com.empresa.medicina.media;

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
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Metadatos de un archivo multimedia. El archivo vive en el almacenamiento externo; aquí sólo hay rutas internas.
 */
@Entity
@Table(name = "activo_multimedia")
public class ActivoMultimedia {

	@Id
	private UUID id;

	@Column(name = "nombre_original", nullable = false)
	private String nombreOriginal;

	@Column(name = "tipo_mime", nullable = false, length = 50)
	private String tipoMime;

	@Column(name = "tamano_bytes", nullable = false)
	private long tamanoBytes;

	@Column(nullable = false, length = 64)
	private String sha256;

	@Column(name = "ruta_original", nullable = false, length = 300)
	private String rutaOriginal;

	@Column(name = "ruta_escritorio", length = 300)
	private String rutaEscritorio;

	@Column(name = "ruta_movil", length = 300)
	private String rutaMovil;

	private Integer ancho;

	private Integer alto;

	@Column(name = "texto_alternativo", length = 500)
	private String textoAlternativo;

	@Column(length = 500)
	private String pie;

	@Column(length = 300)
	private String credito;

	@Column(length = 200)
	private String licencia;

	@Column(name = "derechos_confirmados", nullable = false)
	private boolean derechosConfirmados;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoMedio estado;

	@Column(name = "motivo_rechazo")
	private String motivoRechazo;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "subido_por", nullable = false)
	private Usuario subidoPor;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "revisado_por")
	private Usuario revisadoPor;

	@Column(name = "revisado_at")
	private Instant revisadoAt;

	@Version
	@Column(nullable = false)
	private Long version;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ActivoMultimedia() {
	}

	ActivoMultimedia(UUID id, String nombreOriginal, ArchivoInspeccionado archivo, Rutas rutas, Metadatos metadatos,
			Usuario subidoPor) {
		this.id = id;
		this.nombreOriginal = nombreOriginal;
		this.tipoMime = archivo.tipo().mime();
		this.tamanoBytes = archivo.contenido().length;
		this.sha256 = archivo.sha256();
		this.ancho = archivo.ancho();
		this.alto = archivo.alto();
		this.rutaOriginal = rutas.original();
		this.rutaEscritorio = rutas.escritorio();
		this.rutaMovil = rutas.movil();
		this.estado = EstadoMedio.PENDIENTE;
		this.subidoPor = subidoPor;
		actualizarMetadatos(metadatos);
	}

	void actualizarMetadatos(Metadatos metadatos) {
		this.textoAlternativo = metadatos.textoAlternativo();
		this.pie = metadatos.pie();
		this.credito = metadatos.credito();
		this.licencia = metadatos.licencia();
		this.derechosConfirmados = metadatos.derechosConfirmados();
	}

	/** Un medio aprobado cuyo contenido cambia vuelve a revisión. */
	void volverAPendiente() {
		this.estado = EstadoMedio.PENDIENTE;
		this.revisadoPor = null;
		this.revisadoAt = null;
	}

	void aprobar(Usuario revisor, Instant instante) {
		this.estado = EstadoMedio.APROBADO;
		this.motivoRechazo = null;
		this.revisadoPor = revisor;
		this.revisadoAt = instante;
	}

	void rechazar(Usuario revisor, String motivo, Instant instante) {
		this.estado = EstadoMedio.RECHAZADO;
		this.motivoRechazo = motivo;
		this.revisadoPor = revisor;
		this.revisadoAt = instante;
	}

	void archivar() {
		this.estado = EstadoMedio.ARCHIVADO;
	}

	public boolean esImagen() {
		return !"application/pdf".equals(this.tipoMime);
	}

	public UUID getId() {
		return this.id;
	}

	public String getNombreOriginal() {
		return this.nombreOriginal;
	}

	public String getTipoMime() {
		return this.tipoMime;
	}

	public long getTamanoBytes() {
		return this.tamanoBytes;
	}

	public String getSha256() {
		return this.sha256;
	}

	String getRutaOriginal() {
		return this.rutaOriginal;
	}

	String getRutaEscritorio() {
		return this.rutaEscritorio;
	}

	String getRutaMovil() {
		return this.rutaMovil;
	}

	public Integer getAncho() {
		return this.ancho;
	}

	public Integer getAlto() {
		return this.alto;
	}

	public String getTextoAlternativo() {
		return this.textoAlternativo;
	}

	public String getPie() {
		return this.pie;
	}

	public String getCredito() {
		return this.credito;
	}

	public String getLicencia() {
		return this.licencia;
	}

	public boolean isDerechosConfirmados() {
		return this.derechosConfirmados;
	}

	public EstadoMedio getEstado() {
		return this.estado;
	}

	public String getMotivoRechazo() {
		return this.motivoRechazo;
	}

	public Usuario getSubidoPor() {
		return this.subidoPor;
	}

	public Usuario getRevisadoPor() {
		return this.revisadoPor;
	}

	public Instant getRevisadoAt() {
		return this.revisadoAt;
	}

	public Long getVersion() {
		return this.version;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	public boolean esDe(UUID usuarioId) {
		return this.subidoPor.getId().equals(usuarioId);
	}

	/** Metadatos editoriales del medio. */
	public record Metadatos(String textoAlternativo, String pie, String credito, String licencia,
			boolean derechosConfirmados) {
	}

	record Rutas(String original, String escritorio, String movil) {
	}

}
