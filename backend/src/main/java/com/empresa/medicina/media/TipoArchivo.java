package com.empresa.medicina.media;

/**
 * Formatos admitidos, identificados por su firma de bytes y no por la extensión.
 */
public enum TipoArchivo {

	JPEG("image/jpeg", "jpg"),
	PNG("image/png", "png"),
	WEBP("image/webp", "webp"),
	SVG("image/svg+xml", "svg"),
	PDF("application/pdf", "pdf");

	private final String mime;

	private final String extension;

	TipoArchivo(String mime, String extension) {
		this.mime = mime;
		this.extension = extension;
	}

	public String mime() {
		return this.mime;
	}

	public String extension() {
		return this.extension;
	}

	public boolean esImagen() {
		return this != PDF;
	}

	/** Formatos que la plataforma recodifica en variantes optimizadas. */
	public boolean admiteVariantes() {
		return this == JPEG || this == PNG;
	}

	public static TipoArchivo deMime(String mime) {
		for (TipoArchivo tipo : values()) {
			if (tipo.mime.equals(mime)) {
				return tipo;
			}
		}
		throw new IllegalArgumentException("Tipo no admitido: " + mime);
	}

}
