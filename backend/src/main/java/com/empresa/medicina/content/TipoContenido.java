package com.empresa.medicina.content;

/**
 * Clase de contenido. Cada una tiene reglas de acceso y revisión distintas.
 */
public enum TipoContenido {

	/** Divulgación y lectura breve: siempre gratuito y público. */
	ARTICULO,

	/** Recurso clínico estructurado, trazable y con revisión clínica obligatoria. */
	GUIA_PRACTICA_CLINICA,

	/** Material didáctico complementario; nunca se presenta como GPC. */
	RECURSO_ESTUDIO;

	public boolean esGuiaORecurso() {
		return this != ARTICULO;
	}

}
