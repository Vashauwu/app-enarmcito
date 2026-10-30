package com.empresa.medicina.content;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estado editorial de la copia de trabajo. Lo que ve el público es la última versión publicada
 * ({@code contenido_version}), que puede seguir visible mientras la copia de trabajo pasa por revisión.
 */
public enum EstadoContenido {

	BORRADOR, EN_EDICION, EN_REVISION_EDITORIAL, CAMBIOS_SOLICITADOS, EN_REVISION_CLINICA, APROBADO, PUBLICADO,
	REVISION_REQUERIDA, ARCHIVADO;

	/** Estados en los que el texto y los bloques pueden editarse. */
	public static final Set<EstadoContenido> EDITABLES = EnumSet.of(BORRADOR, EN_EDICION, CAMBIOS_SOLICITADOS,
			PUBLICADO, REVISION_REQUERIDA);

	/** Estados desde los que se envía a revisión editorial. */
	public static final Set<EstadoContenido> ENVIABLES = EnumSet.of(BORRADOR, EN_EDICION, CAMBIOS_SOLICITADOS);

}
