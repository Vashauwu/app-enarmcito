package com.empresa.medicina.config;

import java.util.List;

import com.empresa.medicina.users.UsuarioSistemaRepository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Seguridad basada en sesión de servidor (cookie HttpOnly) y protección CSRF para SPA. La autorización fina
 * se declara con {@code @PreAuthorize} sobre permisos atómicos ({@code users.read}, {@code content.publish}…).
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
class SecurityConfig {

	static final String SESSION_COOKIE = "MEDICINA_SESSION";

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JsonSecurityErrorHandler errorHandler,
			SecurityContextRepository securityContextRepository, CsrfTokenRepository csrfTokenRepository,
			UsuarioSistemaRepository cuentaRepository, LogoutHandler auditoriaLogoutHandler) throws Exception {
		http.authorizeHttpRequests((authorize) -> authorize
				.requestMatchers(HttpMethod.GET, "/api/v1/tiers").permitAll()
				// Salud para la plataforma de despliegue: sin detalles ni datos privados.
				.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/*").permitAll()
				// Catálogo y medios exigen cuenta; la política de acceso de cada contenido decide el resto.
				.requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login",
						"/api/v1/auth/acceso/consultar", "/api/v1/auth/acceso/definir-password")
				.permitAll()
				// Google: navegaciones del navegador. El estado del intento y la sesión se validan en el servicio.
				.requestMatchers(HttpMethod.GET, "/api/v1/auth/proveedores", "/api/v1/auth/google/iniciar",
						"/api/v1/auth/google/callback")
				.permitAll()
				.requestMatchers("/error").permitAll()
				// Primera barrera para toda la zona administrativa; cada endpoint exige además su permiso.
				.requestMatchers("/api/v1/admin/**").hasAuthority("admin.access")
				.anyRequest().authenticated())
			.securityContext((context) -> context.securityContextRepository(securityContextRepository))
			// Cookie XSRF-TOKEN legible por Angular + validación del encabezado X-XSRF-TOKEN.
			.csrf((csrf) -> csrf.spa().csrfTokenRepository(csrfTokenRepository))
			.addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
			// Estado y rol vigentes en cada petición: una cuenta bloqueada pierde la sesión de inmediato.
			.addFilterBefore(new CuentaVigenteFilter(cuentaRepository), AuthorizationFilter.class)
			.cors((cors) -> {
			})
			.exceptionHandling((exceptions) -> exceptions.authenticationEntryPoint(errorHandler)
				.accessDeniedHandler(errorHandler))
			.logout((logout) -> logout.logoutUrl("/api/v1/auth/logout")
				.addLogoutHandler(auditoriaLogoutHandler)
				.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
				.invalidateHttpSession(true)
				.deleteCookies(SESSION_COOKIE))
			.formLogin((form) -> form.disable())
			.httpBasic((basic) -> basic.disable())
			.requestCache((cache) -> cache.disable());
		return http.build();
	}

	@Bean
	SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	CsrfTokenRepository csrfTokenRepository() {
		CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		repository.setCookieCustomizer((cookie) -> cookie.sameSite("Lax"));
		return repository;
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(AppProperties properties) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(List.of(properties.frontendOrigin()));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", configuration);
		return source;
	}

}
