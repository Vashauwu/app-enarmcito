package com.empresa.medicina.auth;

import com.empresa.medicina.IntegrationTest;
import com.empresa.medicina.users.Usuario;
import com.empresa.medicina.users.UsuarioSistemaRepository;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTest {

	@Autowired
	private UsuarioSistemaRepository cuentaRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void registroCorrectoCreaLaCuentaEIniciaSesion() throws Exception {
		String correo = correoUnico();
		MockHttpSession session = (MockHttpSession) this.mockMvc
			.perform(post("/api/v1/auth/register").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(registro("  Ana ", correo.toUpperCase(), PASSWORD, 2)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.nombre").value("Ana"))
			.andExpect(jsonPath("$.correo").value(correo))
			.andExpect(jsonPath("$.tier.codigo").value("INTERMEDIO"))
			.andExpect(jsonPath("$.password").doesNotExist())
			.andExpect(jsonPath("$.passwordHash").doesNotExist())
			.andExpect(content().string(not(containsString(PASSWORD))))
			.andReturn()
			.getRequest()
			.getSession(false);

		this.mockMvc.perform(get("/api/v1/auth/me").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.correo").value(correo));

		String hash = this.cuentaRepository.findByCorreo(correo).orElseThrow().getPasswordHash();
		assertThat(hash).isNotEqualTo(PASSWORD).startsWith("{bcrypt}");
		assertThat(this.passwordEncoder.matches(PASSWORD, hash)).isTrue();
	}

	@Test
	void registroConCorreoDuplicadoEsRechazado() throws Exception {
		String correo = correoUnico();
		registrar(correo).andExpect(status().isCreated());

		registrar(correo.toUpperCase()).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("CORREO_DUPLICADO"))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("correo"));
	}

	@Test
	void registroConDatosInvalidosDevuelveErroresPorCampo() throws Exception {
		this.mockMvc
			.perform(post("/api/v1/auth/register").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(registro("", "no-es-correo", "corta", 1)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDACION"))
			.andExpect(jsonPath("$.fieldErrors[*].field", hasItem("nombre")))
			.andExpect(jsonPath("$.fieldErrors[*].field", hasItem("correo")))
			.andExpect(jsonPath("$.fieldErrors[*].field", hasItem("password")));
	}

	@Test
	void registroConTierInexistenteEsRechazado() throws Exception {
		this.mockMvc
			.perform(post("/api/v1/auth/register").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(registro("Ana", correoUnico(), PASSWORD, 999)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("TIER_INVALIDO"));
	}

	@Test
	void loginCorrectoYUsuarioActual() throws Exception {
		MockHttpSession session = iniciarSesion("BASICO@demo.local", DEMO_PASSWORD);

		this.mockMvc.perform(get("/api/v1/auth/me").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.correo").value("basico@demo.local"))
			.andExpect(jsonPath("$.tier.codigo").value("BASICO"));
	}

	@Test
	void loginIncorrectoDevuelveMensajeGenerico() throws Exception {
		String esperado = "Correo o contraseña incorrectos.";
		this.mockMvc
			.perform(post("/api/v1/auth/login").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(credenciales("basico@demo.local", "incorrecta")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("CREDENCIALES_INVALIDAS"))
			.andExpect(jsonPath("$.message").value(esperado));

		this.mockMvc
			.perform(post("/api/v1/auth/login").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(credenciales("no-existe@demo.local", "incorrecta")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value(esperado));
	}

	@Test
	void endpointsPrivadosRequierenSesion() throws Exception {
		this.mockMvc.perform(get("/api/v1/auth/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
		this.mockMvc.perform(get("/api/v1/me/learning")).andExpect(status().isUnauthorized());
		this.mockMvc.perform(get("/api/v1/me/profile")).andExpect(status().isUnauthorized());
	}

	@Test
	void peticionesQueModificanDatosRequierenTokenCsrf() throws Exception {
		this.mockMvc
			.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(credenciales("basico@demo.local", DEMO_PASSWORD)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("CSRF_INVALIDO"));
	}

	@Test
	void cerrarSesionInvalidaLaSesion() throws Exception {
		MockHttpSession session = registrarUsuario(1);

		this.mockMvc.perform(post("/api/v1/auth/logout").with(csrf()).session(session))
			.andExpect(status().isNoContent());

		assertThat(session.isInvalid()).isTrue();
		this.mockMvc.perform(get("/api/v1/auth/me").session(new MockHttpSession()))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void elUsuarioActualizaSoloSuNombreYApellido() throws Exception {
		MockHttpSession session = registrarUsuario(1);

		this.mockMvc
			.perform(put("/api/v1/me/profile").with(csrf())
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nombre": "Lucía", "apellido": "Gómez", "correo": "otro@example.com"}
						"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nombre").value("Lucía"))
			.andExpect(jsonPath("$.apellido").value("Gómez"))
			.andExpect(jsonPath("$.correo").value(not("otro@example.com")));

		this.mockMvc
			.perform(put("/api/v1/me/profile").with(csrf())
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nombre": " ", "apellido": "Gómez"}
						"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors[0].field").value("nombre"));
	}

	@Test
	void elCatalogoDeTiersEsPublico() throws Exception {
		this.mockMvc.perform(get("/api/v1/tiers"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].codigo").value("BASICO"))
			.andExpect(jsonPath("$.length()").value(3));
	}

	@Test
	void elCorreoSeNormaliza() {
		assertThat(Usuario.normalizarCorreo("  Ana.Perez@Example.COM ")).isEqualTo("ana.perez@example.com");
	}

	private org.springframework.test.web.servlet.ResultActions registrar(String correo) throws Exception {
		return this.mockMvc.perform(post("/api/v1/auth/register").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(registro("Ana", correo, PASSWORD, 1)));
	}

}
