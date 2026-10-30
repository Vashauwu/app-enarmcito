package com.empresa.medicina.users;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.empresa.medicina.auth.Rol;
import com.empresa.medicina.users.UsuarioSistema.EstadoCuenta;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface UsuarioSistemaRepository
		extends JpaRepository<UsuarioSistema, UUID>, JpaSpecificationExecutor<UsuarioSistema> {

	@Query("""
			select s from UsuarioSistema s
			join fetch s.usuario u
			join fetch s.tier
			where u.correo = :correo
			""")
	Optional<UsuarioSistema> findByCorreo(String correo);

	@Query("""
			select s from UsuarioSistema s
			join fetch s.usuario
			join fetch s.tier
			where s.usuarioId = :usuarioId
			""")
	Optional<UsuarioSistema> findConPerfilYTier(UUID usuarioId);

	/** Consulta ligera que se ejecuta en cada petición autenticada. */
	@Query("select s.estado as estado, s.rol as rol from UsuarioSistema s where s.usuarioId = :usuarioId")
	Optional<EstadoYRol> findEstadoYRol(UUID usuarioId);

	/**
	 * Bloquea las filas de los SUPER_ADMIN activos durante la transacción, para que dos cambios simultáneos
	 * no puedan dejar el sistema sin ninguno.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from UsuarioSistema s where s.rol = :rol and s.estado = :estado")
	List<UsuarioSistema> bloquearPorRolYEstado(Rol rol, EstadoCuenta estado);

	long countByRolAndEstado(Rol rol, EstadoCuenta estado);

	@Query("""
			select s from UsuarioSistema s
			join fetch s.usuario u
			where s.rol in :roles and s.estado = :estado
			order by u.nombre, u.apellido
			""")
	List<UsuarioSistema> findActivosConRol(Collection<Rol> roles, EstadoCuenta estado);

	interface EstadoYRol {

		EstadoCuenta getEstado();

		Rol getRol();

	}

}
