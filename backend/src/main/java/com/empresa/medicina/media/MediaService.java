package com.empresa.medicina.media;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.empresa.medicina.audit.AccionAuditoria;
import com.empresa.medicina.audit.Actor;
import com.empresa.medicina.audit.AuditService;
import com.empresa.medicina.audit.Cambios;
import com.empresa.medicina.audit.TipoRecurso;
import com.empresa.medicina.auth.Permiso;
import com.empresa.medicina.common.ApiError;
import com.empresa.medicina.common.ApiException;
import com.empresa.medicina.common.PageResponse;
import com.empresa.medicina.common.Paginacion;
import com.empresa.medicina.users.UsuarioSistema;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Carga, revisión y entrega de medios. Un medio sólo se publica con un contenido cuando está {@code APROBADO}; la
 * entrega pública la decide el catálogo según la visibilidad del contenido que lo usa.
 */
@Service
@Transactional(readOnly = true)
public class MediaService {

	private final ActivoMultimediaRepository repository;

	private final MediaInspector inspector;

	private final MalwareScanner scanner;

	private final MediaStorage storage;

	private final AuditService auditService;

	MediaService(ActivoMultimediaRepository repository, MediaInspector inspector, MalwareScanner scanner,
			MediaStorage storage, AuditService auditService) {
		this.repository = repository;
		this.inspector = inspector;
		this.scanner = scanner;
		this.storage = storage;
		this.auditService = auditService;
	}

	// ---------------------------------------------------------------------------------------------
	// Consulta
	// ---------------------------------------------------------------------------------------------

	public PageResponse<MediaDtos.Medio> listar(UsuarioSistema actor, EstadoMedio estado, String tipoMime, String q,
			int pagina, int tamano) {
		String busqueda = Paginacion.busqueda(q, 100);
		boolean soloPropios = !actor.getRol().tiene(Permiso.MEDIA_UPDATE);
		return PageResponse.of(
				this.repository.buscar(estado, (tipoMime == null || tipoMime.isBlank()) ? null : tipoMime, soloPropios,
						actor.getUsuarioId(), (busqueda != null) ? Paginacion.patronLike(busqueda) : null,
						Paginacion.de(pagina, tamano, Sort.by(Sort.Direction.DESC, "createdAt"))),
				(activo) -> aDto(actor, activo));
	}

	public MediaDtos.Medio detalle(UsuarioSistema actor, UUID id) {
		return aDto(actor, cargarVisible(actor, id));
	}

	/** Información mínima para validar un bloque que referencia el medio. */
	public Optional<Referencia> referencia(UUID id) {
		return this.repository.findById(id)
			.map((a) -> new Referencia(a.getId(), a.getTipoMime(), a.getEstado(), a.getTextoAlternativo(), a.getPie(),
					a.getCredito(), a.getLicencia(), a.getAncho(), a.getAlto(), a.getRutaMovil() != null));
	}

	/**
	 * Archivo a entregar. Las imágenes JPG y PNG sólo se entregan como variantes recodificadas (sin metadatos); el
	 * original queda protegido. SVG y WebP se entregan tal como se guardaron (el SVG ya saneado).
	 */
	public Archivo archivo(UUID id, String variante, boolean permitirOriginal) {
		ActivoMultimedia activo = this.repository.findById(id)
			.orElseThrow(() -> ApiException.notFound("MEDIO_NO_ENCONTRADO", "El archivo no existe."));
		String clave = switch ((variante != null) ? variante : "escritorio") {
			case "movil" -> (activo.getRutaMovil() != null) ? activo.getRutaMovil() : activo.getRutaEscritorio();
			case "original" -> permitirOriginal ? activo.getRutaOriginal() : activo.getRutaEscritorio();
			default -> activo.getRutaEscritorio();
		};
		if (clave == null) {
			clave = activo.getRutaOriginal();
		}
		return new Archivo(this.storage.leer(clave), activo.getTipoMime(), activo.getNombreOriginal());
	}

	public boolean estaAprobado(UUID id) {
		return this.repository.findById(id).map((a) -> a.getEstado() == EstadoMedio.APROBADO).orElse(false);
	}

	// ---------------------------------------------------------------------------------------------
	// Carga y revisión
	// ---------------------------------------------------------------------------------------------

	@Transactional
	public MediaDtos.Medio cargar(UsuarioSistema actor, MultipartFile archivo, MediaDtos.MetadatosRequest metadatos) {
		validarDeclaraciones(metadatos);
		byte[] contenido;
		try {
			contenido = archivo.getBytes();
		}
		catch (IOException ex) {
			throw ApiException.badRequest("ARCHIVO_INVALIDO", "No se pudo leer el archivo.");
		}
		ArchivoInspeccionado inspeccionado = this.inspector.inspeccionar(contenido);
		if (inspeccionado.tipo().esImagen() && vacio(metadatos.textoAlternativo())) {
			throw campo("textoAlternativo", "Describe la imagen: el texto alternativo es obligatorio.");
		}
		this.scanner.analizar(contenido);

		UUID id = UUID.randomUUID();
		String extension = inspeccionado.tipo().extension();
		String original = this.storage.guardar(id, "original", extension, inspeccionado.contenido());
		String escritorio = (inspeccionado.varianteEscritorio() != null)
				? this.storage.guardar(id, "escritorio", extension, inspeccionado.varianteEscritorio()) : null;
		String movil = (inspeccionado.varianteMovil() != null)
				? this.storage.guardar(id, "movil", extension, inspeccionado.varianteMovil()) : null;
		ActivoMultimedia activo = new ActivoMultimedia(id, nombreSeguro(archivo.getOriginalFilename(), extension),
				inspeccionado, new ActivoMultimedia.Rutas(original, escritorio, movil), aMetadatos(metadatos),
				actor.getUsuario());
		this.repository.saveAndFlush(activo);
		this.auditService.registrar(Actor.of(actor), AccionAuditoria.MEDIO_CARGADO, TipoRecurso.MEDIO, id,
				"Archivo cargado: " + activo.getNombreOriginal() + " (" + activo.getTipoMime() + ")", null,
				Cambios.nuevo().campo("sha256", null, activo.getSha256()).campo("credito", null, activo.getCredito()));
		return aDto(actor, activo);
	}

	@Transactional
	public MediaDtos.Medio actualizar(UsuarioSistema actor, UUID id, MediaDtos.MetadatosRequest metadatos) {
		ActivoMultimedia activo = cargarVisible(actor, id);
		permitir(puedeEditar(actor, activo));
		if (metadatos.version() == null || !metadatos.version().equals(activo.getVersion())) {
			throw new ApiException(HttpStatus.CONFLICT, "CONFLICTO_EDICION",
					"El archivo cambió desde que lo abriste. Recarga para ver la versión actual.");
		}
		validarDeclaraciones(metadatos);
		if (activo.esImagen() && vacio(metadatos.textoAlternativo())) {
			throw campo("textoAlternativo", "Describe la imagen: el texto alternativo es obligatorio.");
		}
		ActivoMultimedia.Metadatos nuevos = aMetadatos(metadatos);
		Cambios cambios = Cambios.nuevo()
			.campo("textoAlternativo", activo.getTextoAlternativo(), nuevos.textoAlternativo())
			.campo("pie", activo.getPie(), nuevos.pie())
			.campo("credito", activo.getCredito(), nuevos.credito())
			.campo("licencia", activo.getLicencia(), nuevos.licencia());
		activo.actualizarMetadatos(nuevos);
		// Si quien edita no puede aprobar, el cambio debe revisarse de nuevo.
		if (activo.getEstado() == EstadoMedio.APROBADO && !actor.getRol().tiene(Permiso.MEDIA_PUBLISH)) {
			activo.volverAPendiente();
		}
		this.repository.saveAndFlush(activo);
		if (!cambios.isEmpty()) {
			this.auditService.registrar(Actor.of(actor), AccionAuditoria.MEDIO_ACTUALIZADO, TipoRecurso.MEDIO, id,
					"Metadatos actualizados: " + activo.getNombreOriginal(), null, cambios);
		}
		return aDto(actor, activo);
	}

	@Transactional
	public MediaDtos.Medio aprobar(UsuarioSistema actor, UUID id) {
		ActivoMultimedia activo = cargarVisible(actor, id);
		permitir(actor.getRol().tiene(Permiso.MEDIA_PUBLISH));
		if (activo.getEstado() != EstadoMedio.PENDIENTE && activo.getEstado() != EstadoMedio.RECHAZADO) {
			throw new ApiException(HttpStatus.CONFLICT, "TRANSICION_INVALIDA", "Sólo se aprueba un archivo pendiente.");
		}
		List<ApiError.FieldError> faltantes = new ArrayList<>();
		if (activo.esImagen() && vacio(activo.getTextoAlternativo())) {
			faltantes.add(new ApiError.FieldError("textoAlternativo", "Falta el texto alternativo."));
		}
		if (vacio(activo.getCredito())) {
			faltantes.add(new ApiError.FieldError("credito", "Indica el crédito o la fuente."));
		}
		if (vacio(activo.getLicencia())) {
			faltantes.add(new ApiError.FieldError("licencia", "Indica la licencia o autorización de uso."));
		}
		if (!activo.isDerechosConfirmados()) {
			faltantes.add(new ApiError.FieldError("derechosConfirmados", "Confirma los derechos de uso."));
		}
		if (!faltantes.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "MEDIO_INCOMPLETO",
					"Completa los metadatos antes de aprobar el archivo.", faltantes);
		}
		EstadoMedio anterior = activo.getEstado();
		activo.aprobar(actor.getUsuario(), Instant.now());
		this.repository.saveAndFlush(activo);
		this.auditService.registrar(Actor.of(actor), AccionAuditoria.MEDIO_APROBADO, TipoRecurso.MEDIO, id,
				"Archivo aprobado: " + activo.getNombreOriginal(), null,
				Cambios.nuevo().campo("estado", anterior, EstadoMedio.APROBADO));
		return aDto(actor, activo);
	}

	@Transactional
	public MediaDtos.Medio rechazar(UsuarioSistema actor, UUID id, String motivo) {
		ActivoMultimedia activo = cargarVisible(actor, id);
		permitir(actor.getRol().tiene(Permiso.MEDIA_REVIEW));
		if (activo.getEstado() != EstadoMedio.PENDIENTE && activo.getEstado() != EstadoMedio.APROBADO) {
			throw new ApiException(HttpStatus.CONFLICT, "TRANSICION_INVALIDA", "El archivo no puede rechazarse.");
		}
		EstadoMedio anterior = activo.getEstado();
		activo.rechazar(actor.getUsuario(), motivo.strip(), Instant.now());
		this.repository.saveAndFlush(activo);
		this.auditService.registrar(Actor.of(actor), AccionAuditoria.MEDIO_RECHAZADO, TipoRecurso.MEDIO, id,
				"Archivo rechazado: " + activo.getNombreOriginal(), motivo.strip(),
				Cambios.nuevo().campo("estado", anterior, EstadoMedio.RECHAZADO));
		return aDto(actor, activo);
	}

	/** Archivar no borra el archivo: deja de poder usarse en contenido nuevo o publicaciones futuras. */
	@Transactional
	public MediaDtos.Medio archivar(UsuarioSistema actor, UUID id, String motivo) {
		ActivoMultimedia activo = cargarVisible(actor, id);
		permitir(actor.getRol().tiene(Permiso.MEDIA_ARCHIVE));
		if (activo.getEstado() == EstadoMedio.ARCHIVADO) {
			throw new ApiException(HttpStatus.CONFLICT, "TRANSICION_INVALIDA", "El archivo ya está archivado.");
		}
		EstadoMedio anterior = activo.getEstado();
		activo.archivar();
		this.repository.saveAndFlush(activo);
		this.auditService.registrar(Actor.of(actor), AccionAuditoria.MEDIO_ARCHIVADO, TipoRecurso.MEDIO, id,
				"Archivo archivado: " + activo.getNombreOriginal(), motivo.strip(),
				Cambios.nuevo().campo("estado", anterior, EstadoMedio.ARCHIVADO));
		return aDto(actor, activo);
	}

	// ---------------------------------------------------------------------------------------------

	/** Quien sólo puede cargar ve sus propios archivos; gestores y editores ven todos. */
	private ActivoMultimedia cargarVisible(UsuarioSistema actor, UUID id) {
		return this.repository.findConResponsables(id)
			.filter((a) -> actor.getRol().tiene(Permiso.MEDIA_UPDATE) || a.esDe(actor.getUsuarioId()))
			.orElseThrow(() -> ApiException.notFound("MEDIO_NO_ENCONTRADO", "El archivo no existe."));
	}

	private static boolean puedeEditar(UsuarioSistema actor, ActivoMultimedia activo) {
		if (activo.getEstado() == EstadoMedio.ARCHIVADO) {
			return false;
		}
		return actor.getRol().tiene(Permiso.MEDIA_UPDATE)
				|| (activo.esDe(actor.getUsuarioId()) && activo.getEstado() != EstadoMedio.APROBADO);
	}

	private MediaDtos.Medio aDto(UsuarioSistema actor, ActivoMultimedia a) {
		boolean aprobable = a.getEstado() == EstadoMedio.PENDIENTE || a.getEstado() == EstadoMedio.RECHAZADO;
		MediaDtos.Acciones acciones = new MediaDtos.Acciones(puedeEditar(actor, a),
				aprobable && actor.getRol().tiene(Permiso.MEDIA_PUBLISH),
				(a.getEstado() == EstadoMedio.PENDIENTE || a.getEstado() == EstadoMedio.APROBADO)
						&& actor.getRol().tiene(Permiso.MEDIA_REVIEW),
				a.getEstado() != EstadoMedio.ARCHIVADO && actor.getRol().tiene(Permiso.MEDIA_ARCHIVE));
		return new MediaDtos.Medio(a.getId(), a.getNombreOriginal(), a.getTipoMime(), a.getTamanoBytes(), a.getAncho(),
				a.getAlto(), a.getTextoAlternativo(), a.getPie(), a.getCredito(), a.getLicencia(),
				a.isDerechosConfirmados(), a.getEstado(), a.getMotivoRechazo(), nombre(a.getSubidoPor()),
				nombre(a.getRevisadoPor()), a.getRevisadoAt(), a.getCreatedAt(), a.getVersion(),
				a.getRutaMovil() != null, acciones);
	}

	/** Prohibido cargar datos de pacientes identificables o material sin derechos (sección 4 del documento). */
	private static void validarDeclaraciones(MediaDtos.MetadatosRequest metadatos) {
		List<ApiError.FieldError> errores = new ArrayList<>();
		if (!metadatos.derechosConfirmados()) {
			errores.add(new ApiError.FieldError("derechosConfirmados",
					"Confirma que tienes derechos para usar y publicar este archivo."));
		}
		if (!metadatos.sinDatosPaciente()) {
			errores.add(new ApiError.FieldError("sinDatosPaciente",
					"Confirma que el archivo no contiene datos identificables de pacientes."));
		}
		if (!errores.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "DECLARACION_REQUERIDA",
					"Faltan declaraciones obligatorias sobre el archivo.", errores);
		}
	}

	private static ActivoMultimedia.Metadatos aMetadatos(MediaDtos.MetadatosRequest m) {
		return new ActivoMultimedia.Metadatos(limpio(m.textoAlternativo()), limpio(m.pie()), limpio(m.credito()),
				limpio(m.licencia()), m.derechosConfirmados());
	}

	/** El nombre original sólo se conserva como referencia legible, sin rutas ni caracteres de control. */
	static String nombreSeguro(String original, String extension) {
		String base = (original == null) ? "" : original.replace('\\', '/');
		base = base.substring(base.lastIndexOf('/') + 1).replaceAll("[^\\p{L}\\p{N} ._()-]", "_").strip();
		if (base.isEmpty()) {
			base = "archivo." + extension;
		}
		return (base.length() > 200) ? base.substring(0, 200) : base;
	}

	private static String limpio(String texto) {
		return (texto == null || texto.isBlank()) ? null : texto.strip();
	}

	private static boolean vacio(String texto) {
		return texto == null || texto.isBlank();
	}

	private static String nombre(com.empresa.medicina.users.Usuario usuario) {
		return (usuario != null) ? usuario.getNombre() + " " + usuario.getApellido() : null;
	}

	private static void permitir(boolean permitido) {
		if (!permitido) {
			throw ApiException.forbidden("ACCESO_DENEGADO", "No tienes permiso para realizar esta acción.");
		}
	}

	private static ApiException campo(String campo, String mensaje) {
		return new ApiException(HttpStatus.BAD_REQUEST, "VALIDACION", mensaje,
				List.of(new ApiError.FieldError(campo, mensaje)));
	}

	/** Datos de un medio para validar y renderizar un bloque. */
	public record Referencia(UUID id, String tipoMime, EstadoMedio estado, String textoAlternativo, String pie,
			String credito, String licencia, Integer ancho, Integer alto, boolean tieneVariantes) {

		public boolean esImagen() {
			return !"application/pdf".equals(this.tipoMime);
		}

	}

	public record Archivo(byte[] contenido, String tipoMime, String nombre) {
	}

}
