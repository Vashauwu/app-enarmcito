package com.empresa.medicina.auth;

import java.net.URI;
import java.util.UUID;

import com.empresa.medicina.IntegrationTest;
import com.empresa.medicina.auth.GoogleOidcClient.GoogleIdentidad;
import com.empresa.medicina.auth.GoogleOidcClient.GoogleOidcException;
import com.empresa.medicina.users.UsuarioSistema;
import com.empresa.medicina.users.UsuarioSistemaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo de Google con un cliente OIDC simulado: la red de Google no participa, pero state, sesión, vinculación
 * y reglas de cuenta se ejercitan completos contra PostgreSQL.
 */
@TestPropertySource(properties = { "app.google.client-id=cliente-prueba", "app.google.client-secret=secreto-prueba",
		"app.google.redirect-uri=http://localhost:4200/api/v1/auth/google/callback" })
@Import(GoogleLoginIntegrationTest.GoogleSimulado.class)
class GoogleLoginIntegrationTest extends IntegrationTest {

	@TestConfiguration
	static class GoogleSimulado {

		@Bean
		@Primary
		FakeGoogle googleOidcClient() {
			return new FakeGoogle();
		}

	}

	static class FakeGoogle implements GoogleOidcClient {

		GoogleIdentidad respuesta;

		String ultimoVerifier;

		boolean fallar;

		@Override
		public URI urlAutorizacion(String state, String nonce, String codeChallenge) {
			return URI.create("https://accounts.google.test/auth?state=" + state + "&nonce=" + nonce
					+ "&code_challenge=" + codeChallenge + "&code_challenge_method=S256&scope=openid%20email%20profile");
		}

		@Override
		public GoogleIdentidad canjear(String code, String codeVerifier, String nonce) {
			this.ultimoVerifier = codeVerifier;
			if (this.fallar) {
				throw new GoogleOidcException("simulado");
			}
			return this.respuesta;
		}

	}

	@Autowired
	private FakeGoogle google;

	@Autowired
	private UsuarioSistemaRepository cuentaRepository;

	@BeforeEach
	void reiniciar() {
		this.google.fallar = false;
		this.google.respuesta = null;
	}

	@Test
	void anunciaQueGoogleEstaDisponible() throws Exception {
		this.mockMvc.perform(get("/api/v1/auth/proveedores"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.google").value(true));
	}

	@Test
	void iniciarUsaStateNonceYPkce() throws Exception {
		MvcResult result = this.mockMvc.perform(get("/api/v1/auth/google/iniciar")).andReturn();

		assertThat(result.getResponse().getStatus()).isEqualTo(302);
		var query = UriComponentsBuilder.fromUriString(result.getResponse().getHeader("Location"))
			.build()
			.getQueryParams();
		assertThat(query.getFirst("state")).hasSizeGreaterThan(30);
		assertThat(query.getFirst("nonce")).hasSizeGreaterThan(30).isNotEqualTo(query.getFirst("state"));
		assertThat(query.getFirst("code_challenge_method")).isEqualTo("S256");
		assertThat(query.getFirst("scope")).isEqualTo("openid%20email%20profile");
		assertThat(result.getRequest().getSession(false)).isNotNull();
	}

	@Test
	void usuarioNuevoCreaCuentaDeAlumnoSinContrasena() throws Exception {
		String correo = correoUnico();
		this.google.respuesta = new GoogleIdentidad(subject(), correo, true, "Lucía", "Gómez");

		MvcResult retorno = completarFlujo(null);

		assertThat(retorno.getResponse().getHeader("Location")).isEqualTo("http://localhost:4200/app");
		MockHttpSession sesion = (MockHttpSession) retorno.getRequest().getSession(false);
		this.mockMvc.perform(get("/api/v1/auth/me").session(sesion))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.correo").value(correo))
			.andExpect(jsonPath("$.nombre").value("Lucía"))
			.andExpect(jsonPath("$.rol").value("ALUMNO"))
			.andExpect(jsonPath("$.tier.codigo").value("BASICO"));

		UsuarioSistema cuenta = this.cuentaRepository.findByCorreo(correo).orElseThrow();
		assertThat(cuenta.getPasswordHash()).isNull();
		// Sin contraseña: el login por contraseña falla de forma controlada, no con un error interno.
		this.mockMvc
			.perform(post("/api/v1/auth/login").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(credenciales(correo, "cualquier-contraseña")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("CREDENCIALES_INVALIDAS"));
	}

	@Test
	void unSegundoInicioReutilizaLaMismaCuentaAunqueCambieElCorreo() throws Exception {
		String subject = subject();
		String correo = correoUnico();
		this.google.respuesta = new GoogleIdentidad(subject, correo, true, "Ana", "Ruiz");
		completarFlujo(null);

		this.google.respuesta = new GoogleIdentidad(subject, correoUnico(), true, "Ana", "Ruiz");
		MvcResult retorno = completarFlujo(null);

		assertThat(retorno.getResponse().getHeader("Location")).isEqualTo("http://localhost:4200/app");
		MockHttpSession sesion = (MockHttpSession) retorno.getRequest().getSession(false);
		// El correo de Google no es el identificador: sigue siendo la misma cuenta.
		this.mockMvc.perform(get("/api/v1/auth/me").session(sesion))
			.andExpect(jsonPath("$.correo").value(correo));
	}

	@Test
	void correoDeCuentaExistenteNoSeVinculaEnSilencio() throws Exception {
		String correo = correoUnico();
		this.mockMvc
			.perform(post("/api/v1/auth/register").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(registro("Ana", correo, PASSWORD, 1)))
			.andExpect(status().isCreated());
		this.google.respuesta = new GoogleIdentidad(subject(), correo, true, "Intruso", "X");

		MvcResult retorno = completarFlujo(null);

		assertThat(retorno.getResponse().getHeader("Location"))
			.isEqualTo("http://localhost:4200/login?google=GOOGLE_CORREO_EXISTENTE");
		this.mockMvc.perform(get("/api/v1/auth/me").session((MockHttpSession) retorno.getRequest().getSession(false)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void correoNoVerificadoSeRechaza() throws Exception {
		this.google.respuesta = new GoogleIdentidad(subject(), correoUnico(), false, "Ana", "Ruiz");

		MvcResult retorno = completarFlujo(null);

		assertThat(retorno.getResponse().getHeader("Location"))
			.isEqualTo("http://localhost:4200/login?google=GOOGLE_CORREO_NO_VERIFICADO");
	}

	@Test
	void unStateInvalidoOUnaRepeticionSeRechazan() throws Exception {
		this.google.respuesta = new GoogleIdentidad(subject(), correoUnico(), true, "Ana", "Ruiz");
		MvcResult inicio = this.mockMvc.perform(get("/api/v1/auth/google/iniciar")).andReturn();
		MockHttpSession sesion = (MockHttpSession) inicio.getRequest().getSession(false);
		String state = stateDe(inicio);

		this.mockMvc.perform(get("/api/v1/auth/google/callback").param("code", "c").param("state", "otro").session(sesion))
			.andExpect(status().isFound())
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
				.string("Location", "http://localhost:4200/login?google=GOOGLE_ESTADO_INVALIDO"));

		// El intento se consume aunque falle: el state correcto tampoco sirve después (replay).
		this.mockMvc.perform(get("/api/v1/auth/google/callback").param("code", "c").param("state", state).session(sesion))
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
				.string("Location", "http://localhost:4200/login?google=GOOGLE_ESTADO_INVALIDO"));
	}

	@Test
	void unCallbackSinSesionDeInicioSeRechaza() throws Exception {
		this.mockMvc.perform(get("/api/v1/auth/google/callback").param("code", "c").param("state", "x"))
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
				.string("Location", "http://localhost:4200/login?google=GOOGLE_ESTADO_INVALIDO"));
	}

	@Test
	void unFalloDeGoogleNoDejaSesion() throws Exception {
		this.google.fallar = true;

		MvcResult retorno = completarFlujo(null);

		assertThat(retorno.getResponse().getHeader("Location"))
			.isEqualTo("http://localhost:4200/login?google=GOOGLE_ERROR");
		this.mockMvc.perform(get("/api/v1/auth/me").session((MockHttpSession) retorno.getRequest().getSession(false)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void unaCuentaBloqueadaNoPuedeEntrarConGoogle() throws Exception {
		String correo = correoUnico();
		this.google.respuesta = new GoogleIdentidad(subject(), correo, true, "Ana", "Ruiz");
		completarFlujo(null);
		UsuarioSistema cuenta = this.cuentaRepository.findByCorreo(correo).orElseThrow();
		cuenta.bloquear("prueba", java.time.Instant.now());
		this.cuentaRepository.save(cuenta);

		MvcResult retorno = completarFlujo(null);

		assertThat(retorno.getResponse().getHeader("Location"))
			.isEqualTo("http://localhost:4200/login?google=GOOGLE_CUENTA_INACTIVA");
	}

	@Test
	void unaSesionIniciadaPuedeVincularYDesvincularGoogle() throws Exception {
		String correo = correoUnico();
		MockHttpSession sesion = (MockHttpSession) this.mockMvc
			.perform(post("/api/v1/auth/register").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(registro("Ana", correo, PASSWORD, 1)))
			.andExpect(status().isCreated())
			.andReturn()
			.getRequest()
			.getSession(false);
		this.google.respuesta = new GoogleIdentidad(subject(), "otro-correo@example.com", true, "Ana", "Ruiz");

		MvcResult retorno = completarFlujo(sesion, "vincular");

		assertThat(retorno.getResponse().getHeader("Location"))
			.isEqualTo("http://localhost:4200/app/perfil?google=vinculada");
		this.mockMvc.perform(get("/api/v1/me/identidades").session(sesion))
			.andExpect(jsonPath("$.googleVinculado").value(true))
			.andExpect(jsonPath("$.tienePassword").value(true));

		this.mockMvc.perform(post("/api/v1/me/identidades/google/desvincular").with(csrf()).session(sesion))
			.andExpect(status().isNoContent());
		this.mockMvc.perform(get("/api/v1/me/identidades").session(sesion))
			.andExpect(jsonPath("$.googleVinculado").value(false));
	}

	@Test
	void vincularExigeSesionIniciada() throws Exception {
		MvcResult result = this.mockMvc.perform(get("/api/v1/auth/google/iniciar").param("modo", "vincular"))
			.andReturn();

		assertThat(result.getResponse().getHeader("Location"))
			.isEqualTo("http://localhost:4200/login?google=GOOGLE_ESTADO_INVALIDO");
	}

	@Test
	void unaIdentidadYaVinculadaAOtraCuentaNoSeRoba() throws Exception {
		String subject = subject();
		this.google.respuesta = new GoogleIdentidad(subject, correoUnico(), true, "Ana", "Ruiz");
		completarFlujo(null);

		MockHttpSession otra = (MockHttpSession) this.mockMvc
			.perform(post("/api/v1/auth/register").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(registro("Beto", correoUnico(), PASSWORD, 1)))
			.andReturn()
			.getRequest()
			.getSession(false);
		MvcResult retorno = completarFlujo(otra, "vincular");

		assertThat(retorno.getResponse().getHeader("Location"))
			.isEqualTo("http://localhost:4200/app/perfil?google=GOOGLE_YA_VINCULADA");
	}

	@Test
	void unaCuentaSoloDeGoogleNoPuedeDesvincularse() throws Exception {
		this.google.respuesta = new GoogleIdentidad(subject(), correoUnico(), true, "Ana", "Ruiz");
		MockHttpSession sesion = (MockHttpSession) completarFlujo(null).getRequest().getSession(false);

		this.mockMvc.perform(post("/api/v1/me/identidades/google/desvincular").with(csrf()).session(sesion))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("SIN_OTRO_METODO_DE_ACCESO"));
	}

	private MvcResult completarFlujo(MockHttpSession sesionInicial) throws Exception {
		return completarFlujo(sesionInicial, null);
	}

	/** Recorre iniciar → (Google) → callback con el state emitido por el backend. */
	private MvcResult completarFlujo(MockHttpSession sesionInicial, String modo) throws Exception {
		var peticion = get("/api/v1/auth/google/iniciar");
		if (modo != null) {
			peticion.param("modo", modo);
		}
		if (sesionInicial != null) {
			peticion.session(sesionInicial);
		}
		MvcResult inicio = this.mockMvc.perform(peticion).andReturn();
		MockHttpSession sesion = (MockHttpSession) inicio.getRequest().getSession(false);
		return this.mockMvc
			.perform(get("/api/v1/auth/google/callback").param("code", "codigo-de-prueba")
				.param("state", stateDe(inicio))
				.session(sesion))
			.andReturn();
	}

	private static String stateDe(MvcResult inicio) {
		return UriComponentsBuilder.fromUriString(inicio.getResponse().getHeader("Location"))
			.build()
			.getQueryParams()
			.getFirst("state");
	}

	private static String subject() {
		return "google-" + UUID.randomUUID();
	}

}
