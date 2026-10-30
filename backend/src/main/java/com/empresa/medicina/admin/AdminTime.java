package com.empresa.medicina.admin;

import java.time.ZoneId;

/**
 * Zona horaria de referencia para interpretar los filtros por fecha del panel.
 */
final class AdminTime {

	static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

	private AdminTime() {
	}

}
