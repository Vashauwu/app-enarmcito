package com.empresa.medicina.content.bloques;

public enum TipoBloque {

	TEXTO_RICO(BloqueDatos.TextoRico.class),
	IMAGEN(BloqueDatos.Imagen.class),
	DIAGRAMA(BloqueDatos.Diagrama.class),
	TABLA(BloqueDatos.Tabla.class),
	CALLOUT(BloqueDatos.Callout.class),
	ALGORITMO(BloqueDatos.Algoritmo.class),
	ACORDEON(BloqueDatos.Acordeon.class),
	REFERENCIAS(BloqueDatos.Referencias.class),
	ADJUNTO(BloqueDatos.Adjunto.class);

	private final Class<? extends BloqueDatos> datos;

	TipoBloque(Class<? extends BloqueDatos> datos) {
		this.datos = datos;
	}

	public Class<? extends BloqueDatos> datos() {
		return this.datos;
	}

}
