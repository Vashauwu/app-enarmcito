package com.empresa.medicina.media;

/**
 * Archivo validado y listo para guardar. Para SVG, {@code contenido} ya está saneado.
 */
record ArchivoInspeccionado(TipoArchivo tipo, byte[] contenido, String sha256, Integer ancho, Integer alto,
		byte[] varianteEscritorio, byte[] varianteMovil) {
}
