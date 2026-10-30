package com.empresa.medicina.content;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.empresa.medicina.IntegrationTest;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guías de práctica clínica: revisión clínica obligatoria por otra persona, versión pública estable durante la
 * actualización, política de acceso independiente y vencimiento.
 */
class GuiaIntegrationTest extends IntegrationTest {

	private static final String REVISOR = "00000000-0000-4000-8000-000000000015";

	@Autowired
	private EditorialService editorialService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void elAccesoDependeDelTipoDeContenido() throws Exception {
		MockHttpSession autor = iniciarSesion("autor@demo.local", DEMO_PASSWORD);
		MockHttpSession plataforma = iniciarSesion("plataforma@demo.local", DEMO_PASSWORD);

		// Artículo: gratis con cuenta, sin tier, y su acceso no se puede cambiar.
		String articulo = leer(crear(autor, Map.of("titulo", "Artículo de acceso " + UUID.randomUUID()))
			.andExpect(jsonPath("$.politicaAcceso").value("REGISTRADO_GRATUITO"))
			.andExpect(jsonPath("$.tierMinimo").doesNotExist())
			.andReturn(), "$.id");
		cambiarAcceso(plataforma, articulo, Map.of("politica", "POR_TIER", "tierMinimoId", 3, "motivo", "Prueba"))
			.andExpect(status().isForbidden());

		// Recurso de estudio: siempre Premium, y tampoco se puede cambiar.
		String recurso = leer(crear(autor, Map.of("tipo", "RECURSO_ESTUDIO", "titulo", "Recurso " + UUID.randomUUID()))
			.andExpect(jsonPath("$.politicaAcceso").value("POR_TIER"))
			.andExpect(jsonPath("$.tierMinimo.codigo").value("PREMIUM"))
			.andReturn(), "$.id");
		cambiarAcceso(plataforma, recurso, Map.of("politica", "POR_TIER", "tierMinimoId", 1, "motivo", "Prueba"))
			.andExpect(status().isForbidden());

		// Guía: siempre de pago; elegir otro tier funciona y quitar el tier se rechaza.
		String guia = leer(crear(autor, Map.of("tipo", "GUIA_PRACTICA_CLINICA", "titulo", "Guía " + UUID.randomUUID()))
			.andReturn(), "$.id");
		cambiarAcceso(plataforma, guia, Map.of("politica", "POR_TIER", "tierMinimoId", 2, "motivo", "Intermedio"))
			.andExpect(jsonPath("$.tierMinimo.codigo").value("INTERMEDIO"));
		cambiarAcceso(plataforma, guia, Map.of("politica", "REGISTRADO_GRATUITO", "motivo", "Gratis"))
			.andExpect(status().isBadRequest());

		// Cambiar el tipo reasigna el acceso del tipo nuevo.
		this.mockMvc.perform(put("/api/v1/admin/contenido/{id}/tipo", guia).with(csrf())
			.session(plataforma)
			.contentType(MediaType.APPLICATION_JSON)
			.content(json("tipo", "RECURSO_ESTUDIO", "motivo", "Es material de estudio")))
			.andExpect(jsonPath("$.tierMinimo.codigo").value("PREMIUM"));
		this.mockMvc.perform(put("/api/v1/admin/contenido/{id}/tipo", guia).with(csrf())
			.session(plataforma)
			.contentType(MediaType.APPLICATION_JSON)
			.content(json("tipo", "ARTICULO", "motivo", "Es un artículo")))
			.andExpect(jsonPath("$.politicaAcceso").value("REGISTRADO_GRATUITO"))
			.andExpect(jsonPath("$.tierMinimo").doesNotExist());
	}

	@Test
	void unaGuiaSePublicaSoloConAprobacionClinicaYConservaSuVersionPublica() throws Exception {
		MockHttpSession autor = iniciarSesion("autor@demo.local", DEMO_PASSWORD);
		MockHttpSession editor = iniciarSesion("editor@demo.local", DEMO_PASSWORD);
		MockHttpSession revisor = iniciarSesion("revisor@demo.local", DEMO_PASSWORD);
		MockHttpSession plataforma = iniciarSesion("plataforma@demo.local", DEMO_PASSWORD);

		// Creación: los campos clínicos son obligatorios para enviar a revisión.
		String id = leer(crear(autor, Map.of("tipo", "GUIA_PRACTICA_CLINICA", "titulo", "GPC de prueba " + UUID.randomUUID()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.revisionClinicaObligatoria").value(true))
			.andExpect(jsonPath("$.politicaAcceso").value("POR_TIER"))
			.andExpect(jsonPath("$.tierMinimo.codigo").value("BASICO"))
			.andReturn(), "$.id");
		enviar(autor, id).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors[*].field", hasItem("institucionEmisora")))
			.andExpect(jsonPath("$.fieldErrors[*].field", hasItem("recomendaciones")));
		actualizar(autor, id, "Guía versión 1").andExpect(status().isOk());

		// Revisión: el editor valida la presentación pero no puede sustituir la revisión clínica.
		enviar(autor, id).andExpect(jsonPath("$.estado").value("EN_REVISION_EDITORIAL"));
		revisar(editor, id, "EDITORIAL", "APROBAR").andExpect(jsonPath("$.estado").value("EN_REVISION_CLINICA"));
		revisar(editor, id, "CLINICA", "APROBAR").andExpect(status().isForbidden());
		revisar(plataforma, id, "CLINICA", "APROBAR").andExpect(status().isForbidden());
		revisar(revisor, id, "CLINICA", "APROBAR").andExpect(jsonPath("$.estado").value("APROBADO"));

		// Publicación: el autor no publica; el editor sí.
		publicar(autor, id).andExpect(status().isForbidden());
		String slug = leer(publicar(editor, id).andExpect(jsonPath("$.versionPublicada").value(1)).andReturn(),
				"$.slug");

		// Acceso: una guía nace con el tier más bajo (Básico) y el editor puede subirlo a Premium.
		this.mockMvc.perform(get("/api/v1/content/" + slug)).andExpect(status().isUnauthorized());
		this.mockMvc.perform(get("/api/v1/content/" + slug).session(registrarUsuario(1))).andExpect(status().isOk());
		this.mockMvc
			.perform(put("/api/v1/admin/contenido/{id}/acceso", id).with(csrf())
				.session(editor)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("politica", "POR_TIER", "tierMinimoId", 3, "motivo", "Material Premium")))
			.andExpect(status().isForbidden());
		this.mockMvc
			.perform(put("/api/v1/admin/contenido/{id}/acceso", id).with(csrf())
				.session(plataforma)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("politica", "POR_TIER", "tierMinimoId", 3, "motivo", "Material Premium")))
			.andExpect(jsonPath("$.politicaAcceso").value("POR_TIER"));
		MockHttpSession premium = registrarUsuario(3);
		this.mockMvc.perform(get("/api/v1/content/" + slug).session(registrarUsuario(1)))
			.andExpect(status().isForbidden());
		this.mockMvc.perform(get("/api/v1/content/" + slug).session(premium))
			.andExpect(jsonPath("$.contenido.titulo").value("Guía versión 1"))
			.andExpect(jsonPath("$.contenido.guia.poblacionObjetivo").value("Adultos de prueba."))
			.andExpect(jsonPath("$.contenido.responsables.revisorClinico").value("Ramón Revisor"));

		// Actualización: la copia de trabajo cambia, pero el público sigue viendo la versión 1.
		actualizar(editor, id, "Guía versión 2").andExpect(jsonPath("$.estado").value("EN_EDICION"))
			.andExpect(jsonPath("$.cambiosSinPublicar").value(true));
		this.mockMvc.perform(get("/api/v1/content/" + slug).session(premium))
			.andExpect(jsonPath("$.contenido.titulo").value("Guía versión 1"))
			.andExpect(jsonPath("$.contenido.version").value(1));

		enviar(editor, id);
		revisar(editor, id, "EDITORIAL", "APROBAR");
		revisar(revisor, id, "CLINICA", "APROBAR");
		publicar(editor, id).andExpect(jsonPath("$.versionPublicada").value(2))
			.andExpect(jsonPath("$.versiones.length()").value(2));
		this.mockMvc.perform(get("/api/v1/content/" + slug).session(premium))
			.andExpect(jsonPath("$.contenido.titulo").value("Guía versión 2"))
			.andExpect(jsonPath("$.contenido.version").value(2));

		// Las versiones publicadas son inmutables.
		assertThatThrownBy(() -> this.jdbcTemplate.update("update contenido_version set titulo = 'x' where contenido_id = ?",
				UUID.fromString(id))).hasMessageContaining("versión publicada");
	}

	@Test
	void unaGuiaVencidaSeMarcaYSeMuestraConAviso() throws Exception {
		String slug = "gpc-sepsis-choque-septico-adultos";
		UUID id = UUID.fromString("00000000-0000-4000-8000-0000000000a1");
		try {
			this.jdbcTemplate.update("update contenido set proxima_revision = ? where id = ?",
					java.sql.Date.valueOf(LocalDate.now().minusDays(1)), id);
			this.editorialService.marcarVencidas();
			assertThat(this.jdbcTemplate.queryForObject("select estado from contenido where id = ?", String.class, id))
				.isEqualTo("REVISION_REQUERIDA");

			// Política ADVERTIR: la versión publicada sigue visible para quien tiene acceso.
			this.mockMvc.perform(get("/api/v1/content/" + slug).session(registrarUsuario(3)))
				.andExpect(status().isOk());
		}
		finally {
			this.jdbcTemplate.update("update contenido set proxima_revision = ?, estado = 'PUBLICADO' where id = ?",
					java.sql.Date.valueOf(LocalDate.now().plusMonths(6)), id);
		}
	}

	@Test
	void unRecursoSinAutorizacionDePublicacionNoSePublica() throws Exception {
		MockHttpSession editor = iniciarSesion("editor@demo.local", DEMO_PASSWORD);
		MockHttpSession revisor = iniciarSesion("revisor@demo.local", DEMO_PASSWORD);
		String id = "00000000-0000-4000-8000-0000000000d3";

		this.mockMvc.perform(get("/api/v1/admin/contenido/{id}", id).session(editor))
			.andExpect(jsonPath("$.tipo").value("RECURSO_ESTUDIO"))
			.andExpect(jsonPath("$.autorizacionPublicacion").value(false))
			.andExpect(jsonPath("$.bloques.length()").value(13));

		// Se puede revisar clínicamente aunque no tenga autorización todavía…
		String cuerpo = json("titulo", "Cirugía general ENARM: apendicitis aguda", "areaMedica", "Cirugía general",
				"resumen", "Resumen.", "fuenteTitulo", "Cirugía General ENARM (documento PDF de estudio).",
				"fechaRevision", LocalDate.now().toString(), "tiempoLecturaMinutos", 12, "revisionClinicaRequerida", true,
				"procedencia", "PDF recibido.", "titularDerechos", "Pendiente de confirmar", "licenciaUso",
				"Pendiente de confirmar", "autorizacionPublicacion", false, "version",
				Long.parseLong(leer(this.mockMvc.perform(get("/api/v1/admin/contenido/{id}", id).session(editor))
					.andReturn(), "$.version")));
		this.mockMvc.perform(put("/api/v1/admin/contenido/{id}", id).with(csrf())
			.session(editor)
			.contentType(MediaType.APPLICATION_JSON)
			.content(cuerpo)).andExpect(status().isOk());
		enviar(editor, id).andExpect(jsonPath("$.estado").value("EN_REVISION_EDITORIAL"));
		revisar(editor, id, "EDITORIAL", "APROBAR").andExpect(jsonPath("$.estado").value("EN_REVISION_CLINICA"));
		revisar(revisor, id, "CLINICA", "APROBAR").andExpect(jsonPath("$.estado").value("APROBADO"));

		// …pero sin autorización escrita de uso no se publica.
		publicar(editor, id).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("NO_PUBLICABLE"))
			.andExpect(jsonPath("$.fieldErrors[*].field", hasItem("autorizacionPublicacion")));
	}

	// ---------------------------------------------------------------------------------------------

	private ResultActions crear(MockHttpSession session, Map<String, Object> cuerpo) throws Exception {
		return this.mockMvc.perform(post("/api/v1/admin/contenido").with(csrf())
			.session(session)
			.contentType(MediaType.APPLICATION_JSON)
			.content(this.jsonMapper.writeValueAsString(cuerpo)));
	}

	private ResultActions actualizar(MockHttpSession session, String id, String titulo) throws Exception {
		long version = Long.parseLong(
				leer(this.mockMvc.perform(get("/api/v1/admin/contenido/{id}", id).session(session)).andReturn(),
						"$.version"));
		return this.mockMvc.perform(put("/api/v1/admin/contenido/{id}", id).with(csrf())
			.session(session)
			.contentType(MediaType.APPLICATION_JSON)
			.content(json("titulo", titulo, "areaMedica", "Medicina crítica", "resumen", "Resumen ejecutivo.",
					"fuenteTitulo", "Guía oficial de prueba", "fuenteUrl", "https://example.org/guia", "fechaRevision",
					LocalDate.now().toString(), "fechaPublicacionFuente", "2024-01-15", "tiempoLecturaMinutos", 8,
					"institucionEmisora", "Institución de prueba", "fuenteEdicion", "2024", "poblacionObjetivo",
					"Adultos de prueba.", "contextoAtencion", "Urgencias.", "alcanceClinico", "Reanimación inicial.",
					"vigenteHasta", LocalDate.now().plusYears(1).toString(), "proximaRevision",
					LocalDate.now().plusMonths(6).toString(), "recomendaciones",
					List.of(Map.of("texto", "Recomendación de prueba.", "fuerza", "Fuerte", "nivelEvidencia", "Moderada")),
					"version", version)));
	}

	private ResultActions enviar(MockHttpSession session, String id) throws Exception {
		return this.mockMvc.perform(post("/api/v1/admin/contenido/{id}/enviar-revision", id).with(csrf())
			.session(session)
			.contentType(MediaType.APPLICATION_JSON)
			.content(json("revisorId", REVISOR)));
	}

	private ResultActions revisar(MockHttpSession session, String id, String etapa, String decision) throws Exception {
		return this.mockMvc.perform(post("/api/v1/admin/contenido/{id}/revisiones", id).with(csrf())
			.session(session)
			.contentType(MediaType.APPLICATION_JSON)
			.content(json("etapa", etapa, "decision", decision, "comentario", "Revisado.")));
	}

	private ResultActions publicar(MockHttpSession session, String id) throws Exception {
		return this.mockMvc.perform(post("/api/v1/admin/contenido/{id}/publicar", id).with(csrf()).session(session));
	}

	private ResultActions cambiarAcceso(MockHttpSession sesion, String id, Map<String, Object> cuerpo) throws Exception {
		return this.mockMvc.perform(put("/api/v1/admin/contenido/{id}/acceso", id).with(csrf())
			.session(sesion)
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(cuerpo.entrySet().stream().flatMap((e) -> java.util.stream.Stream.of(e.getKey(), e.getValue())).toArray())));
	}

}
