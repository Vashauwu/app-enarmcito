package com.empresa.medicina.media;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface ActivoMultimediaRepository extends JpaRepository<ActivoMultimedia, UUID> {

	@Query("select a from ActivoMultimedia a join fetch a.subidoPor left join fetch a.revisadoPor where a.id = :id")
	Optional<ActivoMultimedia> findConResponsables(UUID id);

	@Query(value = """
			select a from ActivoMultimedia a join fetch a.subidoPor u
			where (:estado is null or a.estado = :estado)
			  and (:tipo is null or a.tipoMime = :tipo)
			  and (:soloPropios = false or u.id = :usuarioId)
			  and (:patron is null or lower(a.nombreOriginal) like :patron or lower(a.textoAlternativo) like :patron)
			""", countQuery = """
			select count(a) from ActivoMultimedia a join a.subidoPor u
			where (:estado is null or a.estado = :estado)
			  and (:tipo is null or a.tipoMime = :tipo)
			  and (:soloPropios = false or u.id = :usuarioId)
			  and (:patron is null or lower(a.nombreOriginal) like :patron or lower(a.textoAlternativo) like :patron)
			""")
	Page<ActivoMultimedia> buscar(EstadoMedio estado, String tipo, boolean soloPropios, UUID usuarioId, String patron,
			Pageable pageable);

}
