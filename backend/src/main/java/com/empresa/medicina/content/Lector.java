package com.empresa.medicina.content;

import java.util.UUID;

/**
 * Quién lee el catálogo: un visitante anónimo o una cuenta activa con su tier vigente.
 */
public record Lector(UUID usuarioId, int ordenAcceso, boolean autenticado) {

	public static final Lector ANONIMO = new Lector(null, -1, false);

}
