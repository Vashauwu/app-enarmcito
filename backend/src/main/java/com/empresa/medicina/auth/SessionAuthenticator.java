package com.empresa.medicina.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Component;

/**
 * Inicia la sesión HTTP de un usuario ya autenticado.
 */
@Component
class SessionAuthenticator {

	private final SecurityContextHolderStrategy contextHolderStrategy = SecurityContextHolder
		.getContextHolderStrategy();

	private final SecurityContextRepository securityContextRepository;

	private final CsrfTokenRepository csrfTokenRepository;

	SessionAuthenticator(SecurityContextRepository securityContextRepository,
			CsrfTokenRepository csrfTokenRepository) {
		this.securityContextRepository = securityContextRepository;
		this.csrfTokenRepository = csrfTokenRepository;
	}

	void iniciarSesion(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
		// Evita la fijación de sesión: un id de sesión previo nunca queda autenticado.
		if (request.getSession(false) != null) {
			request.changeSessionId();
		}
		SecurityContext context = this.contextHolderStrategy.createEmptyContext();
		context.setAuthentication(authentication);
		this.contextHolderStrategy.setContext(context);
		this.securityContextRepository.saveContext(context, request, response);

		// Nuevo token CSRF tras autenticarse.
		CsrfToken csrfToken = this.csrfTokenRepository.generateToken(request);
		this.csrfTokenRepository.saveToken(csrfToken, request, response);
	}

}
