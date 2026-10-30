package com.empresa.medicina.auth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.empresa.medicina.users.UsuarioSistema;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Principal guardado en la sesión. Sólo contiene la identidad: rol, permisos, tier y estado se consultan en
 * base de datos en cada petición (ver {@code CuentaVigenteFilter}).
 */
public class AuthenticatedUser implements UserDetails, CredentialsContainer {

	private final UUID id;

	private final String correo;

	private String passwordHash;

	private final boolean enabled;

	private final List<GrantedAuthority> authorities;

	private AuthenticatedUser(UUID id, String correo, String passwordHash, boolean enabled, Rol rol) {
		this.id = id;
		this.correo = correo;
		this.passwordHash = passwordHash;
		this.enabled = enabled;
		this.authorities = autoridades(rol);
	}

	static AuthenticatedUser from(UsuarioSistema cuenta) {
		return new AuthenticatedUser(cuenta.getUsuarioId(), cuenta.getUsuario().getCorreo(), cuenta.getPasswordHash(),
				cuenta.isActiva(), cuenta.getRol());
	}

	/** Autoridades de Spring Security para un rol: {@code ROLE_<ROL>} y un código por permiso. */
	public static List<GrantedAuthority> autoridades(Rol rol) {
		List<GrantedAuthority> autoridades = new ArrayList<>();
		autoridades.add(new SimpleGrantedAuthority("ROLE_" + rol.name()));
		rol.permisos().forEach((permiso) -> autoridades.add(new SimpleGrantedAuthority(permiso.codigo())));
		return List.copyOf(autoridades);
	}

	public UUID getId() {
		return this.id;
	}

	@Override
	public String getUsername() {
		return this.correo;
	}

	@Override
	public String getPassword() {
		return this.passwordHash;
	}

	@Override
	public boolean isEnabled() {
		return this.enabled;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return this.authorities;
	}

	@Override
	public void eraseCredentials() {
		this.passwordHash = null;
	}

}
