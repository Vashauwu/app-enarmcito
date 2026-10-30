package com.empresa.medicina.content;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.empresa.medicina.common.ApiError;
import com.empresa.medicina.common.ApiException;
import com.empresa.medicina.config.AppProperties;
import com.empresa.medicina.content.bloques.BloqueProcessor;
import com.empresa.medicina.media.EstadoMedio;
import com.empresa.medicina.media.MediaService;
import com.empresa.medicina.users.Usuario;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Genera la instantánea pública de un contenido. Sólo incluye bloques activos y medios aprobados; si algún medio no
 * lo está, la publicación se rechaza para que nada pendiente quede expuesto.
 */
@Service
public class PublicacionService {

	private final ContenidoVersionRepository versionRepository;

	private final ContenidoBloqueRepository bloqueRepository;

	private final ContenidoRevisionRepository revisionRepository;

	private final BloqueProcessor bloqueProcessor;

	private final MediaService mediaService;

	private final JsonMapper jsonMapper;

	private final AppProperties properties;

	PublicacionService(ContenidoVersionRepository versionRepository, ContenidoBloqueRepository bloqueRepository,
			ContenidoRevisionRepository revisionRepository, BloqueProcessor bloqueProcessor, MediaService mediaService,
			JsonMapper jsonMapper, AppProperties properties) {
		this.versionRepository = versionRepository;
		this.bloqueRepository = bloqueRepository;
		this.revisionRepository = revisionRepository;
		this.bloqueProcessor = bloqueProcessor;
		this.mediaService = mediaService;
		this.jsonMapper = jsonMapper;
		this.properties = properties;
	}

	/** Medios de los bloques activos que aún no están aprobados. */
	List<ApiError.FieldError> mediosNoAprobados(Contenido contenido) {
		List<ApiError.FieldError> errores = new ArrayList<>();
		for (UUID activoId : mediosDe(contenido)) {
			if (!this.mediaService.estaAprobado(activoId)) {
				errores.add(new ApiError.FieldError("bloques",
						"Un archivo usado en los bloques aún no está aprobado (" + activoId + ")."));
			}
		}
		return errores;
	}

	/** Crea la versión siguiente y la deja como versión pública vigente. */
	@Transactional(propagation = Propagation.MANDATORY)
	public int publicar(Contenido contenido, Usuario publicadoPor, Instant instante) {
		List<ApiError.FieldError> pendientes = mediosNoAprobados(contenido);
		if (!pendientes.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "MEDIOS_PENDIENTES",
					"Todos los archivos deben estar aprobados antes de publicar.", pendientes);
		}
		int version = ((contenido.getVersionPublicada() != null) ? contenido.getVersionPublicada() : 0) + 1;
		List<PublicacionDatos.Bloque> bloques = new ArrayList<>();
		Set<UUID> medios = new LinkedHashSet<>();
		for (ContenidoBloque bloque : this.bloqueRepository.findActivos(contenido.getId())) {
			var datos = this.bloqueProcessor.leer(bloque.getTipo(), bloque.getDatos());
			List<PublicacionDatos.Medio> mediosBloque = new ArrayList<>();
			for (UUID activoId : datos.activos()) {
				this.mediaService.referencia(activoId)
					.filter((r) -> r.estado() == EstadoMedio.APROBADO)
					.ifPresent((r) -> mediosBloque.add(new PublicacionDatos.Medio(r.id(), r.tipoMime(),
							r.textoAlternativo(), r.pie(), r.credito(), r.licencia(), r.ancho(), r.alto(),
							r.tieneVariantes())));
				medios.add(activoId);
			}
			bloques.add(new PublicacionDatos.Bloque(bloque.getId(), bloque.getTipo(),
					this.bloqueProcessor.aNodo(bloque.getDatos()), mediosBloque));
		}
		String revisorClinico = this.revisionRepository
			.findDecisiones(contenido.getId(), version, ContenidoRevision.Etapa.CLINICA,
					ContenidoRevision.Decision.APROBADO)
			.stream()
			.findFirst()
			.map((r) -> nombre(r.getRevisor()))
			.orElse(null);
		PublicacionDatos datos = new PublicacionDatos(contenido.getId(), contenido.getSlug(), contenido.getTipo(),
				contenido.getTitulo(), contenido.getAreaMedica(), version, instante, contenido.getFechaRevision(),
				proximaRevision(contenido), contenido.getTiempoLecturaMinutos(), contenido.getObjetivoAprendizaje(),
				contenido.getResumen(), contenido.getPuntosClaveLista(), contenido.getAplicacionClinica(),
				new PublicacionDatos.Fuente(contenido.getFuenteTitulo(), contenido.getFuenteAutores(),
						contenido.getFuenteUrl(), contenido.getFuenteDoi(), contenido.getFechaPublicacionFuente(),
						contenido.getInstitucionEmisora(), contenido.getFuenteEdicion()),
				(contenido.getTipo() == TipoContenido.GUIA_PRACTICA_CLINICA)
						? new PublicacionDatos.Guia(contenido.getPoblacionObjetivo(), contenido.getContextoAtencion(),
								contenido.getAlcanceClinico(), contenido.getVigenteHasta(),
								recomendaciones(contenido.getRecomendaciones()))
						: null,
				(contenido.getTipo() == TipoContenido.RECURSO_ESTUDIO)
						? new PublicacionDatos.Recurso(contenido.getProcedencia(), contenido.getTitularDerechos(),
								contenido.getLicenciaUso())
						: null,
				bloques, new PublicacionDatos.Responsables(nombre(contenido.getAutor()), revisorClinico,
						nombre(publicadoPor)));
		this.versionRepository.saveAndFlush(new ContenidoVersion(contenido.getId(), version, contenido.getTipo(),
				contenido.getTitulo(), contenido.getSlug(), contenido.getAreaMedica(), extracto(contenido.getResumen()),
				contenido.getTiempoLecturaMinutos(), datos.proximaRevision(), this.jsonMapper.writeValueAsString(datos),
				(publicadoPor != null) ? publicadoPor.getId() : null, instante));
		for (UUID activoId : medios) {
			this.versionRepository.registrarMedio(contenido.getId(), version, activoId);
		}
		return version;
	}

	PublicacionDatos leer(String json) {
		return this.jsonMapper.readValue(json, PublicacionDatos.class);
	}

	/**
	 * Fecha a partir de la cual el contenido requiere actualización: la más cercana entre la vigencia y la próxima
	 * revisión de una GPC; para el resto, la próxima revisión o la fecha de revisión más la vigencia configurada.
	 */
	LocalDate proximaRevision(Contenido c) {
		LocalDate proxima = c.getProximaRevision();
		if (proxima == null && c.getFechaRevision() != null) {
			proxima = c.getFechaRevision().plusMonths(this.properties.admin().reviewValidityMonths());
		}
		if (c.getTipo() == TipoContenido.GUIA_PRACTICA_CLINICA && c.getVigenteHasta() != null
				&& (proxima == null || c.getVigenteHasta().isBefore(proxima))) {
			proxima = c.getVigenteHasta();
		}
		return proxima;
	}

	int mesesVigencia() {
		return this.properties.admin().reviewValidityMonths();
	}

	List<Recomendacion> recomendaciones(String json) {
		if (json == null || json.isBlank()) {
			return List.of();
		}
		return this.jsonMapper.readValue(json, new TypeReference<List<Recomendacion>>() {
		});
	}

	private Set<UUID> mediosDe(Contenido contenido) {
		Set<UUID> medios = new LinkedHashSet<>();
		for (ContenidoBloque bloque : this.bloqueRepository.findActivos(contenido.getId())) {
			medios.addAll(this.bloqueProcessor.leer(bloque.getTipo(), bloque.getDatos()).activos());
		}
		return medios;
	}

	static String extracto(String texto) {
		if (texto == null) {
			return null;
		}
		if (texto.length() <= 180) {
			return texto;
		}
		int corte = texto.lastIndexOf(' ', 180);
		return texto.substring(0, (corte > 0) ? corte : 180).replaceAll("[\\s.,;:]+$", "") + "…";
	}

	private static String nombre(Usuario usuario) {
		return (usuario != null) ? usuario.getNombre() + " " + usuario.getApellido() : null;
	}

}
