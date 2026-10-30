package com.empresa.medicina.content;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.empresa.medicina.tiers.Tier;
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
 * Copia de trabajo de un contenido (artículo, GPC o recurso de estudio) con su estado editorial. Lo que ve el
 * público es la versión publicada ({@link ContenidoVersion}), que no cambia al editar esta copia.
 */
@Entity
@Table(name = "contenido")
public class Contenido {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private TipoContenido tipo;

	@Enumerated(EnumType.STRING)
	@Column(name = "politica_acceso", nullable = false, length = 30)
	private PoliticaAcceso politicaAcceso;

	@Column(nullable = false, length = 200)
	private String titulo;

	@Column(nullable = false, unique = true, length = 220)
	private String slug;

	@Column(name = "area_medica", length = 100)
	private String areaMedica;

	@Column(name = "objetivo_aprendizaje")
	private String objetivoAprendizaje;

	private String resumen;

	/** Un punto clave por línea. */
	@Column(name = "puntos_clave")
	private String puntosClave;

	@Column(name = "aplicacion_clinica")
	private String aplicacionClinica;

	@Enumerated(EnumType.STRING)
	@Column(name = "metodo_aprendizaje", nullable = false, length = 30)
	private MetodoAprendizaje metodoAprendizaje;

	/** Sólo con política {@link PoliticaAcceso#POR_TIER}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tier_minimo_id")
	private Tier tierMinimo;

	@Column(name = "fuente_titulo", length = 300)
	private String fuenteTitulo;

	@Column(name = "fuente_autores")
	private String fuenteAutores;

	@Column(name = "fuente_url")
	private String fuenteUrl;

	@Column(name = "fuente_doi", length = 150)
	private String fuenteDoi;

	@Column(name = "fecha_publicacion_fuente")
	private LocalDate fechaPublicacionFuente;

	@Column(name = "fecha_revision")
	private LocalDate fechaRevision;

	@Column(name = "tiempo_lectura_minutos")
	private Integer tiempoLecturaMinutos;

	@Column(name = "revision_clinica_requerida", nullable = false)
	private boolean revisionClinicaRequerida;

	// Guía de práctica clínica

	@Column(name = "institucion_emisora", length = 200)
	private String institucionEmisora;

	@Column(name = "fuente_edicion", length = 100)
	private String fuenteEdicion;

	@Column(name = "poblacion_objetivo")
	private String poblacionObjetivo;

	@Column(name = "contexto_atencion")
	private String contextoAtencion;

	@Column(name = "alcance_clinico")
	private String alcanceClinico;

	@Column(name = "vigente_hasta")
	private LocalDate vigenteHasta;

	@Column(name = "proxima_revision")
	private LocalDate proximaRevision;

	/** JSON con la lista de {@link Recomendacion}. */
	private String recomendaciones;

	// Recurso de estudio

	private String procedencia;

	@Column(name = "titular_derechos", length = 200)
	private String titularDerechos;

	@Column(name = "licencia_uso", length = 200)
	private String licenciaUso;

	@Column(name = "autorizacion_publicacion", nullable = false)
	private boolean autorizacionPublicacion;

	@Column(name = "autorizacion_referencia")
	private String autorizacionReferencia;

	// Flujo editorial

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private EstadoContenido estado;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "autor_id")
	private Usuario autor;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "revisor_id")
	private Usuario revisor;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "editor_id")
	private Usuario editor;

	@Column(name = "enviado_revision_at")
	private Instant enviadoRevisionAt;

	@Column(name = "aprobado_at")
	private Instant aprobadoAt;

	@Column(name = "publicado_at")
	private Instant publicadoAt;

	@Column(name = "archivado_at")
	private Instant archivadoAt;

	/** Versión pública vigente; {@code null} si nunca se publicó. */
	@Column(name = "version_publicada")
	private Integer versionPublicada;

	/** Si la versión publicada sigue en circulación (se retira al archivar). */
	@Column(name = "publicacion_activa", nullable = false)
	private boolean publicacionActiva;

	@Version
	@Column(nullable = false)
	private Long version;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Contenido() {
	}

	/**
	 * Nuevo borrador creado desde el panel. Los artículos nacen gratuitos con cuenta; guías y recursos, por tier
	 * ({@code tier} es el tier que corresponde al tipo, ver {@code EditorialService}).
	 */
	public Contenido(TipoContenido tipo, Tier tier, DatosContenido datos, String recomendacionesJson, Usuario autor) {
		this.id = UUID.randomUUID();
		this.tipo = tipo;
		asignarAccesoPorTipo(tier);
		this.metodoAprendizaje = MetodoAprendizaje.LECTURA_GUIADA;
		this.estado = EstadoContenido.BORRADOR;
		this.autor = autor;
		aplicar(datos, recomendacionesJson);
	}

	public void aplicar(DatosContenido datos, String recomendacionesJson) {
		this.titulo = datos.titulo();
		this.slug = datos.slug();
		this.areaMedica = datos.areaMedica();
		this.objetivoAprendizaje = datos.objetivoAprendizaje();
		this.resumen = datos.resumen();
		this.puntosClave = datos.puntosClave().isEmpty() ? null : String.join("\n", datos.puntosClave());
		this.aplicacionClinica = datos.aplicacionClinica();
		this.fuenteTitulo = datos.fuenteTitulo();
		this.fuenteAutores = datos.fuenteAutores();
		this.fuenteUrl = datos.fuenteUrl();
		this.fuenteDoi = datos.fuenteDoi();
		this.fechaPublicacionFuente = datos.fechaPublicacionFuente();
		this.fechaRevision = datos.fechaRevision();
		this.tiempoLecturaMinutos = datos.tiempoLecturaMinutos();
		this.revisionClinicaRequerida = this.tipo == TipoContenido.GUIA_PRACTICA_CLINICA
				|| datos.revisionClinicaRequerida();
		DatosContenido.Guia guia = datos.guia();
		this.institucionEmisora = (guia != null) ? guia.institucionEmisora() : null;
		this.fuenteEdicion = (guia != null) ? guia.fuenteEdicion() : null;
		this.poblacionObjetivo = (guia != null) ? guia.poblacionObjetivo() : null;
		this.contextoAtencion = (guia != null) ? guia.contextoAtencion() : null;
		this.alcanceClinico = (guia != null) ? guia.alcanceClinico() : null;
		this.vigenteHasta = (guia != null) ? guia.vigenteHasta() : null;
		this.proximaRevision = (guia != null) ? guia.proximaRevision() : null;
		this.recomendaciones = recomendacionesJson;
		DatosContenido.Recurso recurso = datos.recurso();
		this.procedencia = (recurso != null) ? recurso.procedencia() : null;
		this.titularDerechos = (recurso != null) ? recurso.titularDerechos() : null;
		this.licenciaUso = (recurso != null) ? recurso.licenciaUso() : null;
		this.autorizacionPublicacion = recurso != null && recurso.autorizacionPublicacion();
		this.autorizacionReferencia = (recurso != null) ? recurso.autorizacionReferencia() : null;
	}

	/** Cambiar el tipo no conserva aprobaciones: el contenido vuelve a edición y repite el flujo. */
	void cambiarTipo(TipoContenido nuevo, Tier tier) {
		this.tipo = nuevo;
		asignarAccesoPorTipo(tier);
		if (nuevo == TipoContenido.GUIA_PRACTICA_CLINICA) {
			this.revisionClinicaRequerida = true;
		}
	}

	/** Artículo: gratis con cuenta. Guía o recurso: por tier (el que se indique). */
	private void asignarAccesoPorTipo(Tier tier) {
		if (this.tipo == TipoContenido.ARTICULO) {
			asignarAcceso(PoliticaAcceso.REGISTRADO_GRATUITO, null);
		}
		else {
			asignarAcceso(PoliticaAcceso.POR_TIER, tier);
		}
	}

	void asignarAcceso(PoliticaAcceso politica, Tier tier) {
		this.politicaAcceso = politica;
		this.tierMinimo = (politica == PoliticaAcceso.POR_TIER) ? tier : null;
	}

	public void asignarResponsables(Usuario autor, Usuario revisor) {
		this.autor = autor;
		this.revisor = revisor;
	}

	/**
	 * Cualquier cambio de texto o bloques deja la copia de trabajo en edición. Si había una versión publicada, sigue
	 * visible hasta publicar la actualización.
	 */
	void marcarEnEdicion() {
		if (this.estado != EstadoContenido.EN_EDICION) {
			this.estado = EstadoContenido.EN_EDICION;
			this.aprobadoAt = null;
		}
	}

	void enviarARevisionEditorial(Instant instante) {
		this.estado = EstadoContenido.EN_REVISION_EDITORIAL;
		this.enviadoRevisionAt = instante;
	}

	void pasarARevisionClinica(Usuario editor) {
		this.estado = EstadoContenido.EN_REVISION_CLINICA;
		this.editor = editor;
	}

	void aprobar(Instant instante, Usuario editorSiAplica) {
		this.estado = EstadoContenido.APROBADO;
		this.aprobadoAt = instante;
		if (editorSiAplica != null) {
			this.editor = editorSiAplica;
		}
	}

	void solicitarCambios() {
		this.estado = EstadoContenido.CAMBIOS_SOLICITADOS;
	}

	void publicar(int nuevaVersion, Instant instante, Usuario editor) {
		this.estado = EstadoContenido.PUBLICADO;
		this.versionPublicada = nuevaVersion;
		this.publicacionActiva = true;
		this.publicadoAt = instante;
		this.editor = editor;
	}

	void marcarRevisionRequerida() {
		this.estado = EstadoContenido.REVISION_REQUERIDA;
	}

	void archivar(Instant instante) {
		this.estado = EstadoContenido.ARCHIVADO;
		this.archivadoAt = instante;
		this.publicacionActiva = false;
	}

	/** Vuelve a edición; debe pasar de nuevo por revisión antes de volver a publicarse. */
	void restaurar() {
		this.estado = EstadoContenido.EN_EDICION;
		this.aprobadoAt = null;
	}

	public boolean esAutor(UUID usuarioId) {
		return this.autor != null && this.autor.getId().equals(usuarioId);
	}

	public boolean esRevisor(UUID usuarioId) {
		return this.revisor != null && this.revisor.getId().equals(usuarioId);
	}

	public List<String> getPuntosClaveLista() {
		if (this.puntosClave == null) {
			return List.of();
		}
		return this.puntosClave.lines().map(String::strip).filter((linea) -> !linea.isEmpty()).toList();
	}

	public UUID getId() {
		return this.id;
	}

	public TipoContenido getTipo() {
		return this.tipo;
	}

	public PoliticaAcceso getPoliticaAcceso() {
		return this.politicaAcceso;
	}

	public String getTitulo() {
		return this.titulo;
	}

	public String getSlug() {
		return this.slug;
	}

	public String getAreaMedica() {
		return this.areaMedica;
	}

	public String getObjetivoAprendizaje() {
		return this.objetivoAprendizaje;
	}

	public String getResumen() {
		return this.resumen;
	}

	public String getAplicacionClinica() {
		return this.aplicacionClinica;
	}

	public MetodoAprendizaje getMetodoAprendizaje() {
		return this.metodoAprendizaje;
	}

	public Tier getTierMinimo() {
		return this.tierMinimo;
	}

	public String getFuenteTitulo() {
		return this.fuenteTitulo;
	}

	public String getFuenteAutores() {
		return this.fuenteAutores;
	}

	public String getFuenteUrl() {
		return this.fuenteUrl;
	}

	public String getFuenteDoi() {
		return this.fuenteDoi;
	}

	public LocalDate getFechaPublicacionFuente() {
		return this.fechaPublicacionFuente;
	}

	public LocalDate getFechaRevision() {
		return this.fechaRevision;
	}

	public Integer getTiempoLecturaMinutos() {
		return this.tiempoLecturaMinutos;
	}

	public boolean isRevisionClinicaRequerida() {
		return this.revisionClinicaRequerida;
	}

	public String getInstitucionEmisora() {
		return this.institucionEmisora;
	}

	public String getFuenteEdicion() {
		return this.fuenteEdicion;
	}

	public String getPoblacionObjetivo() {
		return this.poblacionObjetivo;
	}

	public String getContextoAtencion() {
		return this.contextoAtencion;
	}

	public String getAlcanceClinico() {
		return this.alcanceClinico;
	}

	public LocalDate getVigenteHasta() {
		return this.vigenteHasta;
	}

	public LocalDate getProximaRevision() {
		return this.proximaRevision;
	}

	public String getRecomendaciones() {
		return this.recomendaciones;
	}

	public String getProcedencia() {
		return this.procedencia;
	}

	public String getTitularDerechos() {
		return this.titularDerechos;
	}

	public String getLicenciaUso() {
		return this.licenciaUso;
	}

	public boolean isAutorizacionPublicacion() {
		return this.autorizacionPublicacion;
	}

	public String getAutorizacionReferencia() {
		return this.autorizacionReferencia;
	}

	public EstadoContenido getEstado() {
		return this.estado;
	}

	public Usuario getAutor() {
		return this.autor;
	}

	public Usuario getRevisor() {
		return this.revisor;
	}

	public Usuario getEditor() {
		return this.editor;
	}

	public Instant getEnviadoRevisionAt() {
		return this.enviadoRevisionAt;
	}

	public Instant getAprobadoAt() {
		return this.aprobadoAt;
	}

	public Instant getPublicadoAt() {
		return this.publicadoAt;
	}

	public Instant getArchivadoAt() {
		return this.archivadoAt;
	}

	public Integer getVersionPublicada() {
		return this.versionPublicada;
	}

	public boolean isPublicacionActiva() {
		return this.publicacionActiva;
	}

	public Long getVersion() {
		return this.version;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	public Instant getUpdatedAt() {
		return this.updatedAt;
	}

}
