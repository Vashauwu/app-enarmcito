package com.empresa.medicina.content;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/**
 * Copias de trabajo del contenido (panel). Las lecturas públicas usan {@link ContenidoVersionRepository}.
 */
public interface ContenidoRepository extends JpaRepository<Contenido, UUID>, JpaSpecificationExecutor<Contenido> {

	@Query("""
			select c from Contenido c
			left join fetch c.tierMinimo
			left join fetch c.autor
			left join fetch c.revisor
			left join fetch c.editor
			where c.id = :id
			""")
	Optional<Contenido> findParaEdicion(UUID id);

	boolean existsBySlug(String slug);

	boolean existsBySlugAndIdNot(String slug, UUID id);

	@Query("select distinct c.areaMedica from Contenido c where c.areaMedica is not null order by c.areaMedica")
	List<String> findAreasMedicas();

}
