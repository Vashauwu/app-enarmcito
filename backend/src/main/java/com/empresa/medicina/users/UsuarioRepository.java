package com.empresa.medicina.users;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

	boolean existsByCorreo(String correo);

}
