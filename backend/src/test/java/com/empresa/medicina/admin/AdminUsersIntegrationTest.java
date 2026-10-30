package com.empresa.medicina.admin;

import java.util.UUID;

import com.empresa.medicina.IntegrationTest;
import com.empresa.medicina.auth.Rol;
import com.empresa.medicina.common.ApiException;
import com.empresa.medicina.users.UsuarioSistema;
import com.empresa.medicina.users.UsuarioSistemaRepository;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminUsersIntegrationTest extends IntegrationTest {

	@Autowired
	private UsuarioSistemaRepository cuentaRepository;

	@Autowired
	private AdminUserService adminUserService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	// ---------------------------------------------------------------------------------------------
	// Acceso por rol
	// ---------------------------------------------------------------------------------------------

	@Test
	void unAlumnoNoAccedeAlPanel() throws Exception {
		MockHttpSession alumno = registrarUsuario(1);

		this.mockMvc.perform(get("/api/v1/admin/dashboard").session(alumno)).andExpect(status().isForbidden());
		this.mockMvc.perform(get("/api/v1/admin/usuarios").session(alumno)).andExpect(status().isForbidden());
		this.mockMvc.perform(get("/api/v1/admin/contenido").session(alumno)).andExpect(status().isForbidden());
		this.mockMvc.perform(get("/api/v1/admin/auditoria").session(alumno)).andExpect(status().isForbidden());
		this.mockMvc.perform(get("/api/v1/auth/me").session(alumno))
			.andExpect(jsonPath("$.rol").value("ALUMNO"))
			.andExpect(jsonPath("$.permisos.length()").value(0));
	}

	@Test
	void cadaRolSoloAccedeASusModulos() throws Exception {
		MockHttpSession soporte = iniciarSesion("soporte@demo.local", DEMO_PASSWORD);
		this.mockMvc.perform(get("/api/v1/admin/contenido").session(soporte)).andExpect(status().isForbidden());
		this.mockMvc.perform(get("/api/v1/admin/auditoria").session(soporte)).andExpect(status().isForbidden());
		this.mockMvc.perform(get("/api/v1/admin/dashboard").session(soporte))
			.andExpect(jsonPath("$.usuarios").exists())
			.andExpect(jsonPath("$.contenido").doesNotExist());

		MockHttpSession autor = iniciarSesion("autor@demo.local", DEMO_PASSWORD);
		this.mockMvc.perform(get("/api/v1/admin/usuarios").session(autor)).andExpect(status().isForbidden());
		this.mockMvc.perform(get("/api/v1/admin/dashboard").session(autor))
			.andExpect(jsonPath("$.usuarios").doesNotExist())
			.andExpect(jsonPath("$.contenido.porEstado").exists());

		MockHttpSession revisor = iniciarSesion("revisor@demo.local", DEMO_PASSWORD);
		this.mockMvc
			.perform(post("/api/v1/admin/contenido").with(csrf())
				.session(revisor)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("titulo", "Intento", "tierMinimoId", 1)))
			.andExpect(status().isForbidden());

		MockHttpSession editor = iniciarSesion("editor@demo.local", DEMO_PASSWORD);
		this.mockMvc.perform(get("/api/v1/admin/usuarios").session(editor)).andExpect(status().isForbidden());
		this.mockMvc.perform(get("/api/v1/auth/me").session(editor))
			.andExpect(jsonPath("$.permisos", hasItem("content.publish")))
			.andExpect(jsonPath("$.permisos", not(hasItem("users.read"))));
	}

	// ---------------------------------------------------------------------------------------------
	// Consulta
	// ---------------------------------------------------------------------------------------------

	@Test
	void elListadoNoExponeCredencialesYPermiteFiltrar() throws Exception {
		MockHttpSession admin = iniciarSesion("usuarios@demo.local", DEMO_PASSWORD);

		this.mockMvc.perform(get("/api/v1/admin/usuarios").param("q", "REVISOR").session(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[*].correo", hasItem("revisor@demo.local")))
			.andExpect(content().string(not(containsString("password"))))
			.andExpect(content().string(not(containsString("{bcrypt}"))));

		this.mockMvc.perform(get("/api/v1/admin/usuarios").param("rol", "SOPORTE").session(admin))
			.andExpect(jsonPath("$.items[*].rol", everyItem(is("SOPORTE"))));
	}

	@Test
	void soporteSoloVeCuentasDeAlumnos() throws Exception {
		MockHttpSession soporte = iniciarSesion("soporte@demo.local", DEMO_PASSWORD);

		this.mockMvc.perform(get("/api/v1/admin/usuarios").param("size", "50").session(soporte))
			.andExpect(jsonPath("$.items[*].rol", everyItem(is("ALUMNO"))));
		this.mockMvc.perform(get("/api/v1/admin/usuarios/00000000-0000-4000-8000-000000000010").session(soporte))
			.andExpect(status().isNotFound());
	}

	// ---------------------------------------------------------------------------------------------
	// Estado de cuenta
	// ---------------------------------------------------------------------------------------------

	@Test
	void bloquearInvalidaLaSesionYDesbloquearRestableceElAcceso() throws Exception {
		String correo = correoUnico();
		MockHttpSession alumno = registrar(correo);
		String id = idDe(alumno);
		MockHttpSession soporte = iniciarSesion("soporte@demo.local", DEMO_PASSWORD);

		this.mockMvc
			.perform(post("/api/v1/admin/usuarios/{id}/bloquear", id).with(csrf())
				.session(soporte)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("motivo", " ")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors[0].field").value("motivo"));

		this.mockMvc
			.perform(post("/api/v1/admin/usuarios/{id}/bloquear", id).with(csrf())
				.session(soporte)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("motivo", "Uso indebido reportado")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.usuario.estado").value("BLOQUEADO"))
			.andExpect(jsonPath("$.bloqueoMotivo").value("Uso indebido reportado"));

		// La sesión previa deja de servir y no puede volver a entrar.
		this.mockMvc.perform(get("/api/v1/me/learning").session(alumno)).andExpect(status().isUnauthorized());
		this.mockMvc
			.perform(post("/api/v1/auth/login").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(credenciales(correo, PASSWORD)))
			.andExpect(status().isUnauthorized());

		this.mockMvc
			.perform(post("/api/v1/admin/usuarios/{id}/desbloquear", id).with(csrf())
				.session(soporte)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("motivo", "Aclarado con el usuario")))
			.andExpect(jsonPath("$.usuario.estado").value("ACTIVO"));
		iniciarSesion(correo, PASSWORD);

		MockHttpSession auditor = iniciarSesion("usuarios@demo.local", DEMO_PASSWORD);
		this.mockMvc
			.perform(get("/api/v1/admin/auditoria").param("recursoId", id)
				.param("accion", "USUARIO_BLOQUEADO")
				.session(auditor))
			.andExpect(jsonPath("$.items[0].motivo").value("Uso indebido reportado"))
			.andExpect(jsonPath("$.items[0].actor.correo").value("soporte@demo.local"))
			.andExpect(jsonPath("$.items[0].cambios.estado.despues").value("BLOQUEADO"));
	}

	@Test
	void soporteNoPuedeCambiarTierNiRol() throws Exception {
		String id = idDe(registrarUsuario(1));
		MockHttpSession soporte = iniciarSesion("soporte@demo.local", DEMO_PASSWORD);

		this.mockMvc
			.perform(put("/api/v1/admin/usuarios/{id}", id).with(csrf())
				.session(soporte)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("nombre", "Ana", "apellido", "Pérez", "tierId", 3)))
			.andExpect(status().isForbidden());
		this.mockMvc
			.perform(put("/api/v1/admin/usuarios/{id}/rol", id).with(csrf())
				.session(soporte)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("rol", "EDITOR_CONTENIDO", "motivo", "Prueba")))
			.andExpect(status().isForbidden());
	}

	@Test
	void cambioDeTierQuedaAuditadoYSurteEfecto() throws Exception {
		MockHttpSession alumno = registrarUsuario(1);
		String id = idDe(alumno);
		MockHttpSession admin = iniciarSesion("usuarios@demo.local", DEMO_PASSWORD);

		this.mockMvc
			.perform(put("/api/v1/admin/usuarios/{id}", id).with(csrf())
				.session(admin)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("nombre", "Ana", "apellido", "Pérez", "tierId", 3, "motivo", "Beca aprobada")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.usuario.tier.codigo").value("PREMIUM"));

		// El alumno conserva su sesión y ya puede leer la guía Premium.
		this.mockMvc.perform(get("/api/v1/content/gpc-sepsis-choque-septico-adultos").session(alumno))
			.andExpect(status().isOk());
		this.mockMvc
			.perform(get("/api/v1/admin/auditoria").param("recursoId", id)
				.param("accion", "USUARIO_TIER_CAMBIADO")
				.session(admin))
			.andExpect(jsonPath("$.items[0].motivo").value("Beca aprobada"))
			.andExpect(jsonPath("$.items[0].cambios.tier.antes").value("BASICO"));
	}

	// ---------------------------------------------------------------------------------------------
	// Roles
	// ---------------------------------------------------------------------------------------------

	@Test
	void laAsignacionDeRolesRespetaLaJerarquia() throws Exception {
		MockHttpSession alumno = registrarUsuario(1);
		String id = idDe(alumno);
		MockHttpSession adminUsuarios = iniciarSesion("usuarios@demo.local", DEMO_PASSWORD);

		cambiarRol(adminUsuarios, id, "ADMIN_PLATAFORMA").andExpect(status().isForbidden());
		cambiarRol(adminUsuarios, id, "EDITOR_CONTENIDO").andExpect(status().isOk())
			.andExpect(jsonPath("$.usuario.rol").value("EDITOR_CONTENIDO"));

		// El nuevo rol rige desde la siguiente petición, sin volver a iniciar sesión.
		this.mockMvc.perform(get("/api/v1/admin/contenido").session(alumno)).andExpect(status().isOk());

		MockHttpSession plataforma = iniciarSesion("plataforma@demo.local", DEMO_PASSWORD);
		cambiarRol(plataforma, id, "SUPER_ADMIN").andExpect(status().isForbidden());
		cambiarRol(plataforma, "00000000-0000-4000-8000-000000000011", "EDITOR_CONTENIDO")
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACCION_PROPIA"));
		cambiarRol(plataforma, "00000000-0000-4000-8000-000000000010", "ALUMNO").andExpect(status().isForbidden());
	}

	@Test
	void nadiePuedeBloquearSuPropiaCuenta() throws Exception {
		MockHttpSession superAdmin = iniciarSesion("superadmin@demo.local", DEMO_PASSWORD);

		this.mockMvc
			.perform(post("/api/v1/admin/usuarios/{id}/bloquear", "00000000-0000-4000-8000-000000000010").with(csrf())
				.session(superAdmin)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("motivo", "Prueba")))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACCION_PROPIA"));
	}

	/**
	 * Escenario que sólo se alcanza con cambios concurrentes: el servicio nunca deja el sistema sin un
	 * SUPER_ADMIN activo. Se ejecuta dentro de una transacción que se revierte al terminar.
	 */
	@Test
	@Transactional
	void siempreQuedaUnSuperAdminActivo() {
		UsuarioSistema original = this.cuentaRepository
			.findConPerfilYTier(UUID.fromString("00000000-0000-4000-8000-000000000010"))
			.orElseThrow();
		UsuarioSistema segundo = this.cuentaRepository
			.findConPerfilYTier(UUID.fromString("00000000-0000-4000-8000-000000000011"))
			.orElseThrow();
		segundo.cambiarRol(Rol.SUPER_ADMIN);
		original.bloquear("Escenario de prueba", java.time.Instant.now());
		this.cuentaRepository.flush();

		assertThatThrownBy(() -> this.adminUserService.cambiarRol(original, segundo.getUsuarioId(),
				new AdminUserDtos.RolRequest(Rol.ADMIN_PLATAFORMA, "Prueba")))
			.isInstanceOf(ApiException.class)
			.extracting("code")
			.isEqualTo("ULTIMO_SUPER_ADMIN");
		assertThatThrownBy(() -> this.adminUserService.bloquear(original, segundo.getUsuarioId(), "Prueba"))
			.extracting("code")
			.isEqualTo("ULTIMO_SUPER_ADMIN");
	}

	// ---------------------------------------------------------------------------------------------
	// Invitación y restablecimiento
	// ---------------------------------------------------------------------------------------------

	@Test
	void unaInvitacionActivaLaCuentaUnaSolaVez() throws Exception {
		MockHttpSession admin = iniciarSesion("usuarios@demo.local", DEMO_PASSWORD);
		String correo = correoUnico();

		this.mockMvc
			.perform(post("/api/v1/admin/usuarios").with(csrf())
				.session(admin)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("nombre", "Laura", "apellido", "Ríos", "correo", correo, "rol", "ADMIN_PLATAFORMA",
						"tierId", 1)))
			.andExpect(status().isForbidden());

		MvcResult invitacion = this.mockMvc
			.perform(post("/api/v1/admin/usuarios").with(csrf())
				.session(admin)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("nombre", "Laura", "apellido", "Ríos", "correo", correo, "rol", "REVISOR_MEDICO",
						"tierId", 1)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.usuario.usuario.estado").value("PENDIENTE"))
			.andExpect(jsonPath("$.enlace.tipo").value("INVITACION"))
			.andExpect(jsonPath("$.enlace.url", containsString("/acceso#token=")))
			.andReturn();
		String token = leer(invitacion, "$.enlace.url").split("#token=")[1];

		// Una cuenta pendiente no puede iniciar sesión.
		this.mockMvc
			.perform(post("/api/v1/auth/login").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(credenciales(correo, PASSWORD)))
			.andExpect(status().isUnauthorized());

		this.mockMvc
			.perform(post("/api/v1/auth/acceso/consultar").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("token", token)))
			.andExpect(jsonPath("$.nombre").value("Laura"));
		this.mockMvc
			.perform(post("/api/v1/auth/acceso/definir-password").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("token", token, "password", PASSWORD)))
			.andExpect(status().isNoContent());

		MockHttpSession invitada = iniciarSesion(correo, PASSWORD);
		this.mockMvc.perform(get("/api/v1/auth/me").session(invitada))
			.andExpect(jsonPath("$.rol").value("REVISOR_MEDICO"));

		this.mockMvc
			.perform(post("/api/v1/auth/acceso/definir-password").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("token", token, "password", "otra-contraseña")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("ENLACE_INVALIDO"));

		String hash = this.jdbcTemplate.queryForObject("""
				select t.token_hash from token_acceso t join usuarios u on u.id = t.usuario_id where u.correo = ?
				""", String.class, correo);
		assertThat(hash).hasSize(64).doesNotContain(token);
	}

	@Test
	void soporteRestableceElAccesoDeUnAlumno() throws Exception {
		String correo = correoUnico();
		String id = idDe(registrar(correo));
		MockHttpSession soporte = iniciarSesion("soporte@demo.local", DEMO_PASSWORD);

		MvcResult resultado = this.mockMvc
			.perform(post("/api/v1/admin/usuarios/{id}/restablecer-acceso", id).with(csrf())
				.session(soporte)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("motivo", "Olvidó su contraseña")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.enlace.tipo").value("RESTABLECIMIENTO"))
			.andReturn();
		String token = leer(resultado, "$.enlace.url").split("#token=")[1];

		this.mockMvc
			.perform(post("/api/v1/auth/acceso/definir-password").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("token", token, "password", "nueva-contraseña")))
			.andExpect(status().isNoContent());
		iniciarSesion(correo, "nueva-contraseña");
	}

	@Test
	void laBitacoraNoAdmiteModificaciones() {
		assertThatThrownBy(() -> this.jdbcTemplate.update("update auditoria_evento set resumen = 'x'"))
			.hasMessageContaining("bitácora");
		assertThatThrownBy(() -> this.jdbcTemplate.update("delete from auditoria_evento"))
			.hasMessageContaining("bitácora");
	}

	// ---------------------------------------------------------------------------------------------

	private MockHttpSession registrar(String correo) throws Exception {
		return (MockHttpSession) this.mockMvc
			.perform(post("/api/v1/auth/register").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(registro("Ana", correo, PASSWORD, 1)))
			.andExpect(status().isCreated())
			.andReturn()
			.getRequest()
			.getSession(false);
	}

	private String idDe(MockHttpSession session) throws Exception {
		return leer(this.mockMvc.perform(get("/api/v1/auth/me").session(session)).andReturn(), "$.id");
	}

	private org.springframework.test.web.servlet.ResultActions cambiarRol(MockHttpSession session, String id,
			String rol) throws Exception {
		return this.mockMvc.perform(put("/api/v1/admin/usuarios/{id}/rol", id).with(csrf())
			.session(session)
			.contentType(MediaType.APPLICATION_JSON)
			.content(json("rol", rol, "motivo", "Cambio de responsabilidades")));
	}

}
