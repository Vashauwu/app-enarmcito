package com.empresa.medicina.tiers;

public record TierResponse(Long id, String codigo, String nombre, String descripcion, int ordenAcceso) {

	public static TierResponse from(Tier tier) {
		return new TierResponse(tier.getId(), tier.getCodigo(), tier.getNombre(), tier.getDescripcion(),
				tier.getOrdenAcceso());
	}

}
