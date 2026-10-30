package com.empresa.medicina.content;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

import com.empresa.medicina.audit.AccionAuditoria;
import com.empresa.medicina.audit.Actor;
import com.empresa.medicina.audit.AuditService;
import com.empresa.medicina.audit.Cambios;
import com.empresa.medicina.audit.TipoRecurso;
import com.empresa.medicina.auth.Permiso;
import com.empresa.medicina.auth.Rol;
import com.empresa.medicina.common.ApiError;
import com.empresa.medicina.common.ApiException;
import com.empresa.medicina.common.PageResponse;
import com.empresa.medicina.common.Paginacion;
import com.empresa.medicina.config.AppProperties;
import com.empresa.medicina.content.bloques.BloqueDatos;
import com.empresa.medicina.content.bloques.BloqueProcessor;
import com.empresa.medicina.content.bloques.TipoBloque;
import com.empresa.medicina.media.MediaService;
import com.empresa.medicina.tiers.Tier;
import com.empresa.medicina.tiers.TierRepository;
import com.empresa.medicina.tiers.TierResponse;
import com.empresa.medicina.users.Usuario;
import com.empresa.medicina.users.UsuarioSistema;
import com.empresa.medicina.users.UsuarioSistema.EstadoCuenta;
import com.empresa.medicina.users.UsuarioSistemaRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestión editorial y clínica del contenido (artículos, GPC y recursos de estudio):
 * BORRADOR/EN_EDICION → EN_REVISION_EDITORIAL → [EN_REVISION_CLINICA] → APROBADO → PUBLICADO, con CAMBIOS_SOLICITADOS,
 * REVISION_REQUERIDA y ARCHIVADO. Publicar crea una versión pública inmutable; editar lo publicado no cambia lo que ve
 * el público hasta la siguiente publicación.
 */
@Service
@Transactional(readOnly = true)
public class EditorialService {

	static final int MAX_BUSQUEDA = 100;

	private final ContenidoRepository contenidoRepository;

	private final ContenidoRevisionRepository revisionRepository;

	private final ContenidoBloqueRepository bloqueRepository;

	private final ContenidoVersionRepository versionRepository;

	private final TierRepository tierRepository;

	private final UsuarioSistemaRepository cuentaRepository;

	private final EditorialValidator validator;

	private final PublicacionService publicacionService;

	private final BloqueProcessor bloqueProcessor;

	private final MediaService mediaService;

	private final AuditService auditService;

	private final JsonMapper jsonMapper;

	EditorialService(ContenidoRepository contenidoRepository, ContenidoRevisionRepository revisionRepository,
			ContenidoBloqueRepository bloqueRepository, ContenidoVersionRepository versionRepository,
			TierRepository tierRepository, UsuarioSistemaRepository cuentaRepository, EditorialValidator validator,
			PublicacionService publicacionService, BloqueProcessor bloqueProcessor, MediaService mediaService,
			AuditService auditService, JsonMapper jsonMapper, AppProperties properties) {
		this.contenidoRepository = contenidoRepository;
		this.revisionRepository = revisionRepository;
		this.bloqueRepository = bloqueRepository;
		this.versionRepository = versionRepository;
		this.tierRepository = tierRepository;
		this.cuentaRepository = cuentaRepository;
		this.validator = validator;
		this.publicacionService = publicacionService;
		this.bloqueProcessor = bloqueProcessor;
		this.mediaService = mediaService;
		this.auditService = auditService;
		this.jsonMapper = jsonMapper;
	}

	// =============================================================================================
	// Consulta
	// =============================================================================================

	public PageResponse<EditorialResponses.Fila> listar(UsuarioSistema actor, Filtro filtro, int pagina, int tamano) {
		String busqueda = Paginacion.busqueda(filtro.q(), MAX_BUSQUEDA);
		Specification<Contenido> spec = alcance(actor).and((root, query, cb) -> {
			List<jakarta.persistence.criteria.Predicate> predicados = new ArrayList<>();
			var autor = root.join("autor", JoinType.LEFT);
			var revisor = root.join("revisor", JoinType.LEFT);
			if (busqueda != null) {
				String patron = Paginacion.patronLike(sinAcentos(busqueda));
				predicados.add(cb.or(cb.like(cb.function("unaccent", String.class, cb.lower(root.get("titulo"))), patron),
						cb.like(root.get("slug"), patron)));
			}
			if (filtro.tipo() != null) {
				predicados.add(cb.equal(root.get("tipo"), filtro.tipo()));
			}
			if (filtro.estado() != null) {
				predicados.add(cb.equal(root.get("estado"), filtro.estado()));
			}
			if (filtro.politica() != null) {
				predicados.add(cb.equal(root.get("politicaAcceso"), filtro.politica()));
			}
			if (filtro.area() != null && !filtro.area().isBlank()) {
				predicados.add(cb.equal(root.get("areaMedica"), filtro.area().strip()));
			}
			if (filtro.autorId() != null) {
				predicados.add(cb.equal(autor.get("id"), filtro.autorId()));
			}
			if (filtro.revisorId() != null) {
				predicados.add(cb.equal(revisor.get("id"), filtro.revisorId()));
			}
			if (Boolean.TRUE.equals(filtro.revisionVencida())) {
				predicados.add(vencidaAntesDe(root, cb, LocalDate.now()));
			}
			return cb.and(predicados.toArray(jakarta.persistence.criteria.Predicate[]::new));
		});
		return PageResponse.of(this.contenidoRepository.findAll(spec,
				Paginacion.de(pagina, tamano, Sort.by(Sort.Direction.DESC, "updatedAt"))),
				(c) -> new EditorialResponses.Fila(c.getId(), c.getTipo(), c.getTitulo(), c.getSlug(), c.getAreaMedica(),
						c.getEstado(), c.getPoliticaAcceso(), tier(c.getTierMinimo()),
						EditorialResponses.Persona.of(c.getAutor()), EditorialResponses.Persona.of(c.getRevisor()),
						c.getFechaRevision(), vencida(c), c.getVersionPublicada(), c.isPublicacionActiva(),
						c.getUpdatedAt()));
	}

	/**
	 * Contenido por estado, pendientes de revisión y publicaciones vencidas o próximas a vencer, dentro del alcance
	 * de la persona.
	 */
	public Resumen resumen(UsuarioSistema actor, int diasAviso) {
		Specification<Contenido> alcance = alcance(actor);
		Map<String, Long> porEstado = new LinkedHashMap<>();
		for (EstadoContenido estado : EstadoContenido.values()) {
			porEstado.put(estado.name(), this.contenidoRepository.count(alcance.and(enEstado(estado))));
		}
		LocalDate hoy = LocalDate.now();
		long vencida = this.contenidoRepository
			.count(alcance.and((root, query, cb) -> vencidaAntesDe(root, cb, hoy)));
		long porVencer = this.contenidoRepository.count(alcance.and((root, query, cb) -> cb.and(
				cb.not(vencidaAntesDe(root, cb, hoy)), vencidaAntesDe(root, cb, hoy.plusDays(diasAviso + 1L)))));
		return new Resumen(porEstado,
				porEstado.get(EstadoContenido.EN_REVISION_EDITORIAL.name())
						+ porEstado.get(EstadoContenido.EN_REVISION_CLINICA.name()),
				vencida, porVencer);
	}

	public record Resumen(Map<String, Long> porEstado, long pendientesRevision, long revisionVencida,
			long revisionPorVencer) {
	}

	public EditorialResponses.Detalle detalle(UsuarioSistema actor, UUID id) {
		return aDetalle(cargarVisible(actor, id), actor);
	}

	public List<EditorialResponses.Revision> revisiones(UsuarioSistema actor, UUID id) {
		return historial(cargarVisible(actor, id).getId());
	}

	public List<String> areasMedicas() {
		return this.contenidoRepository.findAreasMedicas();
	}

	/** Personas que pueden figurar como autoras o revisoras. Sólo nombre y rol. */
	public EditorialResponses.Candidatos candidatos() {
		return new EditorialResponses.Candidatos(candidatosCon(Permiso.CONTENT_CREATE, Permiso.GUIDES_CREATE),
				candidatosCon(Permiso.CONTENT_REVIEW, Permiso.GUIDES_CLINICAL_REVIEW));
	}

	// =============================================================================================
	// Cabecera, tipo y acceso
	// =============================================================================================

	@Transactional
	public EditorialResponses.Detalle crear(UsuarioSistema actor, ContenidoRequest request) {
		TipoContenido tipo = (request.tipo() != null) ? request.tipo() : TipoContenido.ARTICULO;
		permitir(politica(actor).puedeCrear(tipo));
		DatosContenido datos = normalizar(request, tipo, null);
		this.validator.validarFormato(datos);
		Contenido contenido = new Contenido(tipo, tierPorTipo(tipo, null), datos, recomendacionesJson(datos),
				actor.getUsuario());
		guardar(contenido);
		this.auditService.registrar(Actor.of(actor), AccionAuditoria.CONTENIDO_CREADO, TipoRecurso.CONTENIDO,
				contenido.getId(), "Borrador creado (" + tipo + "): «" + contenido.getTitulo() + "»");
		return aDetalle(contenido, actor);
	}

	@Transactional
	public EditorialResponses.Detalle actualizar(UsuarioSistema actor, UUID id, ContenidoRequest request) {
		Contenido contenido = cargarVisible(actor, id);
		permitir(politica(actor).puedeEditar(contenido),
				"No puedes editar este contenido en su estado actual (" + contenido.getEstado() + ").");
		exigirVersion(contenido, request.version());
		DatosContenido datos = normalizar(request, contenido.getTipo(), contenido);
		this.validator.validarFormato(datos);
		if (contenido.getVersionPublicada() != null && !datos.slug().equals(contenido.getSlug())) {
			throw campo("SLUG_BLOQUEADO", "slug", "No se puede cambiar la URL de un contenido ya publicado.");
		}
		String recomendaciones = recomendacionesJson(datos);
		Cambios cambios = cambios(contenido, datos, recomendaciones);
		if (cambios.isEmpty()) {
			return aDetalle(contenido, actor);
		}
		contenido.aplicar(datos, recomendaciones);
		contenido.marcarEnEdicion();
		guardar(contenido);
		this.auditService.registrar(Actor.of(actor), AccionAuditoria.CONTENIDO_EDITADO, TipoRecurso.CONTENIDO,
				contenido.getId(), "Contenido editado: «" + contenido.getTitulo() + "»", null, cambios);
		return aDetalle(contenido, actor);
	}

	/** Cambiar el tipo devuelve el contenido a edición: debe completar los campos y el flujo del tipo nuevo. */
	@Transactional
	public EditorialResponses.Detalle cambiarTipo(UsuarioSistema actor, UUID id, EditorialRequests.CambioTipo request) {
		Contenido contenido = cargarVisible(actor, id);
		permitir(politica(actor).puedeCambiarTipo(contenido));
		TipoContenido anterior = contenido.getTipo();
		if (anterior == request.tipo()) {
			throw new ApiException(HttpStatus.CONFLICT, "SIN_CAMBIOS", "El contenido ya es de ese tipo.");
		}
		contenido.cambiarTipo(request.tipo(), tierPorTipo(request.tipo(), contenido.getTierMinimo()));
		contenido.marcarEnEdicion();
		guardar(contenido);
		this.auditService.registrar(Actor.of(actor), AccionAuditoria.CONTENIDO_TIPO_CAMBIADO, TipoRecurso.CONTENIDO,
				contenido.getId(), "Tipo cambiado: «" + contenido.getTitulo() + "»", request.motivo().strip(),
				Cambios.nuevo().campo("tipo", anterior, request.tipo()));
		return aDetalle(contenido, actor);
	}

	/**
	 * Tier que corresponde a un tipo: los artículos no tienen tier; las guías conservan el que tengan o, si no, el
	 * tier activo más bajo; los recursos de estudio siempre llevan el más alto (Premium).
	 */
	private Tier tierPorTipo(TipoContenido tipo, Tier actual) {
		return switch (tipo) {
			case ARTICULO -> null;
			case GUIA_PRACTICA_CLINICA -> (actual != null) ? actual
					: this.tierRepository.findFirstByActivoTrueOrderByOrdenAccesoAsc()
						.orElseThrow(this::sinTiers);
			case RECURSO_ESTUDIO -> this.tierRepository.findFirstByActivoTrueOrderByOrdenAccesoDesc()
				.orElseThrow(this::sinTiers);
		};
	}

	private ApiException sinTiers() {
		return new ApiException(HttpStatus.CONFLICT, "SIN_TIERS", "No hay tiers activos para asignar al contenido.");
	}

	/**
	 * Sólo las guías cambian de tier: son siempre de pago y el editor elige cuál. Los artículos son gratis con cuenta
	 * y los recursos de estudio van siempre en el tier más alto. La política se aplica en vivo, también a lo publicado.
	 */
	@Transactional
	public EditorialResponses.Detalle cambiarAcceso(UsuarioSistema actor, UUID id, EditorialRequests.Acceso request) {
		Contenido contenido = cargarVisible(actor, id);
		permitir(politica(actor).puedeCambiarAcceso(contenido));
		if (request.politica() != PoliticaAcceso.POR_TIER) {
			throw campo("POLITICA_INVALIDA", "politica", "Las guías de práctica clínica siempre requieren un tier.");
		}
		if (request.tierMinimoId() == null) {
			throw campo("TIER_REQUERIDO", "tierMinimoId", "Selecciona el tier mínimo.");
		}
		Tier tier = this.tierRepository.findByIdAndActivoTrue(request.tierMinimoId())
			.orElseThrow(() -> campo("TIER_INVALIDO", "tierMinimoId", "Selecciona un tier activo."));
		Cambios cambios = Cambios.nuevo()
			.campo("politica", contenido.getPoliticaAcceso(), request.politica())
			.campo("tierMinimo", (contenido.getTierMinimo() != null) ? contenido.getTierMinimo().getCodigo() : null,
					(tier != null) ? tier.getCodigo() : null);
		contenido.asignarAcceso(request.politica(), tier);
		guardar(contenido);
		this.auditService.registrar(Actor.of(actor), AccionAuditoria.CONTENIDO_ACCESO_CAMBIADO, TipoRecurso.CONTENIDO,
				contenido.getId(), "Acceso actualizado: «" + contenido.getTitulo() + "»", request.motivo().strip(),
				cambios);
		return aDetalle(contenido, actor);
	}

	@Transactional
	public EditorialResponses.Detalle asignarResponsables(UsuarioSistema actor, UUID id,
			EditorialRequests.Responsables request) {
		Contenido contenido = cargarVisible(actor, id);
		exigir(contenido.getEstado() != EstadoContenido.ARCHIVADO, "Restaura el contenido antes de reasignarlo.");
		permitir(politica(actor).puedeAsignar(contenido));
		Usuario autor = (request.autorId() != null)
				? candidato(request.autorId(), "autorId", Permiso.CONTENT_CREATE, Permiso.GUIDES_CREATE) : null;
		Usuario revisor = (request.revisorId() != null)
				? candidato(request.revisorId(), "revisorId", permisoClinico(contenido)) : null;
		validarSeparacion(autor, revisor);
		Cambios cambios = Cambios.nuevo()
			.campo("autor", nombre(contenido.getAutor()), nombre(autor))
			.campo("revisor", nombre(contenido.getRevisor()), nombre(revisor));
		contenido.asignarResponsables(autor, revisor);
		guardar(contenido);
		this.auditService.registrar(Actor.of(actor), AccionAuditoria.CONTENIDO_RESPONSABLES, TipoRecurso.CONTENIDO,
				contenido.getId(), "Responsables actualizados: «" + contenido.getTitulo() + "»",
				vacioANulo(request.motivo()), cambios);
		return aDetalle(contenido, actor);
	}

	// =============================================================================================
	// Bloques
	// =============================================================================================

	@Transactional
	public EditorialResponses.Detalle crearBloque(UsuarioSistema actor, UUID id, EditorialRequests.Bloque request) {
		Contenido contenido = cargarVisible(actor, id);
		permitir(politica(actor).puedeAgregarBloques(contenido, request.tipo() == TipoBloque.ALGORITMO),
				"No puedes agregar este bloque en el estado actual o con tu rol.");
		BloqueDatos datos = this.bloqueProcessor.procesar(request.tipo(), request.datos());
		List<ContenidoBloque> activos = this.bloqueRepository.findActivos(id);
		int posicion = (request.posicion() != null) ? Math.max(0, Math.min(request.posicion(), activos.size()))
				: activos.size();
		ContenidoBloque bloque = new ContenidoBloque(contenido, posicion, request.tipo(),
				this.bloqueProcessor.serializar(datos), actor.getUsuario());
		activos.add(posicion, bloque);
		renumerar(activos);
		this.bloqueRepository.saveAll(activos);
		this.bloqueRepository.flush();
		vincularMedios(contenido, bloque, datos);
		tocarContenido(contenido);
		this.auditService.registrar(Actor.of(actor), AccionAuditoria.BLOQUE_CREADO, TipoRecurso.CONTENIDO,
				contenido.getId(), "Bloque " + request.tipo() + " agregado en «" + contenido.getTitulo() + "»");
		return aDetalle(contenido, actor);
	}

	@Transactional
	public EditorialResponses.Detalle actualizarBloque(UsuarioSistema actor, UUID id, UUID bloqueId,
			EditorialRequests.Bloque request) {
		Contenido contenido = cargarVisible(actor, id);
		ContenidoBloque bloque = bloqueActivo(contenido, bloqueId);
		permitir(politica(actor).puedeEditarBloque(contenido, bloque),
				"No puedes editar este bloque en el estado actual o con tu rol.");
		if (bloque.getTipo() != request.tipo()) {
			throw campo("TIPO_BLOQUE", "tipo", "No se puede cambiar el tipo de un bloque; crea uno nuevo.");
		}
		BloqueDatos datos = this.bloqueProcessor.procesar(request.tipo(), request.datos());
		String json = this.bloqueProcessor.serializar(datos);
		if (json.equals(bloque.getDatos())) {
			return aDetalle(contenido, actor);
		}
		bloque.actualizar(json);
		this.bloqueRepository.saveAndFlush(bloque);
		vincularMedios(contenido, bloque, datos);
		tocarContenido(contenido);
		this.auditService.registrar(Actor.of(actor), AccionAuditoria.BLOQUE_EDITADO, TipoRecurso.CONTENIDO,
				contenido.getId(), "Bloque " + bloque.getTipo() + " editado en «" + contenido.getTitulo() + "»");
		return aDetalle(contenido, actor);
	}

	/** Retirar no borra el bloque: deja de mostrarse y se conserva para la trazabilidad. */
	@Transactional
	public EditorialResponses.Detalle retirarBloque(UsuarioSistema actor, UUID id, UUID bloqueId) {
		Contenido contenido = cargarVisible(actor, id);
		ContenidoBloque bloque = bloqueActivo(contenido, bloqueId);
		permitir(politica(actor).puedeEditarBloque(contenido, bloque),
				"No puedes retirar este bloque en el estado actual o con tu rol.");
		bloque.retirar();
		this.bloqueRepository.desvincularMedios(bloqueId);
		List<ContenidoBloque> activos = this.bloqueRepository.findActivos(id)
			.stream()
			.filter((b) -> !b.getId().equals(bloqueId))
			.toList();
		renumerar(activos);
		this.bloqueRepository.save(bloque);
		this.bloqueRepository.saveAllAndFlush(activos);
		tocarContenido(contenido);
		this.auditService.registrar(Actor.of(actor), AccionAuditoria.BLOQUE_RETIRADO, TipoRecurso.CONTENIDO,
				contenido.getId(), "Bloque " + bloque.getTipo() + " retirado de «" + contenido.getTitulo() + "»");
		return aDetalle(contenido, actor);
	}

	@Transactional
	public EditorialResponses.Detalle reordenarBloques(UsuarioSistema actor, UUID id,
			EditorialRequests.OrdenBloques request) {
		Contenido contenido = cargarVisible(actor, id);
		permitir(politica(actor).puedeReordenar(contenido));
		List<ContenidoBloque> activos = this.bloqueRepository.findActivos(id);
		Map<UUID, ContenidoBloque> porId = new HashMap<>();
		activos.forEach((b) -> porId.put(b.getId(), b));
		if (request.bloques().size() != activos.size() || !porId.keySet().containsAll(request.bloques())) {
			throw ApiException.badRequest("ORDEN_INVALIDO", "El orden debe incluir exactamente los bloques actuales.");
		}
		List<ContenidoBloque> ordenados = request.bloques().stream().map(porId::get).toList();
		renumerar(ordenados);
		this.bloqueRepository.saveAllAndFlush(ordenados);
		tocarContenido(contenido);
		this.auditService.registrar(Actor.of(actor), AccionAuditoria.BLOQUES_REORDENADOS, TipoRecurso.CONTENIDO,
				contenido.getId(), "Bloques reordenados en «" + contenido.getTitulo() + "»");
		return aDetalle(contenido, actor);
	}

	// =============================================================================================
	// Transiciones
	// =============================================================================================

	@Transactional
	public EditorialResponses.Detalle enviarARevision(UsuarioSistema actor, UUID id,
			EditorialRequests.EnviarRevision request) {
		Contenido contenido = cargarVisible(actor, id);
		exigir(EstadoContenido.ENVIABLES.contains(contenido.getEstado()),
				"Sólo se envía a revisión contenido en borrador, en edición o con cambios solicitados.");
		permitir(politica(actor).puedeEnviarARevision(contenido));
		Usuario revisor = (request.revisorId() != null)
				? candidato(request.revisorId(), "revisorId", permisoClinico(contenido)) : contenido.getRevisor();
		validarSeparacion(contenido.getAutor(), revisor);
		validarCompleto(contenido);
		Cambios cambios = Cambios.nuevo()
			.campo("estado", contenido.getEstado(), EstadoContenido.EN_REVISION_EDITORIAL)
			.campo("revisor", nombre(contenido.getRevisor()), nombre(revisor));
		contenido.asignarResponsables(contenido.getAutor(), revisor);
		contenido.enviarARevisionEditorial(Instant.now());
		guardar(contenido);
		this.auditService.registrar(Actor.of(actor), AccionAuditoria.CONTENIDO_ENVIADO_REVISION, TipoRecurso.CONTENIDO,
				contenido.getId(), "Enviado a revisión editorial: «" + contenido.getTitulo() + "»",
				vacioANulo(request.comentario()), cambios);
		return aDetalle(contenido, actor);
	}

	@Transactional
	public EditorialResponses.Detalle revisar(UsuarioSistema actor, UUID id, EditorialRequests.Revision request) {
		Contenido contenido = cargarVisible(actor, id);
		boolean aprobar = request.decision() == EditorialRequests.DecisionRevision.APROBAR;
		String comentario = vacioANulo(request.comentario());
		if (!aprobar && (comentario == null || comentario.length() < 10)) {
			throw campo("COMENTARIO_REQUERIDO", "comentario",
					"Explica qué cambios se necesitan (al menos 10 caracteres).");
		}
		EstadoContenido anterior = contenido.getEstado();
		EditorialPolicy policy = politica(actor);
		if (request.etapa() == ContenidoRevision.Etapa.EDITORIAL) {
			exigir(anterior == EstadoContenido.EN_REVISION_EDITORIAL, "El contenido no está en revisión editorial.");
			permitir(policy.puedeRevisarEditorial(contenido));
			if (aprobar) {
				validarCompleto(contenido);
				if (requiereRevisionClinica(contenido)) {
					if (contenido.getRevisor() == null) {
						throw campo("REVISOR_REQUERIDO", "revisorId",
								"Asigna un revisor clínico antes de validar la presentación.");
					}
					contenido.pasarARevisionClinica(actor.getUsuario());
				}
				else {
					contenido.aprobar(Instant.now(), actor.getUsuario());
				}
			}
			else {
				contenido.solicitarCambios();
			}
		}
		else {
			exigir(anterior == EstadoContenido.EN_REVISION_CLINICA, "El contenido no está en revisión clínica.");
			if (contenido.esAutor(actor.getUsuarioId())) {
				throw ApiException.forbidden("SEPARACION_REQUERIDA",
						"La revisión clínica debe hacerla una persona distinta del autor.");
			}
			permitir(policy.puedeRevisarClinica(contenido, tieneAlgoritmo(contenido)),
					"La revisión clínica corresponde al revisor asignado con el permiso clínico adecuado.");
			if (aprobar) {
				validarCompleto(contenido);
				contenido.aprobar(Instant.now(), null);
			}
			else {
				contenido.solicitarCambios();
			}
		}
		this.revisionRepository.save(new ContenidoRevision(contenido, actor.getUsuario(), request.etapa(),
				aprobar ? ContenidoRevision.Decision.APROBADO : ContenidoRevision.Decision.CAMBIOS_SOLICITADOS,
				comentario));
		guardar(contenido);
		this.auditService.registrar(Actor.of(actor),
				(request.etapa() == ContenidoRevision.Etapa.EDITORIAL) ? AccionAuditoria.CONTENIDO_REVISION_EDITORIAL
						: AccionAuditoria.CONTENIDO_REVISION_CLINICA,
				TipoRecurso.CONTENIDO, contenido.getId(),
				(aprobar ? "Aprobado en revisión " : "Cambios solicitados en revisión ")
						+ request.etapa().name().toLowerCase() + ": «" + contenido.getTitulo() + "»",
				comentario, Cambios.nuevo().campo("estado", anterior, contenido.getEstado()));
		return aDetalle(contenido, actor);
	}

	@Transactional
	public EditorialResponses.Detalle publicar(UsuarioSistema actor, UUID id) {
		Contenido contenido = cargarVisible(actor, id);
		exigir(contenido.getEstado() == EstadoContenido.APROBADO, "Sólo se publica contenido aprobado.");
		permitir(politica(actor).puedePublicar(contenido));
		int bloques = this.bloqueRepository.findActivos(id).size();
		int recomendaciones = this.publicacionService.recomendaciones(contenido.getRecomendaciones()).size();
		this.validator.validarPublicable(contenido, bloques, recomendaciones,
				this.publicacionService.proximaRevision(contenido));
		exigirAprobacionClinica(contenido);
		Instant ahora = Instant.now();
		int version = this.publicacionService.publicar(contenido, actor.getUsuario(), ahora);
		Integer anterior = contenido.getVersionPublicada();
		contenido.publicar(version, ahora, actor.getUsuario());
		guardar(contenido);
		this.auditService.registrar(Actor.of(actor), AccionAuditoria.CONTENIDO_PUBLICADO, TipoRecurso.CONTENIDO,
				contenido.getId(), "Publicado (versión " + version + "): «" + contenido.getTitulo() + "»", null,
				Cambios.nuevo().campo("estado", EstadoContenido.APROBADO, EstadoContenido.PUBLICADO)
					.campo("versionPublicada", anterior, version));
		return aDetalle(contenido, actor);
	}

	/** Cambió la fuente o se detectó un problema: la publicación sigue visible con aviso hasta actualizarla. */
	@Transactional
	public EditorialResponses.Detalle marcarRevisionRequerida(UsuarioSistema actor, UUID id, String motivo) {
		Contenido contenido = cargarVisible(actor, id);
		exigir(contenido.getEstado() == EstadoContenido.PUBLICADO, "Sólo se marca contenido publicado.");
		permitir(politica(actor).puedeMarcarRevisionRequerida(contenido));
		contenido.marcarRevisionRequerida();
		guardar(contenido);
		this.auditService.registrar(Actor.of(actor), AccionAuditoria.CONTENIDO_REVISION_REQUERIDA,
				TipoRecurso.CONTENIDO, contenido.getId(), "Requiere actualización: «" + contenido.getTitulo() + "»",
				motivo.strip(),
				Cambios.nuevo().campo("estado", EstadoContenido.PUBLICADO, EstadoContenido.REVISION_REQUERIDA));
		return aDetalle(contenido, actor);
	}

	/** Archivar retira la publicación de los catálogos; conserva historial, versiones y progreso. */
	@Transactional
	public EditorialResponses.Detalle archivar(UsuarioSistema actor, UUID id, String motivo) {
		Contenido contenido = cargarVisible(actor, id);
		exigir(contenido.getEstado() != EstadoContenido.ARCHIVADO, "El contenido ya está archivado.");
		permitir(politica(actor).puedeArchivar(contenido));
		EstadoContenido anterior = contenido.getEstado();
		contenido.archivar(Instant.now());
		guardar(contenido);
		this.auditService.registrar(Actor.of(actor), AccionAuditoria.CONTENIDO_ARCHIVADO, TipoRecurso.CONTENIDO,
				contenido.getId(), "Archivado: «" + contenido.getTitulo() + "»", motivo.strip(),
				Cambios.nuevo().campo("estado", anterior, EstadoContenido.ARCHIVADO));
		return aDetalle(contenido, actor);
	}

	@Transactional
	public EditorialResponses.Detalle restaurar(UsuarioSistema actor, UUID id, String motivo) {
		Contenido contenido = cargarVisible(actor, id);
		exigir(contenido.getEstado() == EstadoContenido.ARCHIVADO, "Sólo se restaura contenido archivado.");
		permitir(politica(actor).puedeRestaurar(contenido));
		contenido.restaurar();
		guardar(contenido);
		this.auditService.registrar(Actor.of(actor), AccionAuditoria.CONTENIDO_RESTAURADO, TipoRecurso.CONTENIDO,
				contenido.getId(), "Restaurado para edición: «" + contenido.getTitulo() + "»", motivo.strip(),
				Cambios.nuevo().campo("estado", EstadoContenido.ARCHIVADO, EstadoContenido.EN_EDICION));
		return aDetalle(contenido, actor);
	}

	// =============================================================================================
	// Tareas del sistema
	// =============================================================================================

	/** Marca como «requiere actualización» las publicaciones con vigencia o revisión vencida. */
	@Transactional
	public int marcarVencidas() {
		LocalDate hoy = LocalDate.now();
		List<Contenido> vencidos = this.contenidoRepository.findAll((root, query, cb) -> cb
			.and(cb.equal(root.get("estado"), EstadoContenido.PUBLICADO), vencidaAntesDe(root, cb, hoy)));
		for (Contenido contenido : vencidos) {
			contenido.marcarRevisionRequerida();
			this.auditService.registrar(null, AccionAuditoria.CONTENIDO_REVISION_REQUERIDA, TipoRecurso.CONTENIDO,
					contenido.getId(), "Vigencia o revisión vencida: «" + contenido.getTitulo() + "»");
		}
		this.contenidoRepository.saveAll(vencidos);
		return vencidos.size();
	}

	/**
	 * Crea la versión pública de contenido marcado como publicado que aún no la tiene (migración del MVP y datos
	 * demo). No requiere revisión porque reproduce lo que ya estaba publicado.
	 */
	@Transactional
	public int publicarExistentes() {
		List<Contenido> pendientes = this.contenidoRepository.findAll((root, query, cb) -> cb.and(
				cb.isTrue(root.get("publicacionActiva")), cb.isNull(root.get("versionPublicada"))));
		for (Contenido contenido : pendientes) {
			Instant instante = (contenido.getPublicadoAt() != null) ? contenido.getPublicadoAt() : Instant.now();
			int version = this.publicacionService.publicar(contenido, contenido.getAutor(), instante);
			contenido.publicar(version, instante, contenido.getEditor());
		}
		this.contenidoRepository.saveAll(pendientes);
		return pendientes.size();
	}

	// =============================================================================================
	// Apoyo
	// =============================================================================================

	private EditorialPolicy politica(UsuarioSistema actor) {
		return new EditorialPolicy(actor.getRol(), actor.getUsuarioId());
	}

	/** Contenido fuera del alcance de la persona responde igual que uno inexistente. */
	private Contenido cargarVisible(UsuarioSistema actor, UUID id) {
		return this.contenidoRepository.findParaEdicion(id)
			.filter(politica(actor)::puedeVer)
			.orElseThrow(() -> ApiException.notFound("CONTENIDO_NO_ENCONTRADO", "El contenido no existe."));
	}

	private ContenidoBloque bloqueActivo(Contenido contenido, UUID bloqueId) {
		return this.bloqueRepository.findById(bloqueId)
			.filter((b) -> b.getEstado() == ContenidoBloque.Estado.ACTIVO)
			.filter((b) -> this.bloqueRepository.findActivos(contenido.getId()).stream()
				.anyMatch((a) -> a.getId().equals(bloqueId)))
			.orElseThrow(() -> ApiException.notFound("BLOQUE_NO_ENCONTRADO", "El bloque no existe."));
	}

	private Specification<Contenido> alcance(UsuarioSistema actor) {
		if (politica(actor).veTodo()) {
			return (root, query, cb) -> cb.conjunction();
		}
		Rol rol = actor.getRol();
		return (root, query, cb) -> {
			List<jakarta.persistence.criteria.Predicate> alcance = new ArrayList<>();
			if (rol.tiene(Permiso.CONTENT_CREATE) || rol.tiene(Permiso.GUIDES_CREATE)) {
				alcance.add(cb.equal(root.join("autor", JoinType.LEFT).get("id"), actor.getUsuarioId()));
			}
			if (rol.tiene(Permiso.CONTENT_REVIEW) || rol.tiene(Permiso.GUIDES_CLINICAL_REVIEW)) {
				alcance.add(cb.equal(root.join("revisor", JoinType.LEFT).get("id"), actor.getUsuarioId()));
			}
			return alcance.isEmpty() ? cb.disjunction()
					: cb.or(alcance.toArray(jakarta.persistence.criteria.Predicate[]::new));
		};
	}

	private static Specification<Contenido> enEstado(EstadoContenido estado) {
		return (root, query, cb) -> cb.equal(root.get("estado"), estado);
	}

	/**
	 * Publicación en circulación cuya próxima revisión (o vigencia de la GPC) es anterior a {@code fecha}. Si no hay
	 * próxima revisión explícita se usa la fecha de revisión más la vigencia configurada.
	 */
	private jakarta.persistence.criteria.Predicate vencidaAntesDe(Root<Contenido> root, CriteriaBuilder cb,
			LocalDate fecha) {
		LocalDate limiteRevision = fecha.minusMonths(this.publicacionService.mesesVigencia());
		return cb.and(cb.isTrue(root.get("publicacionActiva")),
				cb.or(cb.lessThan(root.get("proximaRevision"), fecha),
						cb.and(cb.isNull(root.get("proximaRevision")),
								cb.lessThan(root.get("fechaRevision"), limiteRevision)),
						cb.lessThan(root.get("vigenteHasta"), fecha)));
	}

	private boolean vencida(Contenido c) {
		LocalDate proxima = this.publicacionService.proximaRevision(c);
		return c.isPublicacionActiva() && proxima != null && proxima.isBefore(LocalDate.now());
	}

	private boolean tieneAlgoritmo(Contenido c) {
		return this.bloqueRepository.findActivos(c.getId())
			.stream()
			.anyMatch((b) -> b.getTipo() == TipoBloque.ALGORITMO);
	}

	/** GPC siempre; los demás si así se marcó o si contienen un algoritmo clínico. */
	private boolean requiereRevisionClinica(Contenido c) {
		return c.isRevisionClinicaRequerida() || tieneAlgoritmo(c);
	}

	private void validarCompleto(Contenido c) {
		this.validator.validarCompleto(c, this.bloqueRepository.findActivos(c.getId()).size(),
				this.publicacionService.recomendaciones(c.getRecomendaciones()).size());
	}

	/** Defensa adicional: una revisión clínica requerida debe constar, hecha por alguien distinto del autor. */
	private void exigirAprobacionClinica(Contenido c) {
		if (!requiereRevisionClinica(c)) {
			return;
		}
		int version = ((c.getVersionPublicada() != null) ? c.getVersionPublicada() : 0) + 1;
		boolean valida = this.revisionRepository
			.findDecisiones(c.getId(), version, ContenidoRevision.Etapa.CLINICA, ContenidoRevision.Decision.APROBADO)
			.stream()
			.anyMatch((r) -> r.getRevisor() != null && !c.esAutor(r.getRevisor().getId()));
		if (!valida) {
			throw new ApiException(HttpStatus.CONFLICT, "REVISION_CLINICA_REQUERIDA",
					"La publicación requiere una aprobación clínica de una persona distinta del autor.");
		}
	}

	/** Tras cambiar bloques, la copia de trabajo vuelve a edición (lo publicado sigue igual). */
	private void tocarContenido(Contenido contenido) {
		contenido.marcarEnEdicion();
		guardar(contenido);
	}

	private void vincularMedios(Contenido contenido, ContenidoBloque bloque, BloqueDatos datos) {
		this.bloqueRepository.desvincularMedios(bloque.getId());
		for (UUID activoId : datos.activos()) {
			this.bloqueRepository.vincularMedio(contenido.getId(), bloque.getId(), activoId, bloque.getTipo().name());
		}
	}

	private static void renumerar(List<ContenidoBloque> bloques) {
		for (int i = 0; i < bloques.size(); i++) {
			bloques.get(i).mover(i);
		}
	}

	private void guardar(Contenido contenido) {
		try {
			this.contenidoRepository.saveAndFlush(contenido);
		}
		catch (DataIntegrityViolationException ex) {
			if (String.valueOf(ex.getMostSpecificCause().getMessage()).contains("uk_contenido_slug")) {
				throw campo("SLUG_DUPLICADO", "slug", "Ya existe otro contenido con esa URL.");
			}
			throw ex;
		}
	}

	private void exigirVersion(Contenido contenido, Long version) {
		if (version == null || !version.equals(contenido.getVersion())) {
			throw new ApiException(HttpStatus.CONFLICT, "CONFLICTO_EDICION",
					"El contenido cambió desde que lo abriste. Recarga para ver la versión actual.");
		}
	}

	private DatosContenido normalizar(ContenidoRequest r, TipoContenido tipo, Contenido actual) {
		String titulo = r.titulo().strip();
		String slug = vacioANulo(r.slug());
		if (slug == null) {
			slug = (actual != null) ? actual.getSlug() : slugDisponible(EditorialValidator.slugDesde(titulo));
		}
		else {
			boolean ocupado = (actual != null) ? this.contenidoRepository.existsBySlugAndIdNot(slug, actual.getId())
					: this.contenidoRepository.existsBySlug(slug);
			if (ocupado) {
				throw campo("SLUG_DUPLICADO", "slug", "Ya existe otro contenido con esa URL.");
			}
		}
		List<String> puntos = (r.puntosClave() != null) ? r.puntosClave()
			.stream()
			.filter(Objects::nonNull)
			.map((p) -> p.replaceAll("\\s+", " ").strip())
			.filter(Predicate.not(String::isEmpty))
			.toList() : List.of();
		DatosContenido.Guia guia = (tipo == TipoContenido.GUIA_PRACTICA_CLINICA)
				? new DatosContenido.Guia(vacioANulo(r.institucionEmisora()), vacioANulo(r.fuenteEdicion()),
						vacioANulo(r.poblacionObjetivo()), vacioANulo(r.contextoAtencion()),
						vacioANulo(r.alcanceClinico()), r.vigenteHasta(), r.proximaRevision(),
						(r.recomendaciones() != null) ? r.recomendaciones()
							.stream()
							.map((rec) -> new Recomendacion(rec.texto().strip(), vacioANulo(rec.fuerza()),
									vacioANulo(rec.nivelEvidencia()), vacioANulo(rec.referencia())))
							.toList() : List.of())
				: null;
		DatosContenido.Recurso recurso = (tipo == TipoContenido.RECURSO_ESTUDIO)
				? new DatosContenido.Recurso(vacioANulo(r.procedencia()), vacioANulo(r.titularDerechos()),
						vacioANulo(r.licenciaUso()), Boolean.TRUE.equals(r.autorizacionPublicacion()),
						vacioANulo(r.autorizacionReferencia()))
				: null;
		boolean clinica = (r.revisionClinicaRequerida() != null) ? r.revisionClinicaRequerida()
				: (actual != null) ? actual.isRevisionClinicaRequerida() : tipo != TipoContenido.RECURSO_ESTUDIO;
		return new DatosContenido(titulo, slug, vacioANulo(r.areaMedica()), vacioANulo(r.objetivoAprendizaje()),
				vacioANulo(r.resumen()), puntos, vacioANulo(r.aplicacionClinica()), vacioANulo(r.fuenteTitulo()),
				vacioANulo(r.fuenteAutores()), vacioANulo(r.fuenteUrl()), vacioANulo(r.fuenteDoi()),
				r.fechaPublicacionFuente(), r.fechaRevision(), r.tiempoLecturaMinutos(), clinica, guia, recurso);
	}

	private String recomendacionesJson(DatosContenido datos) {
		if (datos.guia() == null || datos.guia().recomendaciones().isEmpty()) {
			return null;
		}
		return this.jsonMapper.writeValueAsString(datos.guia().recomendaciones());
	}

	private String slugDisponible(String base) {
		String slug = base;
		for (int sufijo = 2; this.contenidoRepository.existsBySlug(slug); sufijo++) {
			slug = base + "-" + sufijo;
		}
		return slug;
	}

	private static Cambios cambios(Contenido c, DatosContenido d, String recomendaciones) {
		DatosContenido.Guia g = d.guia();
		DatosContenido.Recurso r = d.recurso();
		return Cambios.nuevo()
			.campo("titulo", c.getTitulo(), d.titulo())
			.campo("slug", c.getSlug(), d.slug())
			.campo("areaMedica", c.getAreaMedica(), d.areaMedica())
			.texto("objetivoAprendizaje", c.getObjetivoAprendizaje(), d.objetivoAprendizaje())
			.texto("resumen", c.getResumen(), d.resumen())
			.texto("puntosClave", String.join("\n", c.getPuntosClaveLista()), String.join("\n", d.puntosClave()))
			.texto("aplicacionClinica", c.getAplicacionClinica(), d.aplicacionClinica())
			.campo("fuenteTitulo", c.getFuenteTitulo(), d.fuenteTitulo())
			.campo("fuenteAutores", c.getFuenteAutores(), d.fuenteAutores())
			.campo("fuenteUrl", c.getFuenteUrl(), d.fuenteUrl())
			.campo("fuenteDoi", c.getFuenteDoi(), d.fuenteDoi())
			.campo("fechaPublicacionFuente", c.getFechaPublicacionFuente(), d.fechaPublicacionFuente())
			.campo("fechaRevision", c.getFechaRevision(), d.fechaRevision())
			.campo("tiempoLecturaMinutos", c.getTiempoLecturaMinutos(), d.tiempoLecturaMinutos())
			.campo("revisionClinicaRequerida", c.isRevisionClinicaRequerida(),
					c.getTipo() == TipoContenido.GUIA_PRACTICA_CLINICA || d.revisionClinicaRequerida())
			.campo("institucionEmisora", c.getInstitucionEmisora(), (g != null) ? g.institucionEmisora() : null)
			.campo("fuenteEdicion", c.getFuenteEdicion(), (g != null) ? g.fuenteEdicion() : null)
			.texto("poblacionObjetivo", c.getPoblacionObjetivo(), (g != null) ? g.poblacionObjetivo() : null)
			.texto("contextoAtencion", c.getContextoAtencion(), (g != null) ? g.contextoAtencion() : null)
			.texto("alcanceClinico", c.getAlcanceClinico(), (g != null) ? g.alcanceClinico() : null)
			.campo("vigenteHasta", c.getVigenteHasta(), (g != null) ? g.vigenteHasta() : null)
			.campo("proximaRevision", c.getProximaRevision(), (g != null) ? g.proximaRevision() : null)
			.texto("recomendaciones", c.getRecomendaciones(), recomendaciones)
			.texto("procedencia", c.getProcedencia(), (r != null) ? r.procedencia() : null)
			.campo("titularDerechos", c.getTitularDerechos(), (r != null) ? r.titularDerechos() : null)
			.campo("licenciaUso", c.getLicenciaUso(), (r != null) ? r.licenciaUso() : null)
			.campo("autorizacionPublicacion", c.isAutorizacionPublicacion(), r != null && r.autorizacionPublicacion())
			.texto("autorizacionReferencia", c.getAutorizacionReferencia(),
					(r != null) ? r.autorizacionReferencia() : null);
	}

	private Usuario candidato(UUID usuarioId, String campo, Permiso... permisos) {
		return this.cuentaRepository.findConPerfilYTier(usuarioId)
			.filter((cuenta) -> cuenta.getEstado() == EstadoCuenta.ACTIVO
					&& Arrays.stream(permisos).anyMatch(cuenta.getRol()::tiene))
			.map(UsuarioSistema::getUsuario)
			.orElseThrow(() -> campo("RESPONSABLE_INVALIDO", campo,
					"Elige una persona activa con el rol adecuado para esta tarea."));
	}

	/** Permiso que debe tener el revisor asignado según el tipo de contenido. */
	private static Permiso permisoClinico(Contenido c) {
		return (c.getTipo() == TipoContenido.GUIA_PRACTICA_CLINICA) ? Permiso.GUIDES_CLINICAL_REVIEW
				: Permiso.CONTENT_REVIEW;
	}

	private List<EditorialResponses.PersonaConRol> candidatosCon(Permiso... permisos) {
		List<Rol> roles = Arrays.stream(Rol.values())
			.filter((rol) -> Arrays.stream(permisos).anyMatch(rol::tiene))
			.toList();
		return this.cuentaRepository.findActivosConRol(roles, EstadoCuenta.ACTIVO)
			.stream()
			.map((cuenta) -> new EditorialResponses.PersonaConRol(cuenta.getUsuarioId(), nombre(cuenta.getUsuario()),
					cuenta.getRol().name()))
			.toList();
	}

	private static void validarSeparacion(Usuario autor, Usuario revisor) {
		if (autor != null && revisor != null && autor.getId().equals(revisor.getId())) {
			throw campo("SEPARACION_REQUERIDA", "revisorId", "El revisor debe ser una persona distinta del autor.");
		}
	}

	private List<EditorialResponses.Revision> historial(UUID contenidoId) {
		return this.revisionRepository.findHistorial(contenidoId)
			.stream()
			.map((r) -> new EditorialResponses.Revision(r.getId(), r.getEtapa(), r.getVersionContenido(),
					r.getDecision(), r.getComentario(), EditorialResponses.Persona.of(r.getRevisor()),
					r.getCreatedAt()))
			.toList();
	}

	private EditorialResponses.Detalle aDetalle(Contenido c, UsuarioSistema actor) {
		EditorialPolicy policy = politica(actor);
		List<ContenidoBloque> activos = this.bloqueRepository.findActivos(c.getId());
		boolean algoritmo = activos.stream().anyMatch((b) -> b.getTipo() == TipoBloque.ALGORITMO);
		List<EditorialResponses.Bloque> bloques = activos.stream().map((b) -> {
			BloqueDatos datos = this.bloqueProcessor.leer(b.getTipo(), b.getDatos());
			List<EditorialResponses.MedioBloque> medios = datos.activos()
				.stream()
				.map((activoId) -> this.mediaService.referencia(activoId)
					.map((m) -> new EditorialResponses.MedioBloque(m.id(), m.tipoMime(), m.estado(), m.textoAlternativo()))
					.orElse(null))
				.filter(Objects::nonNull)
				.toList();
			return new EditorialResponses.Bloque(b.getId(), b.getTipo(), b.getPosicion(),
					this.bloqueProcessor.aNodo(b.getDatos()), nombre(b.getAutor()), b.getUpdatedAt(), medios,
					policy.puedeEditarBloque(c, b));
		}).toList();
		List<EditorialResponses.VersionPublicada> versiones = this.versionRepository.findHistorial(c.getId())
			.stream()
			.map((v) -> new EditorialResponses.VersionPublicada(v.getVersion(), v.getPublicadoAt(),
					(v.getPublicadoPor() != null)
							? this.cuentaRepository.findConPerfilYTier(v.getPublicadoPor())
								.map((cuenta) -> nombre(cuenta.getUsuario()))
								.orElse(null)
							: null))
			.toList();
		LocalDate proxima = this.publicacionService.proximaRevision(c);
		boolean cambiosSinPublicar = c.getVersionPublicada() != null && c.getEstado() != EstadoContenido.PUBLICADO
				&& c.getEstado() != EstadoContenido.ARCHIVADO;
		return new EditorialResponses.Detalle(c.getId(), c.getTipo(), c.getPoliticaAcceso(), tier(c.getTierMinimo()),
				c.getTitulo(), c.getSlug(), c.getAreaMedica(), c.getObjetivoAprendizaje(), c.getResumen(),
				c.getPuntosClaveLista(), c.getAplicacionClinica(), c.getMetodoAprendizaje().name(), c.getFuenteTitulo(),
				c.getFuenteAutores(), c.getFuenteUrl(), c.getFuenteDoi(), c.getFechaPublicacionFuente(),
				c.getFechaRevision(), c.getTiempoLecturaMinutos(), c.isRevisionClinicaRequerida(),
				c.isRevisionClinicaRequerida() || algoritmo, c.getInstitucionEmisora(), c.getFuenteEdicion(),
				c.getPoblacionObjetivo(), c.getContextoAtencion(), c.getAlcanceClinico(), c.getVigenteHasta(),
				c.getProximaRevision(), this.publicacionService.recomendaciones(c.getRecomendaciones()),
				c.getProcedencia(), c.getTitularDerechos(), c.getLicenciaUso(), c.isAutorizacionPublicacion(),
				c.getAutorizacionReferencia(), c.getEstado(), EditorialResponses.Persona.of(c.getAutor()),
				EditorialResponses.Persona.of(c.getRevisor()), EditorialResponses.Persona.of(c.getEditor()),
				c.getEnviadoRevisionAt(), c.getAprobadoAt(), c.getPublicadoAt(), c.getArchivadoAt(), c.getCreatedAt(),
				c.getUpdatedAt(), c.getVersion(), c.getVersionPublicada(), c.isPublicacionActiva(), cambiosSinPublicar,
				vencida(c), proxima, bloques, historial(c.getId()), versiones, policy.acciones(c, algoritmo));
	}

	private static TierResponse tier(Tier tier) {
		return (tier != null) ? TierResponse.from(tier) : null;
	}

	/** Transición no válida para el estado actual. */
	private static void exigir(boolean valido, String mensaje) {
		if (!valido) {
			throw new ApiException(HttpStatus.CONFLICT, "TRANSICION_INVALIDA", mensaje);
		}
	}

	private static void permitir(boolean permitido) {
		permitir(permitido, "No tienes permiso para realizar esta acción.");
	}

	private static void permitir(boolean permitido, String mensaje) {
		if (!permitido) {
			throw ApiException.forbidden("ACCESO_DENEGADO", mensaje);
		}
	}

	private static ApiException campo(String codigo, String campo, String mensaje) {
		return new ApiException(HttpStatus.BAD_REQUEST, codigo, mensaje, List.of(new ApiError.FieldError(campo, mensaje)));
	}

	private static String nombre(Usuario usuario) {
		return (usuario != null) ? usuario.getNombre() + " " + usuario.getApellido() : null;
	}

	private static String vacioANulo(String texto) {
		if (texto == null) {
			return null;
		}
		String limpio = texto.strip();
		return limpio.isEmpty() ? null : limpio;
	}

	static String sinAcentos(String texto) {
		return java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
	}

	public record Filtro(String q, TipoContenido tipo, EstadoContenido estado, PoliticaAcceso politica, String area,
			UUID autorId, UUID revisorId, Boolean revisionVencida) {
	}

}
