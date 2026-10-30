package com.empresa.medicina.content;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import com.empresa.medicina.IntegrationTest;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BloquesMediosIntegrationTest extends IntegrationTest {

	// ---------------------------------------------------------------------------------------------
	// Medios
	// ---------------------------------------------------------------------------------------------

	@Test
	void unaImagenValidaQuedaPendienteConVariantesYNoEsPublica() throws Exception {
		MockHttpSession autor = iniciarSesion("autor@demo.local", DEMO_PASSWORD);

		MvcResult cargado = cargar(autor, "figura.png", png(2000, 1000), metadatos("Figura de prueba", true, true))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.estado").value("PENDIENTE"))
			.andExpect(jsonPath("$.tipoMime").value("image/png"))
			.andExpect(jsonPath("$.ancho").value(2000))
			.andExpect(jsonPath("$.tieneVariantes").value(true))
			.andReturn();
		String id = leer(cargado, "$.id");

		this.mockMvc.perform(get("/api/v1/media/{id}", id).session(registrarUsuario(3)))
			.andExpect(status().isNotFound());
		this.mockMvc.perform(get("/api/v1/admin/media/{id}/archivo", id).session(autor))
			.andExpect(status().isOk())
			.andExpect(header().string("X-Content-Type-Options", "nosniff"));
	}

	@Test
	void elTipoSeValidaPorContenidoYNoPorExtension() throws Exception {
		MockHttpSession autor = iniciarSesion("autor@demo.local", DEMO_PASSWORD);

		cargar(autor, "foto.png", "no soy una imagen".getBytes(StandardCharsets.UTF_8), metadatos("x", true, true))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("ARCHIVO_INVALIDO"));
		byte[] pdfConScript = "%PDF-1.4\n1 0 obj << /OpenAction << /S /JavaScript /JS (app.alert(1)) >> >>\n%%EOF"
			.getBytes(StandardCharsets.ISO_8859_1);
		cargar(autor, "doc.pdf", pdfConScript, metadatos(null, true, true)).andExpect(status().isBadRequest());
	}

	@Test
	void unSvgSeSaneaAntesDeGuardarse() throws Exception {
		MockHttpSession autor = iniciarSesion("autor@demo.local", DEMO_PASSWORD);
		String svg = """
				<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 50" onload="alert(1)">
				  <script>alert(2)</script>
				  <rect x="0" y="0" width="100" height="50" fill="teal" style="background:url(http://x)"/>
				  <a href="https://malicioso.example"><text x="10" y="30">Diagrama</text></a>
				</svg>
				""";
		String id = leer(cargar(autor, "diagrama.svg", svg.getBytes(StandardCharsets.UTF_8), metadatos("Diagrama", true, true))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.ancho").value(100))
			.andReturn(), "$.id");

		this.mockMvc.perform(get("/api/v1/admin/media/{id}/archivo", id).session(autor))
			.andExpect(content().string(not(containsString("script"))))
			.andExpect(content().string(not(containsString("onload"))))
			.andExpect(content().string(not(containsString("malicioso"))))
			.andExpect(content().string(not(containsString("style"))))
			.andExpect(content().string(containsString("<rect")));
	}

	@Test
	void lasDeclaracionesYElTextoAlternativoSonObligatorios() throws Exception {
		MockHttpSession autor = iniciarSesion("autor@demo.local", DEMO_PASSWORD);

		cargar(autor, "a.png", png(10, 10), metadatos("Imagen", false, true)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("DECLARACION_REQUERIDA"));
		cargar(autor, "a.png", png(10, 10), metadatos("Imagen", true, false)).andExpect(status().isBadRequest());
		cargar(autor, "a.png", png(10, 10), metadatos(" ", true, true)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors[0].field").value("textoAlternativo"));
	}

	// ---------------------------------------------------------------------------------------------
	// Bloques y publicación
	// ---------------------------------------------------------------------------------------------

	@Test
	void losBloquesSeSaneanYSuOrdenSeConservaAlPublicar() throws Exception {
		MockHttpSession editor = iniciarSesion("editor@demo.local", DEMO_PASSWORD);
		MockHttpSession gestor = iniciarSesion("medios@demo.local", DEMO_PASSWORD);

		String id = leer(this.mockMvc.perform(post("/api/v1/admin/contenido").with(csrf())
			.session(editor)
			.contentType(MediaType.APPLICATION_JSON)
			.content(json("tipo", "RECURSO_ESTUDIO", "titulo", "Recurso con bloques " + UUID.randomUUID(),
					"areaMedica", "Cirugía general", "resumen", "Resumen.", "fuenteTitulo", "Elaboración propia",
					"fechaRevision", LocalDate.now().toString(), "tiempoLecturaMinutos", 4, "procedencia",
					"Equipo editorial", "titularDerechos", "Plataforma", "licenciaUso", "Uso educativo",
					"autorizacionPublicacion", true, "autorizacionReferencia", "Contenido propio",
					"revisionClinicaRequerida", false)))
			.andExpect(status().isCreated())
			.andReturn(), "$.id");

		// Texto enriquecido: se eliminan scripts, eventos y enlaces javascript.
		bloque(editor, id, "TEXTO_RICO",
				Map.of("html", "<p onclick=\"x()\">Hola <strong>mundo</strong><script>alert(1)</script>"
						+ "<a href=\"javascript:alert(2)\">enlace</a><img src=x onerror=alert(3)></p>"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.bloques[0].datos.html").value("<p>Hola <strong>mundo</strong>enlace</p>"));
		// Una tabla con filas de longitud distinta se rechaza; una correcta se acepta.
		bloque(editor, id, "TABLA", Map.of("encabezados", List.of("A", "B"), "filas", List.of(List.of("1"))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("BLOQUE_INVALIDO"));
		bloque(editor, id, "TABLA", Map.of("encabezados", List.of("A", "B"), "filas", List.of(List.of("1", "2"))))
			.andExpect(status().isCreated());
		// Un algoritmo sin paso de inicio se rechaza.
		bloque(editor, id, "ALGORITMO", Map.of("titulo", "A", "version", "1", "fuente", "F", "descripcionTextual", "D",
				"nodos", List.of(Map.of("id", "a", "tipo", "ACCION", "texto", "Paso"), Map.of("id", "b", "tipo", "FIN",
						"texto", "Fin")),
				"conexiones", List.of())).andExpect(status().isBadRequest());

		// Imagen con medio pendiente: se puede agregar, pero no publicar.
		String medio = leer(cargar(gestor, "figura.png", png(800, 600), metadatos("Esquema anatómico", true, true))
			.andReturn(), "$.id");
		MvcResult conImagen = bloque(editor, id, "IMAGEN", Map.of("activoId", medio, "pie", "Figura 1")).andReturn();
		List<String> orden = com.jayway.jsonpath.JsonPath.read(conImagen.getResponse().getContentAsString(),
				"$.bloques[*].id");
		// Se reordena: imagen primero.
		this.mockMvc.perform(put("/api/v1/admin/contenido/{id}/bloques/orden", id).with(csrf())
			.session(editor)
			.contentType(MediaType.APPLICATION_JSON)
			.content(json("bloques", List.of(orden.get(2), orden.get(0), orden.get(1)))))
			.andExpect(jsonPath("$.bloques[0].tipo").value("IMAGEN"));

		this.mockMvc.perform(post("/api/v1/admin/contenido/{id}/enviar-revision", id).with(csrf())
			.session(editor)
			.contentType(MediaType.APPLICATION_JSON)
			.content(json("revisorId", null))).andExpect(status().isOk());
		this.mockMvc.perform(post("/api/v1/admin/contenido/{id}/revisiones", id).with(csrf())
			.session(editor)
			.contentType(MediaType.APPLICATION_JSON)
			.content(json("etapa", "EDITORIAL", "decision", "APROBAR")))
			.andExpect(jsonPath("$.estado").value("APROBADO"));
		this.mockMvc.perform(post("/api/v1/admin/contenido/{id}/publicar", id).with(csrf()).session(editor))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MEDIOS_PENDIENTES"));

		// El gestor aprueba el medio y la publicación procede.
		this.mockMvc.perform(post("/api/v1/admin/media/{id}/aprobar", medio).with(csrf()).session(gestor))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MEDIO_INCOMPLETO"));
		long version = Long.parseLong(
				leer(this.mockMvc.perform(get("/api/v1/admin/media/{id}", medio).session(gestor)).andReturn(), "$.version"));
		this.mockMvc.perform(put("/api/v1/admin/media/{id}", medio).with(csrf())
			.session(gestor)
			.contentType(MediaType.APPLICATION_JSON)
			.content(json("textoAlternativo", "Esquema anatómico", "credito", "Equipo editorial", "licencia",
					"Uso educativo", "derechosConfirmados", true, "sinDatosPaciente", true, "version", version)))
			.andExpect(status().isOk());
		this.mockMvc.perform(post("/api/v1/admin/media/{id}/aprobar", medio).with(csrf()).session(gestor))
			.andExpect(jsonPath("$.estado").value("APROBADO"));
		String slug = leer(this.mockMvc.perform(post("/api/v1/admin/contenido/{id}/publicar", id).with(csrf())
			.session(editor)).andExpect(jsonPath("$.versionPublicada").value(1)).andReturn(), "$.slug");

		// Los recursos de estudio son Premium: un alumno Básico no los lee ni obtiene sus medios.
		MockHttpSession basico = registrarUsuario(1);
		this.mockMvc.perform(get("/api/v1/content/" + slug).session(basico))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("TIER_INSUFICIENTE"));
		this.mockMvc.perform(get("/api/v1/media/{id}", medio).session(basico)).andExpect(status().isNotFound());

		// Lectura con tier Premium: orden conservado, etiqueta de recurso y medio con texto alternativo.
		MockHttpSession alumno = registrarUsuario(3);
		this.mockMvc.perform(get("/api/v1/content/" + slug).session(alumno))
			.andExpect(jsonPath("$.contenido.tipo").value("RECURSO_ESTUDIO"))
			.andExpect(jsonPath("$.contenido.bloques[0].tipo").value("IMAGEN"))
			.andExpect(jsonPath("$.contenido.bloques[0].medios[0].textoAlternativo").value("Esquema anatómico"))
			.andExpect(jsonPath("$.contenido.bloques[1].tipo").value("TEXTO_RICO"))
			.andExpect(jsonPath("$.contenido.bloques[2].tipo").value("TABLA"));

		// El medio se entrega a quien puede leer el recurso, no a un visitante sin sesión.
		this.mockMvc.perform(get("/api/v1/media/{id}", medio)).andExpect(status().isUnauthorized());
		byte[] variante = this.mockMvc.perform(get("/api/v1/media/{id}", medio).param("variante", "movil").session(alumno))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Type", "image/png"))
			.andReturn()
			.getResponse()
			.getContentAsByteArray();
		assertThat(ImageIO.read(new java.io.ByteArrayInputStream(variante)).getWidth()).isEqualTo(640);

		// Retirar un bloque de lo publicado no cambia la versión pública hasta republicar.
		this.mockMvc.perform(delete("/api/v1/admin/contenido/{id}/bloques/{bloque}", id, orden.get(1)).with(csrf())
			.session(editor)).andExpect(jsonPath("$.estado").value("EN_EDICION"));
		this.mockMvc.perform(get("/api/v1/content/" + slug).session(alumno))
			.andExpect(jsonPath("$.contenido.bloques.length()").value(3));
	}

	// ---------------------------------------------------------------------------------------------

	private ResultActions cargar(MockHttpSession session, String nombre, byte[] archivo, String metadatos)
			throws Exception {
		return this.mockMvc.perform(multipart("/api/v1/admin/media")
			.file(new MockMultipartFile("archivo", nombre, "application/octet-stream", archivo))
			.file(new MockMultipartFile("metadatos", "", MediaType.APPLICATION_JSON_VALUE,
					metadatos.getBytes(StandardCharsets.UTF_8)))
			.with(csrf())
			.session(session));
	}

	private String metadatos(String alt, boolean derechos, boolean sinPacientes) {
		return json("textoAlternativo", alt, "derechosConfirmados", derechos, "sinDatosPaciente", sinPacientes);
	}

	private ResultActions bloque(MockHttpSession session, String contenidoId, String tipo, Map<String, ?> datos)
			throws Exception {
		return this.mockMvc.perform(post("/api/v1/admin/contenido/{id}/bloques", contenidoId).with(csrf())
			.session(session)
			.contentType(MediaType.APPLICATION_JSON)
			.content(json("tipo", tipo, "datos", datos)));
	}

	private static byte[] png(int ancho, int alto) throws Exception {
		BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream salida = new ByteArrayOutputStream();
		ImageIO.write(imagen, "png", salida);
		return salida.toByteArray();
	}

}
