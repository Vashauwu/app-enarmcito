package com.empresa.medicina.audit;

/**
 * Acciones registradas en la bitácora.
 */
public enum AccionAuditoria {

	SESION_INICIADA("Inicio de sesión administrativo"),
	SESION_CERRADA("Cierre de sesión administrativo"),
	SESION_FALLIDA("Intento fallido de inicio de sesión administrativo"),

	USUARIO_INVITADO("Alta de cuenta por invitación"),
	USUARIO_ACTUALIZADO("Edición de datos de usuario"),
	USUARIO_TIER_CAMBIADO("Cambio de tier"),
	USUARIO_ROL_CAMBIADO("Cambio de rol"),
	USUARIO_BLOQUEADO("Bloqueo de cuenta"),
	USUARIO_DESBLOQUEADO("Desbloqueo de cuenta"),
	ACCESO_RESTABLECIMIENTO_GENERADO("Enlace de restablecimiento de acceso generado"),
	ACCESO_ENLACE_USADO("Contraseña definida con enlace de acceso"),
	SUPER_ADMIN_INICIAL("Alta del SUPER_ADMIN inicial"),

	CONTENIDO_CREADO("Creación de contenido"),
	CONTENIDO_EDITADO("Edición de contenido"),
	CONTENIDO_RESPONSABLES("Reasignación de autor o revisor"),
	CONTENIDO_ENVIADO_REVISION("Envío a revisión"),
	CONTENIDO_APROBADO("Revisión aprobada"),
	CONTENIDO_CAMBIOS_SOLICITADOS("Cambios solicitados en revisión"),
	CONTENIDO_PUBLICADO("Publicación"),
	CONTENIDO_ARCHIVADO("Archivado"),
	CONTENIDO_RESTAURADO("Restauración para edición"),
	CONTENIDO_REVISION_EDITORIAL("Dictamen de revisión editorial"),
	CONTENIDO_REVISION_CLINICA("Dictamen de revisión clínica"),
	CONTENIDO_REVISION_REQUERIDA("Marcado como «requiere actualización»"),
	CONTENIDO_ACCESO_CAMBIADO("Cambio de política de acceso"),
	CONTENIDO_TIPO_CAMBIADO("Cambio de tipo de contenido"),
	BLOQUE_CREADO("Bloque agregado"),
	BLOQUE_EDITADO("Bloque editado"),
	BLOQUE_RETIRADO("Bloque retirado"),
	BLOQUES_REORDENADOS("Bloques reordenados"),
	MEDIO_CARGADO("Carga de archivo multimedia"),
	MEDIO_ACTUALIZADO("Edición de metadatos de archivo"),
	MEDIO_APROBADO("Archivo aprobado para publicación"),
	MEDIO_RECHAZADO("Archivo rechazado"),
	MEDIO_ARCHIVADO("Archivo archivado");

	private final String descripcion;

	AccionAuditoria(String descripcion) {
		this.descripcion = descripcion;
	}

	public String descripcion() {
		return this.descripcion;
	}

}
