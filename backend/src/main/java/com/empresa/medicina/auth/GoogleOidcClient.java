package com.empresa.medicina.auth;

import java.net.URI;

/**
 * Conversación con Google (OpenID Connect). Se aísla en una interfaz para probar el resto del flujo sin red.
 */
interface GoogleOidcClient {

	/** URL de autorización con {@code state}, {@code nonce} y PKCE (S256). */
	URI urlAutorizacion(String state, String nonce, String codeChallenge);

	/**
	 * Canjea el código por el ID token y lo valida (firma, emisor, audiencia, expiración y {@code nonce}).
	 *
	 * @throws GoogleOidcException si Google rechaza el código o el token no es válido
	 */
	GoogleIdentidad canjear(String code, String codeVerifier, String nonce);

	/** Claims del ID token que la plataforma usa. */
	record GoogleIdentidad(String subject, String correo, boolean correoVerificado, String nombre, String apellido) {
	}

	class GoogleOidcException extends RuntimeException {

		GoogleOidcException(String message, Throwable cause) {
			super(message, cause);
		}

		GoogleOidcException(String message) {
			super(message);
		}

	}

}
