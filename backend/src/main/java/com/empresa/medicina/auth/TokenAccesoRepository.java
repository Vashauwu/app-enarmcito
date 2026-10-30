package com.empresa.medicina.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface TokenAccesoRepository extends JpaRepository<TokenAcceso, UUID> {

	@Query("select t from TokenAcceso t join fetch t.usuario where t.tokenHash = :tokenHash")
	Optional<TokenAcceso> findByTokenHash(String tokenHash);

	/** Al generar un enlace nuevo, los anteriores sin usar dejan de servir. */
	@Modifying
	@Query("update TokenAcceso t set t.usadoAt = :ahora where t.usuario.id = :usuarioId and t.usadoAt is null")
	void invalidarPendientes(UUID usuarioId, Instant ahora);

}
