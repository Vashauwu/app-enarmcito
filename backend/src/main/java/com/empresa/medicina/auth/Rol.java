package com.empresa.medicina.auth;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.empresa.medicina.auth.Permiso.ADMIN_ACCESS;
import static com.empresa.medicina.auth.Permiso.ALGORITHMS_CLINICAL_REVIEW;
import static com.empresa.medicina.auth.Permiso.ALGORITHMS_CREATE;
import static com.empresa.medicina.auth.Permiso.AUDIT_READ;
import static com.empresa.medicina.auth.Permiso.AUDIT_READ_EDITORIAL;
import static com.empresa.medicina.auth.Permiso.AUDIT_READ_OWN;
import static com.empresa.medicina.auth.Permiso.BLOCKS_CREATE;
import static com.empresa.medicina.auth.Permiso.BLOCKS_REORDER;
import static com.empresa.medicina.auth.Permiso.BLOCKS_UPDATE_ANY;
import static com.empresa.medicina.auth.Permiso.BLOCKS_UPDATE_OWN;
import static com.empresa.medicina.auth.Permiso.CATALOGS_READ;
import static com.empresa.medicina.auth.Permiso.CATALOGS_UPDATE;
import static com.empresa.medicina.auth.Permiso.CONTENT_ACCESS_POLICY_ASSIGN;
import static com.empresa.medicina.auth.Permiso.CONTENT_ARCHIVE;
import static com.empresa.medicina.auth.Permiso.CONTENT_ARCHIVE_OWN;
import static com.empresa.medicina.auth.Permiso.CONTENT_ASSIGN;
import static com.empresa.medicina.auth.Permiso.CONTENT_CREATE;
import static com.empresa.medicina.auth.Permiso.CONTENT_PUBLISH;
import static com.empresa.medicina.auth.Permiso.CONTENT_READ_ADMIN;
import static com.empresa.medicina.auth.Permiso.CONTENT_REVIEW;
import static com.empresa.medicina.auth.Permiso.CONTENT_TYPE_ASSIGN;
import static com.empresa.medicina.auth.Permiso.CONTENT_UPDATE_ANY;
import static com.empresa.medicina.auth.Permiso.CONTENT_UPDATE_OWN;
import static com.empresa.medicina.auth.Permiso.GUIDES_ARCHIVE;
import static com.empresa.medicina.auth.Permiso.GUIDES_CLINICAL_REVIEW;
import static com.empresa.medicina.auth.Permiso.GUIDES_CREATE;
import static com.empresa.medicina.auth.Permiso.GUIDES_PUBLISH;
import static com.empresa.medicina.auth.Permiso.GUIDES_UPDATE_ANY;
import static com.empresa.medicina.auth.Permiso.GUIDES_UPDATE_OWN;
import static com.empresa.medicina.auth.Permiso.MEDIA_ARCHIVE;
import static com.empresa.medicina.auth.Permiso.MEDIA_PUBLISH;
import static com.empresa.medicina.auth.Permiso.MEDIA_REVIEW;
import static com.empresa.medicina.auth.Permiso.MEDIA_UPDATE;
import static com.empresa.medicina.auth.Permiso.MEDIA_UPLOAD;
import static com.empresa.medicina.auth.Permiso.USERS_ACCESS_RESET;
import static com.empresa.medicina.auth.Permiso.USERS_CREATE;
import static com.empresa.medicina.auth.Permiso.USERS_READ;
import static com.empresa.medicina.auth.Permiso.USERS_READ_STAFF;
import static com.empresa.medicina.auth.Permiso.USERS_ROLE_ASSIGN;
import static com.empresa.medicina.auth.Permiso.USERS_STATUS_UPDATE;
import static com.empresa.medicina.auth.Permiso.USERS_TIER_ASSIGN;
import static com.empresa.medicina.auth.Permiso.USERS_UPDATE;

/**
 * Rol principal de una cuenta (uno por cuenta) y los permisos que deriva. Traduce las matrices de
 * {@code docs/panel-administrativo.md} y {@code docs/modulo-guias-practica-clinica.md}. El tier sigue regulando
 * el acceso a guías y recursos restringidos; los artículos son gratuitos.
 */
public enum Rol {

	ALUMNO("Alumno", false, EnumSet.noneOf(Permiso.class)),

	AUTOR_CONTENIDO("Autor de contenido", false,
			EnumSet.of(ADMIN_ACCESS, CONTENT_READ_ADMIN, CONTENT_CREATE, CONTENT_UPDATE_OWN, CONTENT_ARCHIVE_OWN,
					GUIDES_CREATE, GUIDES_UPDATE_OWN, BLOCKS_CREATE, BLOCKS_UPDATE_OWN, BLOCKS_REORDER,
					ALGORITHMS_CREATE, MEDIA_UPLOAD, AUDIT_READ_OWN)),

	REVISOR_MEDICO("Revisor médico", false,
			EnumSet.of(ADMIN_ACCESS, CONTENT_READ_ADMIN, CONTENT_REVIEW, GUIDES_CLINICAL_REVIEW,
					ALGORITHMS_CLINICAL_REVIEW, AUDIT_READ_OWN)),

	EDITOR_CONTENIDO("Editor de contenido", false, Permisos.EDITORIAL),

	GESTOR_MULTIMEDIA("Gestor multimedia", false,
			EnumSet.of(ADMIN_ACCESS, MEDIA_UPLOAD, MEDIA_UPDATE, MEDIA_REVIEW, MEDIA_PUBLISH, MEDIA_ARCHIVE,
					AUDIT_READ_OWN)),

	SOPORTE("Soporte", false,
			EnumSet.of(ADMIN_ACCESS, USERS_READ, USERS_STATUS_UPDATE, USERS_ACCESS_RESET)),

	ADMIN_USUARIOS("Administrador de usuarios", true, Permisos.USUARIOS),

	ADMIN_PLATAFORMA("Administrador de plataforma", true, Permisos.plataforma()),

	SUPER_ADMIN("Superadministrador", true, EnumSet.allOf(Permiso.class));

	private final String nombre;

	private final boolean critico;

	private final Set<Permiso> permisos;

	Rol(String nombre, boolean critico, Set<Permiso> permisos) {
		this.nombre = nombre;
		this.critico = critico;
		this.permisos = Collections.unmodifiableSet(permisos);
	}

	public String nombre() {
		return this.nombre;
	}

	/** Roles administrativos críticos: sólo los asignan administradores de mayor alcance. */
	public boolean esCritico() {
		return this.critico;
	}

	public Set<Permiso> permisos() {
		return this.permisos;
	}

	public boolean tiene(Permiso permiso) {
		return this.permisos.contains(permiso);
	}

	/**
	 * Roles de las cuentas que este rol puede gestionar (editar, cambiar estado o rol). También son los roles
	 * que puede asignar: nunca uno superior al propio.
	 */
	public Set<Rol> rolesGestionables() {
		return switch (this) {
			case SUPER_ADMIN -> EnumSet.allOf(Rol.class);
			case ADMIN_PLATAFORMA -> EnumSet.complementOf(EnumSet.of(SUPER_ADMIN));
			case ADMIN_USUARIOS -> noCriticos();
			case SOPORTE -> EnumSet.of(ALUMNO);
			default -> EnumSet.noneOf(Rol.class);
		};
	}

	/** Roles con los que puede dar de alta cuentas nuevas. */
	public Set<Rol> rolesCreables() {
		if (this == SUPER_ADMIN) {
			return EnumSet.allOf(Rol.class);
		}
		if (!tiene(USERS_CREATE)) {
			return EnumSet.noneOf(Rol.class);
		}
		EnumSet<Rol> roles = EnumSet.copyOf(noCriticos());
		roles.retainAll(rolesGestionables());
		return roles;
	}

	public boolean puedeGestionar(Rol otro) {
		return rolesGestionables().contains(otro);
	}

	public static List<Rol> administrativos() {
		return Arrays.stream(values()).filter((rol) -> rol != ALUMNO).toList();
	}

	private static Set<Rol> noCriticos() {
		EnumSet<Rol> roles = EnumSet.noneOf(Rol.class);
		for (Rol rol : values()) {
			if (!rol.critico) {
				roles.add(rol);
			}
		}
		return roles;
	}

	private static final class Permisos {

		/** El editor publica y gestiona medios, pero no realiza la revisión clínica de una GPC. */
		static final Set<Permiso> EDITORIAL = EnumSet.of(ADMIN_ACCESS, CONTENT_READ_ADMIN, CONTENT_CREATE,
				CONTENT_UPDATE_OWN, CONTENT_UPDATE_ANY, CONTENT_ASSIGN, CONTENT_REVIEW, CONTENT_PUBLISH,
				CONTENT_ARCHIVE_OWN, CONTENT_ARCHIVE, CONTENT_TYPE_ASSIGN, GUIDES_CREATE, GUIDES_UPDATE_OWN,
				GUIDES_UPDATE_ANY, GUIDES_PUBLISH, GUIDES_ARCHIVE, BLOCKS_CREATE, BLOCKS_UPDATE_OWN, BLOCKS_UPDATE_ANY,
				BLOCKS_REORDER, ALGORITHMS_CREATE, MEDIA_UPLOAD, MEDIA_UPDATE, MEDIA_REVIEW, MEDIA_PUBLISH,
				MEDIA_ARCHIVE, AUDIT_READ_OWN, AUDIT_READ_EDITORIAL, CATALOGS_READ);

		static final Set<Permiso> USUARIOS = EnumSet.of(ADMIN_ACCESS, USERS_READ, USERS_READ_STAFF, USERS_CREATE,
				USERS_UPDATE, USERS_STATUS_UPDATE, USERS_ROLE_ASSIGN, USERS_TIER_ASSIGN, USERS_ACCESS_RESET,
				AUDIT_READ_OWN, AUDIT_READ, CATALOGS_READ);

		/** Administra catálogo y acceso por tier, pero nunca puede omitir la revisión clínica. */
		static Set<Permiso> plataforma() {
			EnumSet<Permiso> permisos = EnumSet.copyOf(EDITORIAL);
			permisos.addAll(USUARIOS);
			permisos.add(CATALOGS_UPDATE);
			permisos.add(CONTENT_ACCESS_POLICY_ASSIGN);
			return permisos;
		}

	}

}
