package com.empresa.medicina.progress;

import java.util.UUID;

import com.empresa.medicina.content.CatalogService;
import com.empresa.medicina.content.Lector;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProgressService {

	private final ProgresoContenidoRepository progresoRepository;

	private final CatalogService catalogService;

	ProgressService(ProgresoContenidoRepository progresoRepository, CatalogService catalogService) {
		this.progresoRepository = progresoRepository;
		this.catalogService = catalogService;
	}

	/** Sólo se completa contenido publicado que la persona puede leer. */
	@Transactional
	public ProgressResponse completar(Lector lector, UUID contenidoId) {
		this.catalogService.autorizarPorId(lector, contenidoId);
		this.progresoRepository.marcarCompletado(lector.usuarioId(), contenidoId);
		return this.progresoRepository.findById(new ProgresoContenido.Id(lector.usuarioId(), contenidoId))
			.map(ProgressResponse::from)
			.orElseThrow();
	}

	public LearningResponse resumen(Lector lector) {
		LearningResponse.UltimoContenido ultimo = this.catalogService.ultimaLectura(lector)
			.map((lectura) -> new LearningResponse.UltimoContenido(lectura.ficha(), lectura.progreso()))
			.orElse(null);
		return new LearningResponse(ultimo,
				this.progresoRepository.countByIdUsuarioIdAndEstado(lector.usuarioId(), EstadoProgreso.COMPLETADO),
				this.progresoRepository.countByIdUsuarioIdAndEstado(lector.usuarioId(), EstadoProgreso.EN_PROGRESO));
	}

}
