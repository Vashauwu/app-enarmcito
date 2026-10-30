package com.empresa.medicina.progress;

import com.empresa.medicina.content.CatalogDtos;

/**
 * Datos del bloque «Continuar aprendizaje».
 *
 * @param ultimo      último contenido consultado que la persona todavía puede leer, o {@code null}
 * @param completados contenidos completados
 * @param enProgreso  contenidos abiertos y no completados
 */
public record LearningResponse(UltimoContenido ultimo, long completados, long enProgreso) {

	public record UltimoContenido(CatalogDtos.Ficha contenido, ProgressResponse progreso) {
	}

}
