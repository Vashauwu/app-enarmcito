package com.empresa.medicina.config;

import java.io.IOException;
import java.util.Optional;

import com.empresa.medicina.auth.AuthenticatedUser;
import com.empresa.medicina.users.UsuarioSistema.EstadoCuenta;
import com.empresa.medicina.users.UsuarioSistemaRepository;
import com.empresa.medicina.users.UsuarioSistemaRepository.EstadoYRol;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reevalúa en cada petición autenticada el estado y el rol vigentes de la cuenta:
 * <ul>
 * <li>si la cuenta ya no está activa (por ejemplo, fue bloqueada), invalida la sesión y la petición continúa
 * como anónima, de modo que los recursos protegidos responden 401;</li>
 * <li>si está activa, sustituye los permisos de la sesión por los del rol actual, así un cambio de rol surte
 * efecto en la siguiente petición.</li>
 * </ul>
 */
class CuentaVigenteFilter extends OncePerRequestFilter {

	private final SecurityContextHolderStrategy contextHolderStrategy = SecurityContextHolder
		.getContextHolderStrategy();

	private final UsuarioSistemaRepository cuentaRepository;

	CuentaVigenteFilter(UsuarioSistemaRepository cuentaRepository) {
		this.cuentaRepository = cuentaRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Authentication authentication = this.contextHolderStrategy.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
			Optional<EstadoYRol> vigente = this.cuentaRepository.findEstadoYRol(user.getId());
			if (vigente.isEmpty() || vigente.get().getEstado() != EstadoCuenta.ACTIVO) {
				HttpSession session = request.getSession(false);
				if (session != null) {
					session.invalidate();
				}
				this.contextHolderStrategy.clearContext();
			}
			else {
				SecurityContext context = this.contextHolderStrategy.createEmptyContext();
				context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null,
						AuthenticatedUser.autoridades(vigente.get().getRol())));
				this.contextHolderStrategy.setContext(context);
			}
		}
		filterChain.doFilter(request, response);
	}

}
