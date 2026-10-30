package com.empresa.medicina.progress;

import java.util.UUID;

import com.empresa.medicina.auth.AuthenticatedUser;
import com.empresa.medicina.content.CatalogService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/learning")
class LearningController {

	private final ProgressService progressService;

	private final CatalogService catalogService;

	LearningController(ProgressService progressService, CatalogService catalogService) {
		this.progressService = progressService;
		this.catalogService = catalogService;
	}

	@GetMapping
	LearningResponse resumen(@AuthenticationPrincipal AuthenticatedUser user) {
		return this.progressService.resumen(this.catalogService.lector(user));
	}

	@PutMapping("/{contenidoId}/complete")
	ProgressResponse completar(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID contenidoId) {
		return this.progressService.completar(this.catalogService.lector(user), contenidoId);
	}

}
