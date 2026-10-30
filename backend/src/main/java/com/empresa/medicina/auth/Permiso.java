package com.empresa.medicina.auth;

/**
 * Permisos atómicos de la API. El código es la autoridad que comprueba Spring Security
 * ({@code hasAuthority('users.read')}).
 */
public enum Permiso {

	ADMIN_ACCESS("admin.access"),

	USERS_READ("users.read"),
	/** Ver cuentas del personal (roles distintos de ALUMNO) y desgloses por rol. */
	USERS_READ_STAFF("users.read.staff"),
	USERS_CREATE("users.create"),
	USERS_UPDATE("users.update"),
	USERS_STATUS_UPDATE("users.status.update"),
	USERS_ROLE_ASSIGN("users.role.assign"),
	USERS_TIER_ASSIGN("users.tier.assign"),
	USERS_ACCESS_RESET("users.access.reset"),

	CONTENT_READ_ADMIN("content.read.admin"),
	CONTENT_CREATE("content.create"),
	CONTENT_UPDATE_OWN("content.update.own"),
	CONTENT_UPDATE_ANY("content.update.any"),
	CONTENT_ASSIGN("content.assign"),
	CONTENT_REVIEW("content.review"),
	CONTENT_PUBLISH("content.publish"),
	CONTENT_ARCHIVE_OWN("content.archive.own"),
	CONTENT_ARCHIVE("content.archive"),

	CONTENT_TYPE_ASSIGN("content.type.assign"),
	CONTENT_ACCESS_POLICY_ASSIGN("content.access-policy.assign"),

	GUIDES_CREATE("guides.create"),
	GUIDES_UPDATE_OWN("guides.update.own"),
	GUIDES_UPDATE_ANY("guides.update.any"),
	GUIDES_CLINICAL_REVIEW("guides.clinical-review"),
	GUIDES_PUBLISH("guides.publish"),
	GUIDES_ARCHIVE("guides.archive"),

	MEDIA_UPLOAD("media.upload"),
	MEDIA_UPDATE("media.update"),
	MEDIA_REVIEW("media.review"),
	MEDIA_PUBLISH("media.publish"),
	MEDIA_ARCHIVE("media.archive"),

	BLOCKS_CREATE("blocks.create"),
	BLOCKS_UPDATE_OWN("blocks.update.own"),
	BLOCKS_UPDATE_ANY("blocks.update.any"),
	BLOCKS_REORDER("blocks.reorder"),

	ALGORITHMS_CREATE("algorithms.create"),
	ALGORITHMS_CLINICAL_REVIEW("algorithms.clinical-review"),

	AUDIT_READ_OWN("audit.read.own"),
	AUDIT_READ_EDITORIAL("audit.read.editorial"),
	AUDIT_READ("audit.read"),

	CATALOGS_READ("catalogs.read"),
	CATALOGS_UPDATE("catalogs.update"),

	SETTINGS_READ("settings.read"),
	SETTINGS_UPDATE("settings.update");

	private final String codigo;

	Permiso(String codigo) {
		this.codigo = codigo;
	}

	public String codigo() {
		return this.codigo;
	}

}
