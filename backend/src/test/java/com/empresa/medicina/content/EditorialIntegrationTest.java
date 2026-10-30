package com.empresa.medicina.content;

import java.util.List;
import java.util.UUID;

import com.empresa.medicina.IntegrationTest;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo editorial de un artículo: edición → revisión editorial → revisión clínica → aprobado → publicado → archivado.
 */
class EditorialIntegrationTest extends IntegrationTest {

	static final String REVISOR = "00000000-0000-4000-8000-000000000015";

	static final String EDITOR = "00000000-0000-4000-8000-000000000014";

	static final String AUTOR = "00000000-0000-4000-8000-000000000017";

	@Test
	void flujoCompletoDeUnArticulo() throws Exception {
		MockHttpSession autor = iniciarSesion("autor@demo.local", DEMO_PASSWORD);
		MockHttpSession revisor = iniciarSesion("revisor@demo.local", DEMO_PASSWORD);
		MockHttpSession otroRevisor = iniciarSesion("revisor2@demo.local", DEMO_PASSWORD);
		MockHttpSession editor = iniciarSesion("editor@demo.local", DEMO_PASSWORD);

		// 1. Borrador incompleto: se guarda, pero no se envía a revisión.
		MvcResult creado = crear(autor, json("titulo", "Hiponatremia: abordaje inicial"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.tipo").value("ARTICULO"))
			.andExpect(jsonPath("$.politicaAcceso").value("REGISTRADO_GRATUITO"))
			.andExpect(jsonPath("$.estado").value("BORRADOR"))
			.andExpect(jsonPath("$.slug").value("hiponatremia-abordaje-inicial"))
			.andReturn();
		String id = leer(creado, "$.id");
		String slug = leer(creado, "$.slug");
		enviar(autor, id, REVISOR).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("CONTENIDO_INCOMPLETO"))
			.andExpect(jsonPath("$.fieldErrors[*].field", hasItem("puntosClave")));

		// 2. Completar; una versión desactualizada se rechaza. Editar pasa el borrador a EN_EDICION.
		actualizar(autor, id, 99L, "Hiponatremia: abordaje inicial").andExpect(status().isConflict());
		actualizar(autor, id, version(autor, id), "Hiponatremia: abordaje inicial").andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("EN_EDICION"));

		// 3. Separación autor/revisor y envío a revisión editorial.
		enviar(autor, id, AUTOR).andExpect(status().isBadRequest());
		enviar(autor, id, REVISOR).andExpect(jsonPath("$.estado").value("EN_REVISION_EDITORIAL"));
		actualizar(autor, id, version(autor, id), "Otro título").andExpect(status().isForbidden());

		// 4. El revisor no hace la revisión editorial; el editor sí, y pasa a revisión clínica.
		revisar(revisor, id, "EDITORIAL", "APROBAR", null).andExpect(status().isForbidden());
		revisar(editor, id, "EDITORIAL", "APROBAR", null).andExpect(jsonPath("$.estado").value("EN_REVISION_CLINICA"));

		// 5. Un revisor no asignado no ve el contenido; el asignado solicita cambios con comentario.
		this.mockMvc.perform(get("/api/v1/admin/contenido/{id}", id).session(otroRevisor))
			.andExpect(status().isNotFound());
		revisar(revisor, id, "CLINICA", "SOLICITAR_CAMBIOS", null).andExpect(status().isBadRequest());
		revisar(revisor, id, "CLINICA", "SOLICITAR_CAMBIOS", "Precisa el umbral de sodio sérico.")
			.andExpect(jsonPath("$.estado").value("CAMBIOS_SOLICITADOS"))
			.andExpect(jsonPath("$.revisiones[0].etapa").value("CLINICA"));

		// 6. Corrige, reenvía, pasa ambas revisiones y el editor publica la versión 1.
		actualizar(autor, id, version(autor, id), "Hiponatremia: abordaje inicial (corregido)")
			.andExpect(jsonPath("$.estado").value("EN_EDICION"));
		enviar(autor, id, null).andExpect(jsonPath("$.estado").value("EN_REVISION_EDITORIAL"));
		revisar(editor, id, "EDITORIAL", "APROBAR", null);
		revisar(revisor, id, "CLINICA", "APROBAR", "Correcto.").andExpect(jsonPath("$.estado").value("APROBADO"));
		this.mockMvc.perform(post("/api/v1/admin/contenido/{id}/publicar", id).with(csrf()).session(revisor))
			.andExpect(status().isForbidden());
		this.mockMvc.perform(post("/api/v1/admin/contenido/{id}/publicar", id).with(csrf()).session(editor))
			.andExpect(jsonPath("$.estado").value("PUBLICADO"))
			.andExpect(jsonPath("$.versionPublicada").value(1));

		// 7. Artículo gratis con cuenta: sin sesión no se lee; cualquier cuenta (aun Básico) sí.
		this.mockMvc.perform(get("/api/v1/content/" + slug)).andExpect(status().isUnauthorized());
		MockHttpSession alumno = registrarUsuario(1);
		this.mockMvc.perform(get("/api/v1/content/" + slug).session(alumno)).andExpect(status().isOk());

		// 8. Archivar exige motivo y lo retira; restaurar lo devuelve a edición.
		archivar(editor, id, "").andExpect(status().isBadRequest());
		archivar(editor, id, "Se reescribirá.").andExpect(jsonPath("$.estado").value("ARCHIVADO"));
		this.mockMvc.perform(get("/api/v1/content/" + slug).session(alumno)).andExpect(status().isNotFound());
		this.mockMvc.perform(get("/api/v1/content").param("q", "hiponatremia").session(alumno))
			.andExpect(jsonPath("$.items[*].slug", not(hasItem(slug))));
		this.mockMvc
			.perform(post("/api/v1/admin/contenido/{id}/restaurar", id).with(csrf())
				.session(autor)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("motivo", "Retomar")))
			.andExpect(status().isForbidden());
		this.mockMvc
			.perform(post("/api/v1/admin/contenido/{id}/restaurar", id).with(csrf())
				.session(editor)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("motivo", "Retomar la edición")))
			.andExpect(jsonPath("$.estado").value("EN_EDICION"));

		// 9. Todo quedó auditado y es visible para el autor porque el contenido es suyo.
		this.mockMvc.perform(get("/api/v1/admin/auditoria").param("recursoId", id).param("size", "50").session(autor))
			.andExpect(jsonPath("$.items[*].accion", hasItem("CONTENIDO_PUBLICADO")))
			.andExpect(jsonPath("$.items[*].accion", hasItem("CONTENIDO_REVISION_CLINICA")))
			.andExpect(jsonPath("$.items[*].accion", hasItem("CONTENIDO_ARCHIVADO")));
	}

	@Test
	void lasTransicionesInvalidasSeRechazan() throws Exception {
		MockHttpSession editor = iniciarSesion("editor@demo.local", DEMO_PASSWORD);
		String id = leer(crear(editor, completo("Transiciones inválidas " + UUID.randomUUID(), null)).andReturn(), "$.id");

		this.mockMvc.perform(post("/api/v1/admin/contenido/{id}/publicar", id).with(csrf()).session(editor))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("TRANSICION_INVALIDA"));
		revisar(editor, id, "CLINICA", "APROBAR", null).andExpect(status().isConflict());
		revisar(editor, id, "EDITORIAL", "APROBAR", null).andExpect(status().isConflict());
	}

	@Test
	void nadieRevisaClinicamenteSuPropioContenido() throws Exception {
		MockHttpSession editor = iniciarSesion("editor@demo.local", DEMO_PASSWORD);
		String id = leer(crear(editor, completo("Separación " + UUID.randomUUID(), null)).andReturn(), "$.id");
		enviar(editor, id, REVISOR);
		revisar(editor, id, "EDITORIAL", "APROBAR", null).andExpect(jsonPath("$.estado").value("EN_REVISION_CLINICA"));

		revisar(editor, id, "CLINICA", "APROBAR", null).andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("SEPARACION_REQUERIDA"));
	}

	@Test
	void sinRevisionClinicaRequeridaLaAprobacionEditorialBasta() throws Exception {
		MockHttpSession editor = iniciarSesion("editor@demo.local", DEMO_PASSWORD);
		MockHttpSession autor = iniciarSesion("autor@demo.local", DEMO_PASSWORD);
		String id = leer(crear(autor, completo("Sin revisión clínica " + UUID.randomUUID(), false)).andReturn(), "$.id");
		enviar(autor, id, null);

		revisar(editor, id, "EDITORIAL", "APROBAR", null).andExpect(jsonPath("$.estado").value("APROBADO"));
	}

	@Test
	void validaFormatoDeFuenteYUnicidadDelSlug() throws Exception {
		MockHttpSession editor = iniciarSesion("editor@demo.local", DEMO_PASSWORD);

		crear(editor, json("titulo", "Formato", "fuenteUrl", "ftp://ejemplo", "fuenteDoi", "doi-invalido"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors[*].field", hasItem("fuenteUrl")))
			.andExpect(jsonPath("$.fieldErrors[*].field", hasItem("fuenteDoi")));
		crear(editor, json("titulo", "Otro", "slug", "sepsis-3-definiciones")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("SLUG_DUPLICADO"));
	}

	@Test
	void unArticuloNoPuedeCambiarDePoliticaDeAcceso() throws Exception {
		MockHttpSession plataforma = iniciarSesion("plataforma@demo.local", DEMO_PASSWORD);
		String id = leer(crear(plataforma, json("titulo", "Gratuito " + UUID.randomUUID())).andReturn(), "$.id");

		this.mockMvc
			.perform(put("/api/v1/admin/contenido/{id}/acceso", id).with(csrf())
				.session(plataforma)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("politica", "POR_TIER", "tierMinimoId", 3, "motivo", "Prueba")))
			.andExpect(status().isForbidden());
	}

	@Test
	void elAutorSoloVeSuContenido() throws Exception {
		MockHttpSession autor = iniciarSesion("autor@demo.local", DEMO_PASSWORD);

		this.mockMvc.perform(get("/api/v1/admin/contenido").param("size", "50").session(autor))
			.andExpect(jsonPath("$.items[*].autor.nombre", everyItem(is("Andrés Autor"))));
	}

	// ---------------------------------------------------------------------------------------------

	ResultActions crear(MockHttpSession session, String cuerpo) throws Exception {
		return this.mockMvc.perform(post("/api/v1/admin/contenido").with(csrf())
			.session(session)
			.contentType(MediaType.APPLICATION_JSON)
			.content(cuerpo));
	}

	String completo(String titulo, Boolean revisionClinica) {
		return json("titulo", titulo, "areaMedica", "Nefrología", "resumen", "Resumen de prueba.", "puntosClave",
				List.of("Primer punto clave.", "  ", "Segundo punto clave."), "fuenteTitulo", "Fuente de prueba",
				"fuenteUrl", "https://doi.org/10.1000/prueba", "fuenteDoi", "10.1000/prueba", "fechaRevision",
				java.time.LocalDate.now().toString(), "tiempoLecturaMinutos", 5, "revisionClinicaRequerida",
				revisionClinica);
	}

	private ResultActions actualizar(MockHttpSession session, String id, long version, String titulo) throws Exception {
		String cuerpo = completo(titulo, null).replaceFirst("\\}$", ",\"version\":" + version + "}");
		return this.mockMvc.perform(put("/api/v1/admin/contenido/{id}", id).with(csrf())
			.session(session)
			.contentType(MediaType.APPLICATION_JSON)
			.content(cuerpo));
	}

	long version(MockHttpSession session, String id) throws Exception {
		return Long.parseLong(
				leer(this.mockMvc.perform(get("/api/v1/admin/contenido/{id}", id).session(session)).andReturn(),
						"$.version"));
	}

	ResultActions enviar(MockHttpSession session, String id, String revisorId) throws Exception {
		return this.mockMvc.perform(post("/api/v1/admin/contenido/{id}/enviar-revision", id).with(csrf())
			.session(session)
			.contentType(MediaType.APPLICATION_JSON)
			.content(json("revisorId", revisorId)));
	}

	ResultActions revisar(MockHttpSession session, String id, String etapa, String decision, String comentario)
			throws Exception {
		return this.mockMvc.perform(post("/api/v1/admin/contenido/{id}/revisiones", id).with(csrf())
			.session(session)
			.contentType(MediaType.APPLICATION_JSON)
			.content(json("etapa", etapa, "decision", decision, "comentario", comentario)));
	}

	private ResultActions archivar(MockHttpSession session, String id, String motivo) throws Exception {
		return this.mockMvc.perform(post("/api/v1/admin/contenido/{id}/archivar", id).with(csrf())
			.session(session)
			.contentType(MediaType.APPLICATION_JSON)
			.content(json("motivo", motivo)));
	}

}
