package com.empresa.medicina.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tareas del sistema sobre el contenido:
 * <ul>
 * <li>al arrancar, crea la versión pública del contenido publicado antes de este módulo (migración y datos demo);</li>
 * <li>cada hora, marca como «requiere actualización» lo publicado con vigencia o revisión vencida.</li>
 * </ul>
 */
@Component
class ContenidoTareas implements ApplicationRunner {

	private static final Logger logger = LoggerFactory.getLogger(ContenidoTareas.class);

	private final EditorialService editorialService;

	ContenidoTareas(EditorialService editorialService) {
		this.editorialService = editorialService;
	}

	@Override
	public void run(ApplicationArguments args) {
		int publicados = this.editorialService.publicarExistentes();
		if (publicados > 0) {
			logger.info("Versiones públicas creadas para {} contenidos publicados previamente.", publicados);
		}
		revisarVencimientos();
	}

	@Scheduled(cron = "0 15 * * * *")
	void revisarVencimientos() {
		int marcados = this.editorialService.marcarVencidas();
		if (marcados > 0) {
			logger.info("{} publicaciones marcadas como «requiere actualización».", marcados);
		}
	}

}
