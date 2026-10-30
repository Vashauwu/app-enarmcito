package com.empresa.medicina.admin;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.empresa.medicina.audit.AccionAuditoria;
import com.empresa.medicina.audit.Actor;
import com.empresa.medicina.audit.AuditService;
import com.empresa.medicina.audit.Cambios;
import com.empresa.medicina.audit.TipoRecurso;
import com.empresa.medicina.auth.AccessLinkService;
import com.empresa.medicina.auth.Permiso;
import com.empresa.medicina.auth.Rol;
import com.empresa.medicina.common.ApiError;
import com.empresa.medicina.common.ApiException;
import com.empresa.medicina.common.PageResponse;
import com.empresa.medicina.common.Paginacion;
import com.empresa.medicina.progress.EstadoProgreso;
import com.empresa.medicina.progress.ProgresoContenidoRepository;
import com.empresa.medicina.tiers.Tier;
import com.empresa.medicina.tiers.TierResponse;
import com.empresa.medicina.tiers.TierService;
import com.empresa.medicina.users.Usuario;
import com.empresa.medicina.users.UsuarioRepository;
import com.empresa.medicina.users.UsuarioSistema;
import com.empresa.medicina.users.UsuarioSistema.EstadoCuenta;
import com.empresa.medicina.users.UsuarioSistemaRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administración de cuentas. Reglas transversales:
 * <ul>
 * <li>sólo se actúa sobre cuentas cuyo rol puede gestionar el rol de quien actúa;</li>
 * <li>nadie cambia su propio rol ni su propio estado;</li>
 * <li>siempre queda al menos un SUPER_ADMIN activo;</li>
 * <li>quien no tiene {@code users.read.staff} sólo ve cuentas de alumnos.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class AdminUserService {

	static final int MAX_BUSQUEDA = 100;

	private final UsuarioSistemaRepository cuentaRepository;

	private final UsuarioRepository usuarioRepository;

	private final ProgresoContenidoRepository progresoRepository;

	private final TierService tierService;

	private final AccessLinkService accessLinkService;

	private final AuditService auditService;

	AdminUserService(UsuarioSistemaRepository cuentaRepository, UsuarioRepository usuarioRepository,
			ProgresoContenidoRepository progresoRepository, TierService tierService,
			AccessLinkService accessLinkService, AuditService auditService) {
		this.cuentaRepository = cuentaRepository;
		this.usuarioRepository = usuarioRepository;
		this.progresoRepository = progresoRepository;
		this.tierService = tierService;
		this.accessLinkService = accessLinkService;
		this.auditService = auditService;
	}

	// ---------------------------------------------------------------------------------------------
	// Consulta
	// ---------------------------------------------------------------------------------------------

	public PageResponse<AdminUserDtos.Fila> listar(UsuarioSistema actor, Filtro filtro, int pagina, int tamano) {
		Sort orden = switch ((filtro.orden() != null) ? filtro.orden() : "recientes") {
			case "nombre" -> Sort.by("usuario.nombre", "usuario.apellido");
			case "ultimoAcceso" -> Sort.by(Sort.Order.desc("lastLoginAt").nullsLast());
			default -> Sort.by(Sort.Direction.DESC, "usuario.createdAt");
		};
		return PageResponse.of(this.cuentaRepository.findAll(especificacion(actor, filtro),
				Paginacion.de(pagina, tamano, orden)), AdminUserDtos.Fila::of);
	}

	Specification<UsuarioSistema> especificacion(UsuarioSistema actor, Filtro filtro) {
		String busqueda = Paginacion.busqueda(filtro.q(), MAX_BUSQUEDA);
		return (root, query, cb) -> {
			Join<UsuarioSistema, Usuario> usuario = esConteo(query.getResultType()) ? root.join("usuario")
					: fetchComoJoin(root);
			List<Predicate> predicados = new ArrayList<>();
			if (!actor.getRol().tiene(Permiso.USERS_READ_STAFF)) {
				predicados.add(cb.equal(root.get("rol"), Rol.ALUMNO));
			}
			if (busqueda != null) {
				String patron = Paginacion.patronLike(sinAcentos(busqueda));
				predicados.add(cb.or(
						cb.like(cb.function("unaccent", String.class, cb.lower(usuario.get("nombre"))), patron),
						cb.like(cb.function("unaccent", String.class, cb.lower(usuario.get("apellido"))), patron),
						cb.like(usuario.get("correo"), patron)));
			}
			if (filtro.rol() != null) {
				predicados.add(cb.equal(root.get("rol"), filtro.rol()));
			}
			if (filtro.tierId() != null) {
				predicados.add(cb.equal(root.get("tier").get("id"), filtro.tierId()));
			}
			if (filtro.estado() != null) {
				predicados.add(cb.equal(root.get("estado"), filtro.estado()));
			}
			if (filtro.altaDesde() != null) {
				predicados.add(cb.greaterThanOrEqualTo(usuario.get("createdAt"), inicio(filtro.altaDesde())));
			}
			if (filtro.altaHasta() != null) {
				predicados.add(cb.lessThan(usuario.get("createdAt"), inicio(filtro.altaHasta().plusDays(1))));
			}
			if (filtro.accesoDesde() != null) {
				predicados.add(cb.greaterThanOrEqualTo(root.get("lastLoginAt"), filtro.accesoDesde()));
			}
			return cb.and(predicados.toArray(Predicate[]::new));
		};
	}

	public AdminUserDtos.Detalle detalle(UsuarioSistema actor, UUID id) {
		return aDetalle(actor, cargarVisible(actor, id));
	}

	public AdminUserDtos.Opciones opciones(UsuarioSistema actor) {
		List<AdminUserDtos.OpcionRol> roles = actor.getRol().tiene(Permiso.USERS_READ_STAFF)
				? Arrays.stream(Rol.values()).map(AdminUserDtos.OpcionRol::of).toList()
				: List.of(AdminUserDtos.OpcionRol.of(Rol.ALUMNO));
		List<AdminUserDtos.OpcionRol> creables = Arrays.stream(Rol.values())
			.filter(actor.getRol().rolesCreables()::contains)
			.map(AdminUserDtos.OpcionRol::of)
			.toList();
		List<TierResponse> tiers = this.tierService.listarActivos();
		return new AdminUserDtos.Opciones(roles, creables, tiers);
	}

	// ---------------------------------------------------------------------------------------------
	// Modificaciones
	// ---------------------------------------------------------------------------------------------

	@Transactional
	public AdminUserDtos.Detalle actualizar(UsuarioSistema actor, UUID id, AdminUserDtos.ActualizarRequest request) {
		UsuarioSistema cuenta = cargarVisible(actor, id);
		exigirGestion(actor, cuenta);
		Usuario usuario = cuenta.getUsuario();
		boolean cambiaDatos = !usuario.getNombre().equals(request.nombre().strip())
				|| !usuario.getApellido().equals(request.apellido().strip());
		boolean cambiaTier = !cuenta.getTier().getId().equals(request.tierId());
		if (cambiaDatos) {
			permitir(actor.getRol().tiene(Permiso.USERS_UPDATE), "No tienes permiso para editar datos de usuarios.");
			Cambios cambios = Cambios.nuevo()
				.campo("nombre", usuario.getNombre(), request.nombre())
				.campo("apellido", usuario.getApellido(), request.apellido());
			usuario.actualizarNombre(request.nombre(), request.apellido());
			this.auditService.registrar(Actor.of(actor), AccionAuditoria.USUARIO_ACTUALIZADO, TipoRecurso.USUARIO,
					id, "Datos actualizados de " + usuario.getCorreo(), vacioANulo(request.motivo()), cambios);
		}
		if (cambiaTier) {
			permitir(actor.getRol().tiene(Permiso.USERS_TIER_ASSIGN), "No tienes permiso para cambiar tiers.");
			Tier tier = this.tierService.obtenerActivo(request.tierId());
			Cambios cambios = Cambios.nuevo().campo("tier", cuenta.getTier().getCodigo(), tier.getCodigo());
			cuenta.cambiarTier(tier);
			this.auditService.registrar(Actor.of(actor), AccionAuditoria.USUARIO_TIER_CAMBIADO, TipoRecurso.USUARIO,
					id, "Tier de " + usuario.getCorreo() + " cambiado a " + tier.getNombre(),
					vacioANulo(request.motivo()), cambios);
		}
		this.cuentaRepository.saveAndFlush(cuenta);
		return aDetalle(actor, cuenta);
	}

	@Transactional
	public AdminUserDtos.Detalle bloquear(UsuarioSistema actor, UUID id, String motivo) {
		UsuarioSistema cuenta = cargarVisible(actor, id);
		exigirCambioDeEstado(actor, cuenta);
		if (cuenta.getEstado() != EstadoCuenta.ACTIVO) {
			throw conflicto("ESTADO_INVALIDO", (cuenta.getEstado() == EstadoCuenta.PENDIENTE)
					? "La cuenta aún no se activa; puedes generar un nuevo enlace o dejarla pendiente."
					: "La cuenta ya está bloqueada.");
		}
		if (cuenta.getRol() == Rol.SUPER_ADMIN) {
			exigirOtroSuperAdmin(cuenta);
		}
		cuenta.bloquear(motivo.strip(), Instant.now());
		this.cuentaRepository.saveAndFlush(cuenta);
		this.auditService.registrar(Actor.of(actor), AccionAuditoria.USUARIO_BLOQUEADO, TipoRecurso.USUARIO, id,
				"Cuenta bloqueada: " + cuenta.getUsuario().getCorreo(), motivo.strip(),
				Cambios.nuevo().campo("estado", EstadoCuenta.ACTIVO, EstadoCuenta.BLOQUEADO));
		return aDetalle(actor, cuenta);
	}

	@Transactional
	public AdminUserDtos.Detalle desbloquear(UsuarioSistema actor, UUID id, String motivo) {
		UsuarioSistema cuenta = cargarVisible(actor, id);
		exigirCambioDeEstado(actor, cuenta);
		if (cuenta.getEstado() != EstadoCuenta.BLOQUEADO) {
			throw conflicto("ESTADO_INVALIDO", "La cuenta no está bloqueada.");
		}
		cuenta.desbloquear();
		this.cuentaRepository.saveAndFlush(cuenta);
		this.auditService.registrar(Actor.of(actor), AccionAuditoria.USUARIO_DESBLOQUEADO, TipoRecurso.USUARIO, id,
				"Cuenta desbloqueada: " + cuenta.getUsuario().getCorreo(), motivo.strip(),
				Cambios.nuevo().campo("estado", EstadoCuenta.BLOQUEADO, EstadoCuenta.ACTIVO));
		return aDetalle(actor, cuenta);
	}

	@Transactional
	public AdminUserDtos.Detalle cambiarRol(UsuarioSistema actor, UUID id, AdminUserDtos.RolRequest request) {
		UsuarioSistema cuenta = cargarVisible(actor, id);
		if (esLaMisma(actor, cuenta)) {
			throw ApiException.forbidden("ACCION_PROPIA", "No puedes cambiar tu propio rol.");
		}
		exigirGestion(actor, cuenta);
		Rol nuevo = request.rol();
		permitir(actor.getRol().puedeGestionar(nuevo), "No puedes asignar el rol " + nuevo.nombre() + ".");
		Rol anterior = cuenta.getRol();
		if (anterior == nuevo) {
			throw conflicto("SIN_CAMBIOS", "La cuenta ya tiene ese rol.");
		}
		if (anterior == Rol.SUPER_ADMIN && cuenta.isActiva()) {
			exigirOtroSuperAdmin(cuenta);
		}
		cuenta.cambiarRol(nuevo);
		this.cuentaRepository.saveAndFlush(cuenta);
		this.auditService.registrar(Actor.of(actor), AccionAuditoria.USUARIO_ROL_CAMBIADO, TipoRecurso.USUARIO, id,
				"Rol de " + cuenta.getUsuario().getCorreo() + " cambiado a " + nuevo.nombre(),
				request.motivo().strip(), Cambios.nuevo().campo("rol", anterior, nuevo));
		return aDetalle(actor, cuenta);
	}

	/** Alta controlada: la cuenta queda pendiente hasta que la persona define su contraseña con el enlace. */
	@Transactional
	public AdminUserDtos.ConEnlace invitar(UsuarioSistema actor, AdminUserDtos.InvitarRequest request) {
		permitir(actor.getRol().rolesCreables().contains(request.rol()),
				"No puedes crear cuentas con el rol " + request.rol().nombre() + ".");
		String correo = Usuario.normalizarCorreo(request.correo());
		if (this.usuarioRepository.existsByCorreo(correo)) {
			throw new ApiException(HttpStatus.CONFLICT, "CORREO_DUPLICADO", "Ya existe una cuenta con ese correo.",
					List.of(new ApiError.FieldError("correo", "Ya existe una cuenta con ese correo.")));
		}
		Tier tier = this.tierService.obtenerActivo(request.tierId());
		UsuarioSistema cuenta;
		try {
			Usuario usuario = this.usuarioRepository
				.saveAndFlush(new Usuario(request.nombre(), request.apellido(), correo));
			cuenta = this.cuentaRepository.saveAndFlush(UsuarioSistema.invitada(usuario, request.rol(), tier));
		}
		catch (DataIntegrityViolationException ex) {
			throw new ApiException(HttpStatus.CONFLICT, "CORREO_DUPLICADO", "Ya existe una cuenta con ese correo.",
					List.of(new ApiError.FieldError("correo", "Ya existe una cuenta con ese correo.")));
		}
		AccessLinkService.Enlace enlace = this.accessLinkService.generar(cuenta, actor.getUsuario());
		this.auditService.registrar(Actor.of(actor), AccionAuditoria.USUARIO_INVITADO, TipoRecurso.USUARIO,
				cuenta.getUsuarioId(), "Cuenta invitada: " + correo + " (" + request.rol().nombre() + ")", null,
				Cambios.nuevo().campo("rol", null, request.rol()).campo("tier", null, tier.getCodigo()));
		return new AdminUserDtos.ConEnlace(aDetalle(actor, cuenta), enlace);
	}

	@Transactional
	public AdminUserDtos.ConEnlace restablecerAcceso(UsuarioSistema actor, UUID id, String motivo) {
		UsuarioSistema cuenta = cargarVisible(actor, id);
		permitir(actor.getRol().tiene(Permiso.USERS_ACCESS_RESET), "No tienes permiso para restablecer accesos.");
		exigirGestion(actor, cuenta);
		AccessLinkService.Enlace enlace = this.accessLinkService.generar(cuenta, actor.getUsuario());
		this.auditService.registrar(Actor.of(actor), AccionAuditoria.ACCESO_RESTABLECIMIENTO_GENERADO,
				TipoRecurso.USUARIO, id, "Enlace de acceso generado para " + cuenta.getUsuario().getCorreo(),
				motivo.strip(), null);
		return new AdminUserDtos.ConEnlace(aDetalle(actor, cuenta), enlace);
	}

	// ---------------------------------------------------------------------------------------------
	// Apoyo
	// ---------------------------------------------------------------------------------------------

	/** Cuentas fuera del alcance de lectura responden como inexistentes. */
	private UsuarioSistema cargarVisible(UsuarioSistema actor, UUID id) {
		return this.cuentaRepository.findConPerfilYTier(id)
			.filter((cuenta) -> actor.getRol().tiene(Permiso.USERS_READ_STAFF) || cuenta.getRol() == Rol.ALUMNO)
			.orElseThrow(() -> ApiException.notFound("USUARIO_NO_ENCONTRADO", "El usuario no existe."));
	}

	private void exigirGestion(UsuarioSistema actor, UsuarioSistema cuenta) {
		permitir(actor.getRol().puedeGestionar(cuenta.getRol()),
				"No puedes administrar cuentas con el rol " + cuenta.getRol().nombre() + ".");
	}

	private void exigirCambioDeEstado(UsuarioSistema actor, UsuarioSistema cuenta) {
		if (esLaMisma(actor, cuenta)) {
			throw ApiException.forbidden("ACCION_PROPIA", "No puedes cambiar el estado de tu propia cuenta.");
		}
		permitir(actor.getRol().tiene(Permiso.USERS_STATUS_UPDATE), "No tienes permiso para cambiar estados.");
		exigirGestion(actor, cuenta);
	}

	/** Bloquea las filas de los SUPER_ADMIN activos y exige que quede al menos otro además de esta cuenta. */
	private void exigirOtroSuperAdmin(UsuarioSistema cuenta) {
		long otros = this.cuentaRepository.bloquearPorRolYEstado(Rol.SUPER_ADMIN, EstadoCuenta.ACTIVO)
			.stream()
			.filter((otro) -> !otro.getUsuarioId().equals(cuenta.getUsuarioId()))
			.count();
		if (otros == 0) {
			throw conflicto("ULTIMO_SUPER_ADMIN",
					"Debe quedar al menos un SUPER_ADMIN activo. Asigna el rol a otra cuenta antes de continuar.");
		}
	}

	private AdminUserDtos.Detalle aDetalle(UsuarioSistema actor, UsuarioSistema cuenta) {
		UUID id = cuenta.getUsuarioId();
		AdminUserDtos.Actividad actividad = new AdminUserDtos.Actividad(
				this.progresoRepository.countByIdUsuarioIdAndEstado(id, EstadoProgreso.COMPLETADO),
				this.progresoRepository.countByIdUsuarioIdAndEstado(id, EstadoProgreso.EN_PROGRESO),
				this.progresoRepository.findUltimaConsulta(id).orElse(null));
		Rol rol = actor.getRol();
		boolean gestiona = rol.puedeGestionar(cuenta.getRol());
		boolean propia = esLaMisma(actor, cuenta);
		boolean cambiarRol = gestiona && !propia && rol.tiene(Permiso.USERS_ROLE_ASSIGN);
		AdminUserDtos.Acciones acciones = new AdminUserDtos.Acciones(gestiona && rol.tiene(Permiso.USERS_UPDATE),
				gestiona && rol.tiene(Permiso.USERS_TIER_ASSIGN),
				gestiona && !propia && rol.tiene(Permiso.USERS_STATUS_UPDATE)
						&& cuenta.getEstado() != EstadoCuenta.PENDIENTE,
				cambiarRol, gestiona && rol.tiene(Permiso.USERS_ACCESS_RESET),
				cambiarRol ? Arrays.stream(Rol.values()).filter(rol::puedeGestionar).toList() : List.of());
		return new AdminUserDtos.Detalle(AdminUserDtos.Fila.of(cuenta), cuenta.getBloqueoMotivo(),
				cuenta.getBloqueadoAt(), actividad, acciones);
	}

	private static boolean esLaMisma(UsuarioSistema actor, UsuarioSistema cuenta) {
		return actor.getUsuarioId().equals(cuenta.getUsuarioId());
	}

	@SuppressWarnings("unchecked")
	private static Join<UsuarioSistema, Usuario> fetchComoJoin(jakarta.persistence.criteria.Root<UsuarioSistema> root) {
		root.fetch("tier");
		// En Hibernate un fetch también es un join: se reutiliza para filtrar sin duplicar la unión.
		return (Join<UsuarioSistema, Usuario>) (Object) root.fetch("usuario");
	}

	private static boolean esConteo(Class<?> tipo) {
		return Long.class.equals(tipo) || long.class.equals(tipo);
	}

	private static void permitir(boolean permitido, String mensaje) {
		if (!permitido) {
			throw ApiException.forbidden("ACCESO_DENEGADO", mensaje);
		}
	}

	private static ApiException conflicto(String codigo, String mensaje) {
		return new ApiException(HttpStatus.CONFLICT, codigo, mensaje);
	}

	private static Instant inicio(LocalDate fecha) {
		return fecha.atStartOfDay(AdminTime.ZONA).toInstant();
	}

	private static String vacioANulo(String texto) {
		return (texto == null || texto.isBlank()) ? null : texto.strip();
	}

	static String sinAcentos(String texto) {
		return java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
	}

	public record Filtro(String q, Rol rol, Long tierId, EstadoCuenta estado, LocalDate altaDesde,
			LocalDate altaHasta, Instant accesoDesde, String orden) {
	}

}
