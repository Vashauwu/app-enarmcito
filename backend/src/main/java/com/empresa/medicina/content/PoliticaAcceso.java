package com.empresa.medicina.content;

public enum PoliticaAcceso {

	/** Gratuito con cuenta, sin tier. Única política posible para un artículo. */
	REGISTRADO_GRATUITO,

	/** Requiere sesión y un tier igual o superior al tier mínimo. Única política de guías y recursos. */
	POR_TIER

}
