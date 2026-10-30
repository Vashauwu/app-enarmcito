package com.empresa.medicina.content.bloques;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import org.springframework.stereotype.Component;

/**
 * Sanea el HTML de los bloques con una lista blanca: sólo formato de texto, listas, encabezados y enlaces
 * http(s). Elimina scripts, estilos, atributos de eventos, iframes, imágenes incrustadas y cualquier otro HTML.
 */
@Component
public class HtmlSanitizer {

	private static final PolicyFactory POLITICA = new HtmlPolicyBuilder()
		.allowElements("p", "br", "h3", "h4", "strong", "b", "em", "i", "u", "sub", "sup", "ul", "ol", "li",
				"blockquote", "a")
		.allowUrlProtocols("https", "http")
		.allowAttributes("href")
		.onElements("a")
		.requireRelsOnLinks("noopener", "noreferrer", "nofollow")
		.allowAttributes("target")
		.matching(true, "_blank")
		.onElements("a")
		.toFactory();

	public String sanear(String html) {
		return POLITICA.sanitize(html).strip();
	}

	/** Texto visible tras quitar las etiquetas, para saber si quedó contenido. */
	public static boolean tieneTexto(String htmlSaneado) {
		return !htmlSaneado.replaceAll("<[^>]*>", "").replace("&nbsp;", " ").isBlank();
	}

}
