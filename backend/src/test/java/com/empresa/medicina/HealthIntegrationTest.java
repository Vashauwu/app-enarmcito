package com.empresa.medicina;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Salud pública para la plataforma de despliegue y configuración del perfil {@code prod}: el ambiente público
 * nunca carga datos demo ni deja elegir tier al registrarse, y la cookie de sesión es {@code Secure}.
 */
@ActiveProfiles("prod")
class HealthIntegrationTest extends IntegrationTest {

	@Autowired
	private com.empresa.medicina.config.AppProperties properties;

	@Value("${server.servlet.session.cookie.secure}")
	private boolean cookieSecure;

	@Test
	void laSaludSeConsultaSinSesionYNoExponeDetalles() throws Exception {
		this.mockMvc.perform(get("/actuator/health"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("UP"))
			.andExpect(jsonPath("$.components").doesNotExist());
		this.mockMvc.perform(get("/actuator/health/liveness"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void soloLaSaludEstaExpuesta() throws Exception {
		this.mockMvc.perform(get("/actuator/env")).andExpect(status().is4xxClientError());
		this.mockMvc.perform(get("/actuator/beans")).andExpect(status().is4xxClientError());
	}

	@Test
	void elPerfilProdNoCargaDatosDemoYExigeCookieSegura() {
		assertThat(this.properties.demoData()).isFalse();
		assertThat(this.properties.registration().allowTierSelection()).isFalse();
		assertThat(this.cookieSecure).isTrue();
	}

}
