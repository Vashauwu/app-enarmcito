package com.empresa.medicina.tiers;

import java.util.List;

import com.empresa.medicina.common.ApiError;
import com.empresa.medicina.common.ApiException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TierService {

	private final TierRepository tierRepository;

	TierService(TierRepository tierRepository) {
		this.tierRepository = tierRepository;
	}

	public List<TierResponse> listarActivos() {
		return this.tierRepository.findByActivoTrueOrderByOrdenAccesoAsc().stream().map(TierResponse::from).toList();
	}

	/**
	 * Devuelve el tier activo con el id indicado o rechaza la solicitud como error de validación.
	 */
	public Tier obtenerActivo(Long tierId) {
		return this.tierRepository.findByIdAndActivoTrue(tierId)
			.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "TIER_INVALIDO",
					"El tier seleccionado no está disponible.",
					List.of(new ApiError.FieldError("tierId", "Selecciona un tier disponible."))));
	}

	public Tier obtenerTierInicial() {
		return this.tierRepository.findFirstByActivoTrueOrderByOrdenAccesoAsc()
			.orElseThrow(() -> new IllegalStateException("No hay tiers activos configurados"));
	}

}
