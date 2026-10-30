package com.empresa.medicina.auth;

import com.empresa.medicina.users.Usuario;
import com.empresa.medicina.users.UsuarioSistemaRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DatabaseUserDetailsService implements UserDetailsService {

	private final UsuarioSistemaRepository cuentaRepository;

	DatabaseUserDetailsService(UsuarioSistemaRepository cuentaRepository) {
		this.cuentaRepository = cuentaRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String correo) {
		return this.cuentaRepository.findByCorreo(Usuario.normalizarCorreo(correo))
			.map(AuthenticatedUser::from)
			.orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
	}

}
