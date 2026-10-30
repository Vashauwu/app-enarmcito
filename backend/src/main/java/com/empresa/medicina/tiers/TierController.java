package com.empresa.medicina.tiers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tiers")
class TierController {

	private final TierService tierService;

	TierController(TierService tierService) {
		this.tierService = tierService;
	}

	@GetMapping
	List<TierResponse> listar() {
		return this.tierService.listarActivos();
	}

}
