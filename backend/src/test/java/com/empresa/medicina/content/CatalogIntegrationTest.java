package com.empresa.medicina.content;

import com.empresa.medicina.IntegrationTest;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Catálogo público: artículos gratuitos sin sesión; guías y recursos según su política de acceso.
 */
class CatalogIntegrationTest extends IntegrationTest {

	private static final String ARTICULO = "sepsis-3-definiciones";

	private static final String GUIA_PREMIUM = "gpc-sepsis-choque-septico-adultos";

	private static final String RECURSO_ESTUDIO_PREMIUM = "ficha-repaso-qsofa-choque-septico";

	private static final String BORRADOR = "anafilaxia-reconocimiento-inicial";

	@Test
	void unVisitanteSinSesionNoLeeNadaDelCatalogo() throws Exception {
		this.mockMvc.perform(get("/api/v1/content").param("size", "50"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
		this.mockMvc.perform(get("/api/v1/content/" + ARTICULO))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
	}

	@Test
	void cualquierCuentaLeeLosArticulosSinImportarSuTier() throws Exception {
		MockHttpSession basico = registrarUsuario(1);

		this.mockMvc.perform(get("/api/v1/content").param("size", "50").session(basico))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[*].slug", hasItem(ARTICULO)))
			.andExpect(jsonPath("$.items[*].slug", not(hasItem(RECURSO_ESTUDIO_PREMIUM))))
			.andExpect(jsonPath("$.items[*].slug", not(hasItem(GUIA_PREMIUM))))
			.andExpect(jsonPath("$.items[*].acceso.politica", everyItem(is("REGISTRADO_GRATUITO"))));

		this.mockMvc.perform(get("/api/v1/content/" + ARTICULO).session(basico))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.contenido.tipo").value("ARTICULO"))
			.andExpect(jsonPath("$.contenido.puntosClave.length()").value(5));
	}

	@Test
	void elTierYaNoLimitaLosArticulos() throws Exception {
		MockHttpSession basico = registrarUsuario(1);

		// El artículo que antes era Premium ahora es gratuito.
		this.mockMvc.perform(get("/api/v1/content/sepsis-reanimacion-inicial").session(basico))
			.andExpect(status().isOk());
		this.mockMvc.perform(get("/api/v1/content").param("tipo", "ARTICULO").param("size", "50").session(basico))
			.andExpect(jsonPath("$.items[*].slug", hasItem("sepsis-reanimacion-inicial")))
			.andExpect(jsonPath("$.items[*].tipo", everyItem(is("ARTICULO"))));
	}

	@Test
	void lasGuiasYRecursosAplicanSuPoliticaDeAcceso() throws Exception {
		// Sin sesión no se lee nada.
		this.mockMvc.perform(get("/api/v1/content/" + RECURSO_ESTUDIO_PREMIUM))
			.andExpect(status().isUnauthorized());
		this.mockMvc.perform(get("/api/v1/content/" + GUIA_PREMIUM)).andExpect(status().isUnauthorized());

		// Los recursos de estudio son Premium; la guía de la demo también exige Premium.
		for (int tier : new int[] { 1, 2 }) {
			MockHttpSession sesion = registrarUsuario(tier);
			this.mockMvc.perform(get("/api/v1/content/" + RECURSO_ESTUDIO_PREMIUM).session(sesion))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("TIER_INSUFICIENTE"))
				.andExpect(jsonPath("$.contenido").doesNotExist());
			this.mockMvc.perform(get("/api/v1/content/" + GUIA_PREMIUM).session(sesion))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("TIER_INSUFICIENTE"));
			this.mockMvc.perform(get("/api/v1/content").param("size", "50").session(sesion))
				.andExpect(jsonPath("$.items[*].slug", not(hasItem(RECURSO_ESTUDIO_PREMIUM))))
				.andExpect(jsonPath("$.items[*].slug", not(hasItem(GUIA_PREMIUM))));
		}

		MockHttpSession premium = registrarUsuario(3);
		this.mockMvc.perform(get("/api/v1/content/" + RECURSO_ESTUDIO_PREMIUM).session(premium))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.contenido.tipo").value("RECURSO_ESTUDIO"))
			.andExpect(jsonPath("$.contenido.recurso.procedencia").exists())
			.andExpect(jsonPath("$.acceso.tierNombre").value("Premium"));
		this.mockMvc.perform(get("/api/v1/content/" + GUIA_PREMIUM).session(premium))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.contenido.tipo").value("GUIA_PRACTICA_CLINICA"))
			.andExpect(jsonPath("$.contenido.guia.recomendaciones.length()").value(6))
			.andExpect(jsonPath("$.contenido.guia.recomendaciones[0].fuerza").value("Fuerte"))
			.andExpect(jsonPath("$.contenido.fuente.institucion").exists())
			.andExpect(jsonPath("$.contenido.responsables.revisorClinico").value("Ramón Revisor"))
			.andExpect(jsonPath("$.contenido.bloques[1].tipo").value("ALGORITMO"))
			.andExpect(jsonPath("$.acceso.tierNombre").value("Premium"));
	}

	@Test
	void ningunBorradorNiMaterialSinAutorizacionEsPublico() throws Exception {
		MockHttpSession premium = registrarUsuario(3);

		this.mockMvc.perform(get("/api/v1/content/" + BORRADOR).session(premium)).andExpect(status().isNotFound());
		this.mockMvc.perform(get("/api/v1/content/cirugia-enarm-apendicitis-aguda").session(premium))
			.andExpect(status().isNotFound());
		this.mockMvc.perform(get("/api/v1/content").param("q", "cirugia general enarm").session(premium))
			.andExpect(jsonPath("$.totalItems").value(0));
	}

	@Test
	void laBusquedaIgnoraAcentosYFiltraPorTipo() throws Exception {
		MockHttpSession premium = registrarUsuario(3);

		this.mockMvc.perform(get("/api/v1/content").param("q", "CARDIOLOGIA").session(premium))
			.andExpect(jsonPath("$.totalItems").value(3));
		this.mockMvc.perform(get("/api/v1/content").param("q", "sepsis").session(premium))
			.andExpect(jsonPath("$.totalItems").value(3));
		this.mockMvc.perform(get("/api/v1/content").param("q", "sepsis").param("tipo", "GUIA_PRACTICA_CLINICA")
			.session(premium)).andExpect(jsonPath("$.totalItems").value(1));
		this.mockMvc.perform(get("/api/v1/content").param("q", "100%_").session(premium))
			.andExpect(jsonPath("$.totalItems").value(0));
		this.mockMvc.perform(get("/api/v1/content").param("q", "x".repeat(101)).session(premium))
			.andExpect(status().isBadRequest());
	}

	@Test
	void abrirYCompletarActualizanElProgreso() throws Exception {
		MockHttpSession session = registrarUsuario(1);

		this.mockMvc.perform(get("/api/v1/me/learning").session(session))
			.andExpect(jsonPath("$.ultimo").doesNotExist());
		MvcResult abierto = this.mockMvc.perform(get("/api/v1/content/" + ARTICULO).session(session))
			.andExpect(jsonPath("$.progreso.estado").value("EN_PROGRESO"))
			.andReturn();
		String id = leer(abierto, "$.contenido.id");

		this.mockMvc.perform(get("/api/v1/me/learning").session(session))
			.andExpect(jsonPath("$.ultimo.contenido.slug").value(ARTICULO))
			.andExpect(jsonPath("$.enProgreso").value(1));
		this.mockMvc.perform(put("/api/v1/me/learning/{id}/complete", id).with(csrf()).session(session))
			.andExpect(jsonPath("$.estado").value("COMPLETADO"));
		this.mockMvc.perform(get("/api/v1/content/" + ARTICULO).session(session))
			.andExpect(jsonPath("$.progreso.estado").value("COMPLETADO"));
	}

	@Test
	void noSePuedeCompletarUnaGuiaFueraDelTier() throws Exception {
		String id = leer(this.mockMvc.perform(get("/api/v1/content/" + GUIA_PREMIUM).session(registrarUsuario(3)))
			.andReturn(), "$.contenido.id");

		this.mockMvc.perform(put("/api/v1/me/learning/{id}/complete", id).with(csrf()).session(registrarUsuario(1)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("TIER_INSUFICIENTE"));
	}

	@Test
	void elPatronDeBusquedaQuitaAcentosYEscapaComodines() {
		org.assertj.core.api.Assertions.assertThat(CatalogService.patronBusqueda("Cardiología"))
			.isEqualTo("%cardiologia%");
		org.assertj.core.api.Assertions.assertThat(CatalogService.patronBusqueda("100%_a\\b"))
			.isEqualTo("%100\\%\\_a\\\\b%");
	}

}
