package com.empresa.medicina.admin;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.empresa.medicina.audit.AuditEventResponse;
import com.empresa.medicina.audit.AuditQueryService;
import com.empresa.medicina.auth.Permiso;
import com.empresa.medicina.auth.Rol;
import com.empresa.medicina.common.ApiException;
import com.empresa.medicina.common.PageResponse;
import com.empresa.medicina.config.AppProperties;
import com.empresa.medicina.content.EditorialService;
import com.empresa.medicina.users.UsuarioSistema;
import com.empresa.medicina.users.UsuarioSistema.EstadoCuenta;
import com.empresa.medicina.users.UsuarioSistemaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tablero administrativo y actividad de usuarios. Sólo agregados, y sólo las secciones que el rol puede ver.
 * Un usuario es «activo» si su {@code last_login_at} cae dentro del periodo configurado
 * ({@code app.admin.active-user-days}, 30 días por defecto).
 */
@Service
@Transactional(readOnly = true)
public class AdminInsightsService {

	static final List<Integer> PERIODOS = List.of(7, 30, 90);

	private final UsuarioSistemaRepository cuentaRepository;

	private final AdminUserService adminUserService;

	private final EditorialService editorialService;

	private final AuditQueryService auditQueryService;

	private final AppProperties properties;

	AdminInsightsService(UsuarioSistemaRepository cuentaRepository, AdminUserService adminUserService,
			EditorialService editorialService, AuditQueryService auditQueryService, AppProperties properties) {
		this.cuentaRepository = cuentaRepository;
		this.adminUserService = adminUserService;
		this.editorialService = editorialService;
		this.auditQueryService = auditQueryService;
		this.properties = properties;
	}

	public Dashboard dashboard(UsuarioSistema actor) {
		Rol rol = actor.getRol();
		int dias = this.properties.admin().activeUserDays();
		Usuarios usuarios = null;
		if (rol.tiene(Permiso.USERS_READ)) {
			Instant desde = Instant.now().minus(Duration.ofDays(dias));
			usuarios = new Usuarios(dias, contar(actor, new AdminUserService.Filtro(null, null, null, null, null, null,
					desde, null)),
					contar(actor, new AdminUserService.Filtro(null, null, null, null,
							LocalDate.now(AdminTime.ZONA).minusDays(dias), null, null, null)),
					contar(actor, filtroEstado(EstadoCuenta.BLOQUEADO)),
					contar(actor, filtroEstado(EstadoCuenta.PENDIENTE)));
		}
		EditorialService.Resumen contenido = rol.tiene(Permiso.CONTENT_READ_ADMIN)
				? this.editorialService.resumen(actor, this.properties.admin().reviewWarningDays()) : null;
		List<AuditEventResponse> recientes = this.auditQueryService.recientes(actor.getUsuarioId(), rol, 8);
		return new Dashboard(rol, rol.nombre(), usuarios, contenido, recientes);
	}

	public Actividad actividad(UsuarioSistema actor, int dias, Rol rol, Long tierId, EstadoCuenta estado, int pagina,
			int tamano) {
		if (!PERIODOS.contains(dias)) {
			throw ApiException.badRequest("PERIODO_INVALIDO", "El periodo debe ser de 7, 30 o 90 días.");
		}
		Instant desde = Instant.now().minus(Duration.ofDays(dias));
		boolean veStaff = actor.getRol().tiene(Permiso.USERS_READ_STAFF);
		AdminUserService.Filtro activos = new AdminUserService.Filtro(null, rol, tierId, estado, null, null, desde,
				"ultimoAcceso");
		Map<String, Long> porRol = new LinkedHashMap<>();
		if (veStaff) {
			for (Rol r : Rol.values()) {
				porRol.put(r.name(), contar(actor, new AdminUserService.Filtro(null, r, null, null, null, null, desde,
						null)));
			}
		}
		Map<String, Long> porTier = new LinkedHashMap<>();
		this.adminUserService.opciones(actor)
			.tiers()
			.forEach((tier) -> porTier.put(tier.nombre(), contar(actor,
					new AdminUserService.Filtro(null, null, tier.id(), null, null, null, desde, null))));
		Map<String, Long> porEstado = new LinkedHashMap<>();
		for (EstadoCuenta e : EstadoCuenta.values()) {
			porEstado.put(e.name(), contar(actor, filtroEstado(e)));
		}
		return new Actividad(dias, desde, contar(actor, new AdminUserService.Filtro(null, null, null, null, null, null,
				desde, null)), veStaff ? porRol : null, porTier, porEstado,
				this.adminUserService.listar(actor, activos, pagina, tamano));
	}

	private long contar(UsuarioSistema actor, AdminUserService.Filtro filtro) {
		return this.cuentaRepository.count(this.adminUserService.especificacion(actor, filtro));
	}

	private static AdminUserService.Filtro filtroEstado(EstadoCuenta estado) {
		return new AdminUserService.Filtro(null, null, null, estado, null, null, null, null);
	}

	public record Usuarios(int periodoDias, long activos, long nuevos, long bloqueados, long pendientes) {
	}

	public record Dashboard(Rol rol, String rolNombre, Usuarios usuarios, EditorialService.Resumen contenido,
			List<AuditEventResponse> actividadReciente) {
	}

	/**
	 * @param porRol    sólo para quien puede ver cuentas del personal
	 * @param porEstado cuentas por estado (todas, no sólo las activas)
	 */
	public record Actividad(int periodoDias, Instant desde, long totalActivos, Map<String, Long> porRol,
			Map<String, Long> porTier, Map<String, Long> porEstado, PageResponse<AdminUserDtos.Fila> usuarios) {
	}

}
