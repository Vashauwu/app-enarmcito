package com.empresa.medicina.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints públicos para usar un enlace de acceso. El token viaja en el cuerpo, nunca en la URL de la API.
 */
@RestController
@RequestMapping("/api/v1/auth/acceso")
class AccessLinkController {

	private final AccessLinkService accessLinkService;

	AccessLinkController(AccessLinkService accessLinkService) {
		this.accessLinkService = accessLinkService;
	}

	@PostMapping("/consultar")
	AccessLinkService.InfoEnlace consultar(@Valid @RequestBody TokenRequest request) {
		return this.accessLinkService.consultar(request.token());
	}

	@PostMapping("/definir-password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void definirPassword(@Valid @RequestBody DefinirPasswordRequest request) {
		this.accessLinkService.definirPassword(request.token(), request.password());
	}

	record TokenRequest(@NotBlank @Size(max = 100) String token) {
	}

	record DefinirPasswordRequest(@NotBlank @Size(max = 100) String token,
			@NotBlank(message = "La contraseña es obligatoria.") @Size(min = 8, max = 72,
					message = "La contraseña debe tener entre 8 y 72 caracteres.") String password) {

		@Override
		public String toString() {
			return "DefinirPasswordRequest[]";
		}

	}

}
