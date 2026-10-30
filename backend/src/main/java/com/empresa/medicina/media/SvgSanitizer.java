package com.empresa.medicina.media;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Sanea SVG con listas blancas de elementos y atributos. Rechaza DTD y entidades externas; elimina scripts,
 * {@code foreignObject}, manejadores de eventos, estilos y referencias que no sean internas ({@code #id}).
 */
final class SvgSanitizer {

	private static final String SVG_NS = "http://www.w3.org/2000/svg";

	private static final Set<String> ELEMENTOS = Set.of("svg", "g", "path", "rect", "circle", "ellipse", "line",
			"polyline", "polygon", "text", "tspan", "title", "desc", "defs", "lineargradient", "radialgradient", "stop",
			"clippath", "marker", "symbol", "use");

	private static final Set<String> ATRIBUTOS = Set.of("id", "class", "d", "x", "y", "x1", "y1", "x2", "y2", "cx",
			"cy", "r", "rx", "ry", "fx", "fy", "width", "height", "viewbox", "transform", "points", "fill", "fill-opacity",
			"fill-rule", "stroke", "stroke-width", "stroke-opacity", "stroke-linecap", "stroke-linejoin",
			"stroke-dasharray", "opacity", "font-size", "font-family", "font-weight", "font-style", "text-anchor",
			"dominant-baseline", "letter-spacing", "offset", "stop-color", "stop-opacity", "gradientunits",
			"gradienttransform", "clip-path", "marker-start", "marker-mid", "marker-end", "markerwidth", "markerheight",
			"refx", "refy", "orient", "preserveaspectratio", "version", "xmlns", "href", "xlink:href", "xmlns:xlink",
			"role", "aria-label", "dx", "dy");

	private SvgSanitizer() {
	}

	static Resultado sanear(byte[] contenido) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		builder.setErrorHandler(null);
		Document documento = builder.parse(new ByteArrayInputStream(contenido));
		Element raiz = documento.getDocumentElement();
		if (!"svg".equals(raiz.getLocalName()) || !SVG_NS.equals(raiz.getNamespaceURI())) {
			throw new IllegalArgumentException("El archivo no es un SVG válido");
		}
		limpiar(raiz);

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		ByteArrayOutputStream salida = new ByteArrayOutputStream();
		transformer.transform(new DOMSource(documento), new StreamResult(salida));
		return new Resultado(salida.toByteArray(), dimension(raiz, "width", 2), dimension(raiz, "height", 3));
	}

	private static void limpiar(Element elemento) {
		NamedNodeMap atributos = elemento.getAttributes();
		List<Attr> retirar = new ArrayList<>();
		for (int i = 0; i < atributos.getLength(); i++) {
			Attr atributo = (Attr) atributos.item(i);
			String nombre = atributo.getName().toLowerCase(Locale.ROOT);
			String valor = atributo.getValue().strip().toLowerCase(Locale.ROOT);
			boolean permitido = ATRIBUTOS.contains(nombre) && !valor.contains("javascript:")
					&& !valor.contains("data:") && (!valor.contains("url(") || valor.matches(".*url\\(\\s*#[^)]*\\).*"));
			if ((nombre.equals("href") || nombre.equals("xlink:href")) && !valor.startsWith("#")) {
				permitido = false;
			}
			if (!permitido) {
				retirar.add(atributo);
			}
		}
		retirar.forEach(elemento::removeAttributeNode);

		NodeList hijos = elemento.getChildNodes();
		for (int i = hijos.getLength() - 1; i >= 0; i--) {
			Node hijo = hijos.item(i);
			if (hijo.getNodeType() == Node.ELEMENT_NODE) {
				Element hijoElemento = (Element) hijo;
				String nombre = (hijoElemento.getLocalName() != null) ? hijoElemento.getLocalName() : hijo.getNodeName();
				if (!SVG_NS.equals(hijoElemento.getNamespaceURI())
						|| !ELEMENTOS.contains(nombre.toLowerCase(Locale.ROOT))) {
					elemento.removeChild(hijo);
				}
				else {
					limpiar(hijoElemento);
				}
			}
			else if (hijo.getNodeType() != Node.TEXT_NODE) {
				// Comentarios, instrucciones de proceso y CDATA se descartan.
				elemento.removeChild(hijo);
			}
		}
	}

	/** Dimensión declarada o, en su defecto, la del {@code viewBox}. */
	private static Integer dimension(Element raiz, String atributo, int indiceViewBox) {
		String valor = raiz.getAttribute(atributo).replaceAll("[^0-9.]", "");
		try {
			if (!valor.isEmpty()) {
				return (int) Math.round(Double.parseDouble(valor));
			}
			String[] viewBox = raiz.getAttribute("viewBox").trim().split("[\\s,]+");
			if (viewBox.length == 4) {
				return (int) Math.round(Double.parseDouble(viewBox[indiceViewBox]));
			}
		}
		catch (NumberFormatException ex) {
			// Sin dimensiones utilizables.
		}
		return null;
	}

	record Resultado(byte[] contenido, Integer ancho, Integer alto) {
	}

}
