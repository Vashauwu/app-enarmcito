package com.empresa.medicina.users;

import com.empresa.medicina.auth.AuthenticatedUser;
import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Perfil del usuario autenticado. El id siempre sale de la sesión: nadie puede editar datos ajenos.
 */
@RestController
@RequestMapping("/api/v1/me/profile")
class ProfileController {

	private final UserService userService;

	ProfileController(UserService userService) {
		this.userService = userService;
	}

	@PutMapping
	UserResponse actualizar(@AuthenticationPrincipal AuthenticatedUser user,
			@Valid @RequestBody UpdateProfileRequest request) {
		return this.userService.actualizarPerfil(user.getId(), request);
	}

}
