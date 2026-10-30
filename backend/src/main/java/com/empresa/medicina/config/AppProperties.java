package com.empresa.medicina.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración propia de la aplicación (prefijo {@code app}).
 *
 * @param frontendOrigin origen permitido por CORS
 * @param demoData       si se cargan las migraciones de {@code db/demo}
 * @param registration   reglas del registro de usuarios
 * @param admin          parámetros del panel administrativo
 * @param content        políticas del catálogo de contenido
 * @param media          almacenamiento y validación de archivos
 * @param google         inicio de sesión con Google
 */
@ConfigurationProperties("app")
public record AppProperties(String frontendOrigin, boolean demoData, Registration registration, Admin admin,
		Content content, Media media, Google google) {

	/**
	 * Inicio de sesión con Google (OpenID Connect, flujo de código con PKCE y el backend como cliente
	 * confidencial). Se desactiva solo cuando no hay {@code clientId}.
	 *
	 * @param clientId     ID de cliente OAuth web del ambiente
	 * @param clientSecret secreto del cliente; nunca sale del backend
	 * @param redirectUri  URI de retorno autorizada exactamente en Google Cloud
	 */
	public record Google(String clientId, String clientSecret, String redirectUri) {

		public boolean habilitado() {
			return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
		}

	}

	/**
	 * @param allowTierSelection si el usuario puede elegir su tier al registrarse. Sólo para pruebas
	 *                           locales: en producción el tier debe provenir de una asignación autorizada.
	 */
	public record Registration(boolean allowTierSelection) {
	}

	/**
	 * @param activeUserDays              días desde el último acceso para considerar activo a un usuario
	 * @param reviewValidityMonths        meses de vigencia de la fecha de revisión de un artículo
	 * @param reviewWarningDays           días de aviso antes de que venza la revisión
	 * @param accessLinkHours             horas de vigencia de los enlaces de invitación o restablecimiento
	 * @param bootstrapSuperAdminEmail    correo del primer SUPER_ADMIN (sólo si no existe ninguno activo)
	 * @param bootstrapSuperAdminPassword contraseña inicial de ese SUPER_ADMIN
	 */
	public record Admin(int activeUserDays, int reviewValidityMonths, int reviewWarningDays, int accessLinkHours,
			String bootstrapSuperAdminEmail, String bootstrapSuperAdminPassword) {
	}

	/**
	 * @param expiredGuidePolicy {@code ADVERTIR} o {@code RETIRAR} para GPC con vigencia vencida
	 */
	public record Content(ExpiredGuidePolicy expiredGuidePolicy) {

		public enum ExpiredGuidePolicy {

			ADVERTIR, RETIRAR

		}

	}

	/**
	 * @param storageDir     directorio de almacenamiento de archivos
	 * @param maxImageBytes  tamaño máximo de una imagen
	 * @param maxPdfBytes    tamaño máximo de un PDF
	 * @param maxImagePixels ancho o alto máximo de una imagen
	 * @param clamavHost     host de clamd; vacío para no usar antivirus externo
	 * @param clamavPort     puerto de clamd
	 */
	public record Media(String storageDir, long maxImageBytes, long maxPdfBytes, int maxImagePixels,
			String clamavHost, int clamavPort) {
	}

}
