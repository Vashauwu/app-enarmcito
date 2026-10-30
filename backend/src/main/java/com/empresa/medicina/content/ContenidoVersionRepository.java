package com.empresa.medicina.content;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * Consultas públicas: siempre sobre la versión publicada vigente ({@code c.version_publicada}) de contenido en
 * circulación ({@code c.publicacion_activa}). La política de acceso y el tier se leen en vivo de {@code contenido}.
 */
interface ContenidoVersionRepository extends JpaRepository<ContenidoVersion, UUID> {

	String COLUMNAS = """
			c.id AS contenidoId, v.slug AS slug, v.tipo AS tipo, v.titulo AS titulo, v.area_medica AS areaMedica,
			v.extracto AS extracto, v.tiempo_lectura_minutos AS tiempoLecturaMinutos,
			CAST(v.proxima_revision AS text) AS proximaRevision, c.politica_acceso AS politicaAcceso,
			t.codigo AS tierCodigo, t.nombre AS tierNombre, t.orden_acceso AS tierOrden, v.version AS version
			""";

	String ORIGEN = """
			FROM contenido c
			JOIN contenido_version v ON v.contenido_id = c.id AND v.version = c.version_publicada
			LEFT JOIN tier t ON t.id = c.tier_minimo_id
			WHERE c.publicacion_activa
			""";

	/** Acceso: cuenta (gratis con cuenta) o tier suficiente; nada es anónimo. */
	String ACCESO = """
			AND ((c.politica_acceso = 'REGISTRADO_GRATUITO' AND :autenticado)
			     OR (c.politica_acceso = 'POR_TIER' AND :autenticado AND t.orden_acceso <= :ordenAcceso))
			AND (:retirarVencidas = false OR v.tipo <> 'GUIA_PRACTICA_CLINICA'
			     OR v.proxima_revision IS NULL OR v.proxima_revision >= current_date)
			""";

	String FILTRO = """
			AND (:tipo = '' OR v.tipo = :tipo)
			AND (unaccent(lower(v.titulo)) LIKE :patron OR unaccent(lower(coalesce(v.area_medica, ''))) LIKE :patron)
			""";

	@Query(value = "SELECT " + COLUMNAS + ORIGEN + ACCESO + FILTRO + " ORDER BY v.titulo",
			countQuery = "SELECT count(*) " + ORIGEN + ACCESO + FILTRO, nativeQuery = true)
	Page<FichaCatalogo> buscar(boolean autenticado, int ordenAcceso, boolean retirarVencidas, String tipo,
			String patron, Pageable pageable);

	@Query(value = "SELECT " + COLUMNAS + ", v.datos AS datos " + ORIGEN + " AND c.slug = :slug", nativeQuery = true)
	Optional<Publicada> findPublicadaPorSlug(String slug);

	@Query(value = "SELECT " + COLUMNAS + ", v.datos AS datos " + ORIGEN + " AND c.id = :contenidoId",
			nativeQuery = true)
	Optional<Publicada> findPublicadaPorId(UUID contenidoId);

	/** Publicaciones vigentes que usan un medio, para decidir si se puede entregar al visitante. */
	@Query(value = "SELECT " + COLUMNAS + ORIGEN + """
			AND EXISTS (SELECT 1 FROM contenido_version_activo va
			            WHERE va.contenido_id = c.id AND va.version = c.version_publicada AND va.activo_id = :activoId)
			""", nativeQuery = true)
	List<FichaCatalogo> findPublicadasConMedio(UUID activoId);

	/** Último artículo, guía o recurso que la persona abrió y todavía puede leer. */
	@Query(value = "SELECT " + COLUMNAS + """
			, p.estado AS progresoEstado,
			to_char(p.ultima_consulta_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"') AS ultimaConsultaAt,
			to_char(p.completado_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"') AS completadoAt
			FROM usuario_contenido_progreso p
			JOIN contenido c ON c.id = p.contenido_id
			JOIN contenido_version v ON v.contenido_id = c.id AND v.version = c.version_publicada
			LEFT JOIN tier t ON t.id = c.tier_minimo_id
			WHERE c.publicacion_activa AND p.usuario_id = :usuarioId
			""" + ACCESO + " ORDER BY p.ultima_consulta_at DESC LIMIT 1", nativeQuery = true)
	Optional<FichaConProgreso> findUltimaLectura(UUID usuarioId, boolean autenticado, int ordenAcceso,
			boolean retirarVencidas);

	@Query("select v from ContenidoVersion v where v.contenidoId = :contenidoId order by v.version desc")
	List<ContenidoVersion> findHistorial(UUID contenidoId);

	@Modifying
	@Query(value = """
			INSERT INTO contenido_version_activo (contenido_id, version, activo_id)
			VALUES (:contenidoId, :version, :activoId) ON CONFLICT DO NOTHING
			""", nativeQuery = true)
	void registrarMedio(UUID contenidoId, int version, UUID activoId);

	interface FichaCatalogo {

		UUID getContenidoId();

		String getSlug();

		String getTipo();

		String getTitulo();

		String getAreaMedica();

		String getExtracto();

		Integer getTiempoLecturaMinutos();

		String getProximaRevision();

		String getPoliticaAcceso();

		String getTierCodigo();

		String getTierNombre();

		Integer getTierOrden();

		Integer getVersion();

	}

	interface Publicada extends FichaCatalogo {

		String getDatos();

	}

	interface FichaConProgreso extends FichaCatalogo {

		String getProgresoEstado();

		String getUltimaConsultaAt();

		String getCompletadoAt();

	}

}
