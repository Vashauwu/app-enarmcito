package com.empresa.medicina;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de las pruebas de integración: API completa contra PostgreSQL real (Testcontainers) con las
 * migraciones y los datos demo aplicados.
 */
@SpringBootTest(properties = "app.media.storage-dir=target/test-media")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

	protected static final String DEMO_PASSWORD = "Demo1234";

	protected static final String PASSWORD = "contraseña-segura";

	@Autowired
	protected MockMvc mockMvc;

	protected static String correoUnico() {
		return "prueba-" + UUID.randomUUID() + "@example.com";
	}

	protected static String registro(String nombre, String correo, String password, long tierId) {
		return """
				{"nombre": "%s", "apellido": "Pérez", "correo": "%s", "password": "%s", "tierId": %d}
				""".formatted(nombre, correo, password, tierId);
	}

	protected static String credenciales(String correo, String password) {
		return """
				{"correo": "%s", "password": "%s"}
				""".formatted(correo, password);
	}

	/** Cuerpo JSON a partir de pares clave/valor; admite valores nulos. */
	protected String json(Object... claveValor) {
		java.util.Map<String, Object> mapa = new java.util.LinkedHashMap<>();
		for (int i = 0; i < claveValor.length; i += 2) {
			mapa.put((String) claveValor[i], claveValor[i + 1]);
		}
		return this.jsonMapper.writeValueAsString(mapa);
	}

	protected static String leer(MvcResult result, String path) throws Exception {
		return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), path).toString();
	}

	@Autowired
	protected tools.jackson.databind.json.JsonMapper jsonMapper;

	protected MockHttpSession iniciarSesion(String correo, String password) throws Exception {
		MvcResult result = this.mockMvc
			.perform(post("/api/v1/auth/login").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(credenciales(correo, password)))
			.andExpect(status().isOk())
			.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	/**
	 * Registra un usuario nuevo con el tier indicado (1 = Básico, 2 = Intermedio, 3 = Premium) y devuelve
	 * su sesión.
	 */
	protected MockHttpSession registrarUsuario(long tierId) throws Exception {
		MvcResult result = this.mockMvc
			.perform(post("/api/v1/auth/register").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(registro("Ana", correoUnico(), PASSWORD, tierId)))
			.andExpect(status().isCreated())
			.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

}
