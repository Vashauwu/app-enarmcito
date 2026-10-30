package com.empresa.medicina.audit;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Diferencias de un cambio auditado. Sólo registra los campos que realmente cambiaron. Los textos largos
 * (contenido editorial) se resumen para no copiar artículos completos en la bitácora.
 */
public final class Cambios {

	static final int MAX_TEXTO = 160;

	private final Map<String, Map<String, Object>> campos = new LinkedHashMap<>();

	public static Cambios nuevo() {
		return new Cambios();
	}

	public Cambios campo(String nombre, Object antes, Object despues) {
		Object a = normalizar(antes);
		Object d = normalizar(despues);
		if (!Objects.equals(a, d)) {
			Map<String, Object> valor = new LinkedHashMap<>();
			valor.put("antes", recortar(a));
			valor.put("despues", recortar(d));
			this.campos.put(nombre, valor);
		}
		return this;
	}

	/** Para textos extensos: sólo indica que el campo cambió y su nueva longitud. */
	public Cambios texto(String nombre, String antes, String despues) {
		if (!Objects.equals(normalizar(antes), normalizar(despues))) {
			Map<String, Object> valor = new LinkedHashMap<>();
			valor.put("modificado", true);
			valor.put("longitud", (despues != null) ? despues.length() : 0);
			this.campos.put(nombre, valor);
		}
		return this;
	}

	public boolean isEmpty() {
		return this.campos.isEmpty();
	}

	Map<String, Map<String, Object>> comoMapa() {
		return this.campos;
	}

	private static Object normalizar(Object valor) {
		if (valor instanceof String texto) {
			String limpio = texto.strip();
			return limpio.isEmpty() ? null : limpio;
		}
		return (valor != null) ? valor.toString() : null;
	}

	private static Object recortar(Object valor) {
		if (valor instanceof String texto && texto.length() > MAX_TEXTO) {
			return texto.substring(0, MAX_TEXTO) + "…";
		}
		return valor;
	}

}
