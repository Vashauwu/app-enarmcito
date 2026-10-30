package com.empresa.medicina.config;

import java.io.IOException;

import com.empresa.medicina.common.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

/**
 * Responde con {@link ApiError} en JSON cuando Spring Security rechaza una petición (401 / 403).
 */
@Component
class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final JsonMapper jsonMapper;

	JsonSecurityErrorHandler(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		write(response, HttpStatus.UNAUTHORIZED, new ApiError("NO_AUTENTICADO", "Inicia sesión para continuar."));
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		ApiError error = (accessDeniedException instanceof CsrfException)
				? new ApiError("CSRF_INVALIDO", "La sesión de seguridad expiró. Recarga la página e inténtalo de nuevo.")
				: new ApiError("ACCESO_DENEGADO", "No tienes permiso para realizar esta acción.");
		write(response, HttpStatus.FORBIDDEN, error);
	}

	private void write(HttpServletResponse response, HttpStatus status, ApiError error) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		this.jsonMapper.writeValue(response.getOutputStream(), error);
	}

}
