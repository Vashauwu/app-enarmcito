package com.empresa.medicina.content;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import com.empresa.medicina.auth.AuthenticatedUser;
import com.empresa.medicina.common.ApiException;
import com.empresa.medicina.common.PageResponse;
import com.empresa.medicina.common.Paginacion;
import com.empresa.medicina.config.AppProperties;
import com.empresa.medicina.progress.EstadoProgreso;
import com.empresa.medicina.progress.ProgresoContenidoRepository;
import com.empresa.medicina.progress.ProgressResponse;
import com.empresa.medicina.users.UserService;
import com.empresa.medicina.users.UsuarioSistema;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Catálogo público. Lee siempre la versión publicada vigente y aplica la política de acceso en cada consulta:
 * los artículos son gratis con cuenta; guías y recursos exigen tier.
 */
@Service
@Transactional(readOnly = true)
public class CatalogService {

	static final int MAX_LONGITUD_BUSQUEDA = 100;

	static final int MAX_TAMANO_PAGINA = 50;

	private final ContenidoVersionRepository versionRepository;

	private final ProgresoContenidoRepository progresoRepository;

	private final PublicacionService publicacionService;

	private final UserService userService;

	private final AppProperties properties;

	CatalogService(ContenidoVersionRepository versionRepository, ProgresoContenidoRepository progresoRepository,
			PublicacionService publicacionService, UserService userService, AppProperties properties) {
		this.versionRepository = versionRepository;
		this.progresoRepository = progresoRepository;
		this.publicacionService = publicacionService;
		this.userService = userService;
		this.properties = properties;
	}

	/** Visitante anónimo o cuenta activa con su tier vigente. */
	public Lector lector(AuthenticatedUser user) {
		if (user == null) {
			return Lector.ANONIMO;
		}
		UsuarioSistema cuenta = this.userService.obtenerCuentaActiva(user.getId());
		return new Lector(cuenta.getUsuarioId(), cuenta.getTier().getOrdenAcceso(), true);
	}

	public PageResponse<CatalogDtos.Ficha> buscar(Lector lector, String termino, TipoContenido tipo, int pagina,
			int tamano) {
		String texto = (termino != null) ? termino.strip() : "";
		if (texto.length() > MAX_LONGITUD_BUSQUEDA) {
			throw ApiException.badRequest("BUSQUEDA_INVALIDA",
					"La búsqueda admite como máximo " + MAX_LONGITUD_BUSQUEDA + " caracteres.");
		}
		return PageResponse.of(this.versionRepository.buscar(lector.autenticado(), lector.ordenAcceso(),
				retirarVencidas(), (tipo != null) ? tipo.name() : "", patronBusqueda(texto),
				Paginacion.de(pagina, tamano, Sort.unsorted())), this::aFicha);
	}

	/** Abre la versión publicada y, si hay sesión, la registra como la última consultada. */
	@Transactional
	public CatalogDtos.Lectura abrir(Lector lector, String slug) {
		ContenidoVersionRepository.Publicada publicada = this.versionRepository.findPublicadaPorSlug(slug)
			.orElseThrow(CatalogService::noEncontrado);
		autorizar(lector, publicada);
		ProgressResponse progreso = null;
		if (lector.autenticado()) {
			this.progresoRepository.registrarConsulta(lector.usuarioId(), publicada.getContenidoId());
			progreso = this.progresoRepository
				.findById(new com.empresa.medicina.progress.ProgresoContenido.Id(lector.usuarioId(),
						publicada.getContenidoId()))
				.map(ProgressResponse::from)
				.orElse(null);
		}
		return new CatalogDtos.Lectura(this.publicacionService.leer(publicada.getDatos()), acceso(publicada),
				vencida(publicada), progreso);
	}

	/** Comprueba el acceso a un contenido publicado por id (por ejemplo, al marcarlo como completado). */
	public CatalogDtos.Ficha autorizarPorId(Lector lector, UUID contenidoId) {
		ContenidoVersionRepository.Publicada publicada = this.versionRepository.findPublicadaPorId(contenidoId)
			.orElseThrow(CatalogService::noEncontrado);
		autorizar(lector, publicada);
		return aFicha(publicada);
	}

	/** Un medio se entrega si alguna publicación vigente que lo usa es legible para el visitante. */
	public boolean puedeVerMedio(Lector lector, UUID activoId) {
		return this.versionRepository.findPublicadasConMedio(activoId)
			.stream()
			.anyMatch((ficha) -> permite(lector, ficha));
	}

	public Optional<UltimaLectura> ultimaLectura(Lector lector) {
		if (!lector.autenticado()) {
			return Optional.empty();
		}
		return this.versionRepository
			.findUltimaLectura(lector.usuarioId(), true, lector.ordenAcceso(), retirarVencidas())
			.map((f) -> new UltimaLectura(aFicha(f),
					new ProgressResponse(f.getContenidoId(), EstadoProgreso.valueOf(f.getProgresoEstado()),
							instante(f.getUltimaConsultaAt()), instante(f.getCompletadoAt()))));
	}

	public record UltimaLectura(CatalogDtos.Ficha ficha, ProgressResponse progreso) {
	}

	// ---------------------------------------------------------------------------------------------

	private void autorizar(Lector lector, ContenidoVersionRepository.FichaCatalogo ficha) {
		if (retirarVencidas() && esGuia(ficha) && vencida(ficha)) {
			throw noEncontrado();
		}
		PoliticaAcceso politica = PoliticaAcceso.valueOf(ficha.getPoliticaAcceso());
		if (!lector.autenticado()) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "REQUIERE_REGISTRO",
					(politica == PoliticaAcceso.REGISTRADO_GRATUITO)
							? "Este material es gratuito; inicia sesión o crea una cuenta para leerlo."
							: "Inicia sesión para consultar este material.");
		}
		if (politica == PoliticaAcceso.POR_TIER && lector.ordenAcceso() < ficha.getTierOrden()) {
			throw ApiException.forbidden("TIER_INSUFICIENTE",
					"Este material requiere el tier " + ficha.getTierNombre() + ".");
		}
	}

	private boolean permite(Lector lector, ContenidoVersionRepository.FichaCatalogo ficha) {
		try {
			autorizar(lector, ficha);
			return true;
		}
		catch (ApiException ex) {
			return false;
		}
	}

	private CatalogDtos.Ficha aFicha(ContenidoVersionRepository.FichaCatalogo f) {
		return new CatalogDtos.Ficha(f.getContenidoId(), f.getSlug(), TipoContenido.valueOf(f.getTipo()), f.getTitulo(),
				f.getAreaMedica(), f.getExtracto(), f.getTiempoLecturaMinutos(), acceso(f), fecha(f.getProximaRevision()),
				vencida(f));
	}

	private static CatalogDtos.Acceso acceso(ContenidoVersionRepository.FichaCatalogo f) {
		return new CatalogDtos.Acceso(PoliticaAcceso.valueOf(f.getPoliticaAcceso()), f.getTierCodigo(),
				f.getTierNombre());
	}

	private static boolean vencida(ContenidoVersionRepository.FichaCatalogo f) {
		LocalDate proxima = fecha(f.getProximaRevision());
		return proxima != null && proxima.isBefore(LocalDate.now());
	}

	private static boolean esGuia(ContenidoVersionRepository.FichaCatalogo f) {
		return TipoContenido.GUIA_PRACTICA_CLINICA.name().equals(f.getTipo());
	}

	private boolean retirarVencidas() {
		return this.properties.content().expiredGuidePolicy() == AppProperties.Content.ExpiredGuidePolicy.RETIRAR;
	}

	private static LocalDate fecha(String texto) {
		return (texto != null) ? LocalDate.parse(texto) : null;
	}

	private static Instant instante(String texto) {
		return (texto != null) ? Instant.parse(texto) : null;
	}

	private static ApiException noEncontrado() {
		return ApiException.notFound("CONTENIDO_NO_ENCONTRADO", "El contenido no existe o no está disponible.");
	}

	/** Patrón LIKE en minúsculas, sin acentos y con los comodines del usuario escapados. */
	static String patronBusqueda(String texto) {
		String normalizado = Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
			.replaceAll("\\p{M}", "");
		String escapado = normalizado.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
		return "%" + escapado + "%";
	}

}
