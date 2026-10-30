package com.empresa.medicina.content;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface ContenidoRevisionRepository extends JpaRepository<ContenidoRevision, UUID> {

	@Query("""
			select r from ContenidoRevision r
			left join fetch r.revisor
			where r.contenido.id = :contenidoId
			order by r.createdAt desc
			""")
	List<ContenidoRevision> findHistorial(UUID contenidoId);

	/** Última aprobación clínica de la versión en preparación, para registrarla en la publicación. */
	@Query("""
			select r from ContenidoRevision r
			left join fetch r.revisor
			where r.contenido.id = :contenidoId and r.versionContenido = :version
			  and r.etapa = :etapa and r.decision = :decision
			order by r.createdAt desc
			""")
	List<ContenidoRevision> findDecisiones(UUID contenidoId, int version, ContenidoRevision.Etapa etapa,
			ContenidoRevision.Decision decision);

}
