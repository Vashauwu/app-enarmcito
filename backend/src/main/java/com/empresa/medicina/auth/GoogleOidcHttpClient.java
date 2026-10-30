package com.empresa.medicina.auth;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.empresa.medicina.config.AppProperties;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Cliente real de Google: endpoint de autorización, endpoint de token y verificación del ID token contra las
 * claves públicas de Google. El {@code client_secret} sólo se envía por TLS al endpoint de token.
 */
@Component
class GoogleOidcHttpClient implements GoogleOidcClient {

	private static final String AUTHORIZATION_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";

	private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";

	private static final String JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";

	/** Google publica el emisor con y sin esquema. */
	private static final List<String> ISSUERS = List.of("https://accounts.google.com", "accounts.google.com");

	private final AppProperties.Google config;

	private final RestClient restClient;

	private volatile JwtDecoder decoder;

	GoogleOidcHttpClient(AppProperties properties) {
		this.config = properties.google();
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(5));
		factory.setReadTimeout(Duration.ofSeconds(10));
		this.restClient = RestClient.builder().requestFactory(factory).build();
	}

	@Override
	public URI urlAutorizacion(String state, String nonce, String codeChallenge) {
		return UriComponentsBuilder.fromUriString(AUTHORIZATION_ENDPOINT)
			.queryParam("client_id", this.config.clientId())
			.queryParam("redirect_uri", this.config.redirectUri())
			.queryParam("response_type", "code")
			// Sólo identidad básica: ninguna API de Google (Drive, Calendar…).
			.queryParam("scope", "openid email profile")
			.queryParam("state", state)
			.queryParam("nonce", nonce)
			.queryParam("code_challenge", codeChallenge)
			.queryParam("code_challenge_method", "S256")
			.queryParam("prompt", "select_account")
			.build()
			.encode()
			.toUri();
	}

	@Override
	public GoogleIdentidad canjear(String code, String codeVerifier, String nonce) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("code", code);
		form.add("redirect_uri", this.config.redirectUri());
		form.add("client_id", this.config.clientId());
		form.add("client_secret", this.config.clientSecret());
		form.add("code_verifier", codeVerifier);

		Map<String, Object> respuesta;
		try {
			respuesta = this.restClient.post()
				.uri(TOKEN_ENDPOINT)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		}
		catch (RestClientException ex) {
			// No se incluye la respuesta: podría contener el código o datos del cliente.
			throw new GoogleOidcException("Google rechazó el intercambio del código.", ex);
		}
		if (respuesta == null || !(respuesta.get("id_token") instanceof String idToken)) {
			throw new GoogleOidcException("Google no devolvió un ID token.");
		}

		Jwt jwt;
		try {
			jwt = decoder().decode(idToken);
		}
		catch (JwtException ex) {
			throw new GoogleOidcException("El ID token de Google no es válido.", ex);
		}
		if (!java.security.MessageDigest.isEqual(nonce.getBytes(java.nio.charset.StandardCharsets.UTF_8),
				String.valueOf(jwt.getClaimAsString("nonce")).getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
			throw new GoogleOidcException("El nonce del ID token no coincide.");
		}
		String subject = jwt.getSubject();
		String correo = jwt.getClaimAsString("email");
		if (subject == null || subject.isBlank() || correo == null || correo.isBlank()) {
			throw new GoogleOidcException("El ID token no incluye identidad ni correo.");
		}
		return new GoogleIdentidad(subject, correo, correoVerificado(jwt.getClaim("email_verified")),
				jwt.getClaimAsString("given_name"), jwt.getClaimAsString("family_name"));
	}

	private static boolean correoVerificado(Object claim) {
		return Boolean.TRUE.equals(claim) || "true".equals(claim);
	}

	/**
	 * Decodificador compartido (descarga y cachea las claves de Google) con validadores que no dependen del
	 * intento en curso. El {@code nonce}, que sí depende, se comprueba tras decodificar.
	 */
	private JwtDecoder decoder() {
		JwtDecoder actual = this.decoder;
		if (actual == null) {
			synchronized (this) {
				if (this.decoder == null) {
					NimbusJwtDecoder nuevo = NimbusJwtDecoder.withJwkSetUri(JWKS_URI).build();
					nuevo.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
							new JwtTimestampValidator(Duration.ofSeconds(60)), emisor(),
							audiencia(this.config.clientId())));
					this.decoder = nuevo;
				}
				actual = this.decoder;
			}
		}
		return actual;
	}

	private static OAuth2TokenValidator<Jwt> emisor() {
		return (jwt) -> (jwt.getIssuer() != null && ISSUERS.contains(jwt.getIssuer().toString()))
				|| ISSUERS.contains(jwt.getClaimAsString("iss")) ? OAuth2TokenValidatorResult.success()
						: fallo("Emisor inválido.");
	}

	private static OAuth2TokenValidator<Jwt> audiencia(String clientId) {
		return (jwt) -> (jwt.getAudience() != null && jwt.getAudience().contains(clientId))
				? OAuth2TokenValidatorResult.success() : fallo("Audiencia inválida.");
	}

	private static OAuth2TokenValidatorResult fallo(String descripcion) {
		return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", descripcion, null));
	}

}
