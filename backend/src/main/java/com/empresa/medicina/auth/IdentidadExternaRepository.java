package com.empresa.medicina.auth;

import java.util.Optional;
import java.util.UUID;

import com.empresa.medicina.auth.IdentidadExterna.Proveedor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface IdentidadExternaRepository extends JpaRepository<IdentidadExterna, UUID> {

	@Query("""
			select i from IdentidadExterna i
			join fetch i.usuario
			where i.proveedor = :proveedor and i.subject = :subject
			""")
	Optional<IdentidadExterna> findByProveedorAndSubject(Proveedor proveedor, String subject);

	@Query("""
			select i from IdentidadExterna i
			where i.usuario.id = :usuarioId and i.proveedor = :proveedor
			""")
	Optional<IdentidadExterna> findByUsuarioAndProveedor(UUID usuarioId, Proveedor proveedor);

}
