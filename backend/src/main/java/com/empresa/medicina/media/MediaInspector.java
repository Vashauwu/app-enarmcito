package com.empresa.medicina.media;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.empresa.medicina.common.ApiException;
import com.empresa.medicina.config.AppProperties;

import org.springframework.stereotype.Component;

/**
 * Valida un archivo cargado: tipo real por firma de bytes, tamaño, dimensiones y contenido activo. Genera las
 * variantes optimizadas (que además eliminan metadatos EXIF, por ejemplo la ubicación GPS).
 */
@Component
class MediaInspector {

	static final int ANCHO_ESCRITORIO = 1280;

	static final int ANCHO_MOVIL = 640;

	/** Marcadores de contenido activo que no se admiten en un PDF. */
	private static final List<String> PDF_PROHIBIDO = List.of("/javascript", "/js ", "/js(", "/js<", "/launch",
			"/embeddedfile", "/richmedia", "/xfa");

	private final AppProperties.Media config;

	MediaInspector(AppProperties properties) {
		this.config = properties.media();
	}

	ArchivoInspeccionado inspeccionar(byte[] contenido) {
		if (contenido.length == 0) {
			throw invalido("El archivo está vacío.");
		}
		TipoArchivo tipo = detectar(contenido);
		long maximo = (tipo == TipoArchivo.PDF) ? this.config.maxPdfBytes() : this.config.maxImageBytes();
		if (contenido.length > maximo) {
			throw invalido("El archivo supera el tamaño máximo de " + (maximo / (1024 * 1024)) + " MB.");
		}
		return switch (tipo) {
			case JPEG, PNG -> mapa(tipo, contenido);
			case WEBP -> webp(contenido);
			case SVG -> svg(contenido);
			case PDF -> pdf(contenido);
		};
	}

	/** Tipo según la firma; la extensión y el Content-Type del cliente se ignoran. */
	static TipoArchivo detectar(byte[] b) {
		if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
			return TipoArchivo.JPEG;
		}
		if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G' && b[4] == 0x0D
				&& b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
			return TipoArchivo.PNG;
		}
		if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E'
				&& b[10] == 'B' && b[11] == 'P') {
			return TipoArchivo.WEBP;
		}
		if (b.length >= 5 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F' && b[4] == '-') {
			return TipoArchivo.PDF;
		}
		String inicio = new String(b, 0, Math.min(b.length, 1024), StandardCharsets.UTF_8).stripLeading()
			.toLowerCase(Locale.ROOT);
		if ((inicio.startsWith("<?xml") || inicio.startsWith("<svg")) && inicio.contains("<svg")) {
			return TipoArchivo.SVG;
		}
		throw invalido("Formato no admitido. Usa JPG, PNG, WebP, SVG o PDF.");
	}

	private ArchivoInspeccionado mapa(TipoArchivo tipo, byte[] contenido) {
		int[] dimensiones = dimensiones(contenido);
		validarDimensiones(dimensiones[0], dimensiones[1]);
		BufferedImage imagen;
		try {
			imagen = ImageIO.read(new ByteArrayInputStream(contenido));
		}
		catch (IOException ex) {
			imagen = null;
		}
		if (imagen == null) {
			throw invalido("La imagen está dañada o usa una codificación no admitida.");
		}
		String formato = (tipo == TipoArchivo.PNG) ? "png" : "jpg";
		return new ArchivoInspeccionado(tipo, contenido, sha256(contenido), imagen.getWidth(), imagen.getHeight(),
				variante(imagen, ANCHO_ESCRITORIO, formato), variante(imagen, ANCHO_MOVIL, formato));
	}

	private ArchivoInspeccionado webp(byte[] contenido) {
		int[] dimensiones = dimensionesWebp(contenido);
		validarDimensiones(dimensiones[0], dimensiones[1]);
		return new ArchivoInspeccionado(TipoArchivo.WEBP, contenido, sha256(contenido), dimensiones[0], dimensiones[1],
				null, null);
	}

	private ArchivoInspeccionado svg(byte[] contenido) {
		try {
			SvgSanitizer.Resultado saneado = SvgSanitizer.sanear(contenido);
			return new ArchivoInspeccionado(TipoArchivo.SVG, saneado.contenido(), sha256(saneado.contenido()),
					saneado.ancho(), saneado.alto(), null, null);
		}
		catch (Exception ex) {
			throw invalido("El SVG no es válido o incluye construcciones no permitidas (DTD o entidades).");
		}
	}

	private ArchivoInspeccionado pdf(byte[] contenido) {
		String texto = new String(contenido, StandardCharsets.ISO_8859_1).toLowerCase(Locale.ROOT);
		for (String marcador : PDF_PROHIBIDO) {
			if (texto.contains(marcador)) {
				throw invalido("El PDF contiene JavaScript, acciones automáticas o archivos incrustados.");
			}
		}
		if (!texto.contains("%%eof")) {
			throw invalido("El PDF está incompleto o dañado.");
		}
		return new ArchivoInspeccionado(TipoArchivo.PDF, contenido, sha256(contenido), null, null, null, null);
	}

	/** Lee sólo la cabecera para rechazar imágenes gigantes antes de decodificarlas. */
	private static int[] dimensiones(byte[] contenido) {
		try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(contenido))) {
			Iterator<ImageReader> lectores = ImageIO.getImageReaders(entrada);
			if (!lectores.hasNext()) {
				throw invalido("La imagen está dañada o usa una codificación no admitida.");
			}
			ImageReader lector = lectores.next();
			try {
				lector.setInput(entrada);
				return new int[] { lector.getWidth(0), lector.getHeight(0) };
			}
			finally {
				lector.dispose();
			}
		}
		catch (IOException ex) {
			throw invalido("La imagen está dañada o usa una codificación no admitida.");
		}
	}

	/** Dimensiones de un WebP (VP8, VP8L o VP8X) leídas de su cabecera. */
	static int[] dimensionesWebp(byte[] b) {
		if (b.length < 30) {
			throw invalido("La imagen WebP está dañada.");
		}
		String fragmento = new String(b, 12, 4, StandardCharsets.US_ASCII);
		switch (fragmento) {
			case "VP8X" -> {
				int ancho = 1 + ((b[24] & 0xFF) | ((b[25] & 0xFF) << 8) | ((b[26] & 0xFF) << 16));
				int alto = 1 + ((b[27] & 0xFF) | ((b[28] & 0xFF) << 8) | ((b[29] & 0xFF) << 16));
				return new int[] { ancho, alto };
			}
			case "VP8 " -> {
				int ancho = ((b[26] & 0xFF) | ((b[27] & 0xFF) << 8)) & 0x3FFF;
				int alto = ((b[28] & 0xFF) | ((b[29] & 0xFF) << 8)) & 0x3FFF;
				return new int[] { ancho, alto };
			}
			case "VP8L" -> {
				int bits = (b[21] & 0xFF) | ((b[22] & 0xFF) << 8) | ((b[23] & 0xFF) << 16) | ((b[24] & 0xFF) << 24);
				return new int[] { (bits & 0x3FFF) + 1, ((bits >> 14) & 0x3FFF) + 1 };
			}
			default -> throw invalido("La imagen WebP está dañada.");
		}
	}

	private void validarDimensiones(int ancho, int alto) {
		int maximo = this.config.maxImagePixels();
		if (ancho <= 0 || alto <= 0 || ancho > maximo || alto > maximo) {
			throw invalido("La imagen debe medir como máximo " + maximo + " × " + maximo + " píxeles.");
		}
	}

	/** Recodifica (sin metadatos) y reduce al ancho indicado si la imagen es más grande. */
	private static byte[] variante(BufferedImage original, int anchoMaximo, String formato) {
		int ancho = Math.min(anchoMaximo, original.getWidth());
		int alto = Math.max(1, (int) Math.round(original.getHeight() * (ancho / (double) original.getWidth())));
		boolean alfa = "png".equals(formato) && original.getColorModel().hasAlpha();
		BufferedImage destino = new BufferedImage(ancho, alto,
				alfa ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
		Graphics2D g = destino.createGraphics();
		try {
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			if (!alfa) {
				g.setColor(java.awt.Color.WHITE);
				g.fillRect(0, 0, ancho, alto);
			}
			g.drawImage(original, 0, 0, ancho, alto, null);
		}
		finally {
			g.dispose();
		}
		try {
			ByteArrayOutputStream salida = new ByteArrayOutputStream();
			ImageIO.write(destino, formato, salida);
			return salida.toByteArray();
		}
		catch (IOException ex) {
			throw new IllegalStateException("No se pudo generar la variante", ex);
		}
	}

	static String sha256(byte[] contenido) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static ApiException invalido(String mensaje) {
		return ApiException.badRequest("ARCHIVO_INVALIDO", mensaje);
	}

}
