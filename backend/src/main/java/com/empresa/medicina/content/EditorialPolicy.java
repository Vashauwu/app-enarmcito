package com.empresa.medicina.content;

import java.util.UUID;

import com.empresa.medicina.auth.Permiso;
import com.empresa.medicina.auth.Rol;

import static com.empresa.medicina.content.EstadoContenido.APROBADO;
import static com.empresa.medicina.content.EstadoContenido.ARCHIVADO;
import static com.empresa.medicina.content.EstadoContenido.EDITABLES;
import static com.empresa.medicina.content.EstadoContenido.ENVIABLES;
import static com.empresa.medicina.content.EstadoContenido.EN_REVISION_CLINICA;
import static com.empresa.medicina.content.EstadoContenido.EN_REVISION_EDITORIAL;
import static com.empresa.medicina.content.EstadoContenido.PUBLICADO;

/**
 * Reglas de autorización del flujo editorial y clínico sobre un contenido concreto. Combina el permiso del rol
 * (distinto para artículos y para guías o recursos) con la relación de la persona con el contenido y su estado.
 */
final class EditorialPolicy {

	private final Rol rol;

	private final UUID usuarioId;

	EditorialPolicy(Rol rol, UUID usuarioId) {
		this.rol = rol;
		this.usuarioId = usuarioId;
	}

	/** Editores y administradores ven todo; autores lo propio; revisores lo asignado. */
	boolean veTodo() {
		return this.rol.tiene(Permiso.CONTENT_UPDATE_ANY) || this.rol.tiene(Permiso.GUIDES_UPDATE_ANY);
	}

	boolean puedeCrear(TipoContenido tipo) {
		return this.rol.tiene((tipo == TipoContenido.ARTICULO) ? Permiso.CONTENT_CREATE : Permiso.GUIDES_CREATE);
	}

	boolean puedeVer(Contenido c) {
		return veTodo() || (puedeCrear(c.getTipo()) && c.esAutor(this.usuarioId))
				|| ((this.rol.tiene(Permiso.CONTENT_REVIEW) || this.rol.tiene(Permiso.GUIDES_CLINICAL_REVIEW))
						&& c.esRevisor(this.usuarioId));
	}

	boolean puedeEditar(Contenido c) {
		return EDITABLES.contains(c.getEstado()) && (editaCualquiera(c) || editaPropio(c));
	}

	boolean puedeAgregarBloques(Contenido c, boolean algoritmo) {
		return puedeEditar(c) && this.rol.tiene(Permiso.BLOCKS_CREATE)
				&& (!algoritmo || this.rol.tiene(Permiso.ALGORITHMS_CREATE));
	}

	boolean puedeEditarBloque(Contenido c, ContenidoBloque bloque) {
		if (!puedeEditar(c)) {
			return false;
		}
		return this.rol.tiene(Permiso.BLOCKS_UPDATE_ANY)
				|| (this.rol.tiene(Permiso.BLOCKS_UPDATE_OWN) && (bloque.esDe(this.usuarioId) || c.esAutor(this.usuarioId)));
	}

	boolean puedeReordenar(Contenido c) {
		return puedeEditar(c) && this.rol.tiene(Permiso.BLOCKS_REORDER);
	}

	boolean puedeEnviarARevision(Contenido c) {
		return ENVIABLES.contains(c.getEstado()) && (editaCualquiera(c) || editaPropio(c));
	}

	/** El editor valida presentación, fuentes y créditos. */
	boolean puedeRevisarEditorial(Contenido c) {
		return c.getEstado() == EN_REVISION_EDITORIAL && editaCualquiera(c);
	}

	/**
	 * Revisión clínica: la hace el revisor asignado, con el permiso clínico del tipo (y el de algoritmos si los hay),
	 * y nunca sobre contenido propio. Un editor sólo puede sustituirla en artículos.
	 */
	boolean puedeRevisarClinica(Contenido c, boolean tieneAlgoritmo) {
		if (c.getEstado() != EN_REVISION_CLINICA || c.esAutor(this.usuarioId)) {
			return false;
		}
		boolean permisoClinico = this.rol.tiene((c.getTipo() == TipoContenido.GUIA_PRACTICA_CLINICA)
				? Permiso.GUIDES_CLINICAL_REVIEW : Permiso.CONTENT_REVIEW);
		if (tieneAlgoritmo) {
			permisoClinico = permisoClinico && this.rol.tiene(Permiso.ALGORITHMS_CLINICAL_REVIEW);
		}
		boolean asignado = c.esRevisor(this.usuarioId)
				|| (c.getTipo() == TipoContenido.ARTICULO && this.rol.tiene(Permiso.CONTENT_UPDATE_ANY));
		return permisoClinico && asignado;
	}

	boolean puedePublicar(Contenido c) {
		return c.getEstado() == APROBADO && this.rol.tiene(
				c.getTipo().esGuiaORecurso() ? Permiso.GUIDES_PUBLISH : Permiso.CONTENT_PUBLISH);
	}

	boolean puedeMarcarRevisionRequerida(Contenido c) {
		return c.getEstado() == PUBLICADO && editaCualquiera(c);
	}

	boolean puedeArchivar(Contenido c) {
		if (c.getEstado() == ARCHIVADO) {
			return false;
		}
		return archivaCualquiera(c) || (this.rol.tiene(Permiso.CONTENT_ARCHIVE_OWN) && c.esAutor(this.usuarioId)
				&& c.getVersionPublicada() == null && EDITABLES.contains(c.getEstado()));
	}

	/** Un autor sólo restaura contenido propio que nunca llegó a publicarse. */
	boolean puedeRestaurar(Contenido c) {
		if (c.getEstado() != ARCHIVADO) {
			return false;
		}
		return archivaCualquiera(c) || (this.rol.tiene(Permiso.CONTENT_ARCHIVE_OWN) && c.esAutor(this.usuarioId)
				&& c.getVersionPublicada() == null);
	}

	boolean puedeAsignar(Contenido c) {
		return this.rol.tiene(Permiso.CONTENT_ASSIGN) && c.getEstado() != ARCHIVADO;
	}

	boolean puedeCambiarTipo(Contenido c) {
		return this.rol.tiene(Permiso.CONTENT_TYPE_ASSIGN) && puedeEditar(c);
	}

	boolean puedeCambiarAcceso(Contenido c) {
		return this.rol.tiene(Permiso.CONTENT_ACCESS_POLICY_ASSIGN)
				&& c.getTipo() == TipoContenido.GUIA_PRACTICA_CLINICA && c.getEstado() != ARCHIVADO;
	}

	EditorialResponses.Acciones acciones(Contenido c, boolean tieneAlgoritmo) {
		return new EditorialResponses.Acciones(puedeEditar(c), puedeAgregarBloques(c, false), puedeReordenar(c),
				puedeEnviarARevision(c), puedeRevisarEditorial(c), puedeRevisarClinica(c, tieneAlgoritmo),
				puedePublicar(c), puedeMarcarRevisionRequerida(c), puedeArchivar(c), puedeRestaurar(c),
				puedeAsignar(c), puedeCambiarTipo(c), puedeCambiarAcceso(c),
				puedeEditar(c) && this.rol.tiene(Permiso.ALGORITHMS_CREATE));
	}

	private boolean editaCualquiera(Contenido c) {
		return this.rol.tiene(c.getTipo().esGuiaORecurso() ? Permiso.GUIDES_UPDATE_ANY : Permiso.CONTENT_UPDATE_ANY);
	}

	private boolean editaPropio(Contenido c) {
		return this.rol.tiene(c.getTipo().esGuiaORecurso() ? Permiso.GUIDES_UPDATE_OWN : Permiso.CONTENT_UPDATE_OWN)
				&& c.esAutor(this.usuarioId);
	}

	private boolean archivaCualquiera(Contenido c) {
		return this.rol.tiene(c.getTipo().esGuiaORecurso() ? Permiso.GUIDES_ARCHIVE : Permiso.CONTENT_ARCHIVE);
	}

}
