package com.empresa.medicina.content;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface ContenidoBloqueRepository extends JpaRepository<ContenidoBloque, UUID> {

	@Query("""
			select b from ContenidoBloque b left join fetch b.autor
			where b.contenido.id = :contenidoId and b.estado = :estado
			order by b.posicion, b.createdAt
			""")
	List<ContenidoBloque> findPorContenido(UUID contenidoId, ContenidoBloque.Estado estado);

	default List<ContenidoBloque> findActivos(UUID contenidoId) {
		return findPorContenido(contenidoId, ContenidoBloque.Estado.ACTIVO);
	}

	@Modifying
	@Query(value = "DELETE FROM contenido_activo WHERE bloque_id = :bloqueId", nativeQuery = true)
	void desvincularMedios(UUID bloqueId);

	@Modifying
	@Query(value = """
			INSERT INTO contenido_activo (contenido_id, bloque_id, activo_id, proposito)
			VALUES (:contenidoId, :bloqueId, :activoId, :proposito)
			ON CONFLICT DO NOTHING
			""", nativeQuery = true)
	void vincularMedio(UUID contenidoId, UUID bloqueId, UUID activoId, String proposito);

}
