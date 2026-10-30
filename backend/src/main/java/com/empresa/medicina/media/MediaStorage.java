package com.empresa.medicina.media;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import com.empresa.medicina.config.AppProperties;

import org.springframework.stereotype.Component;

/**
 * Almacenamiento de archivos en disco, fuera de la base de datos y de cualquier directorio servido. Las claves
 * son rutas generadas por la plataforma; nunca se usa el nombre del archivo del cliente. Puede sustituirse por un
 * almacenamiento de objetos implementando las mismas operaciones.
 */
@Component
class MediaStorage {

	private final Path raiz;

	MediaStorage(AppProperties properties) {
		this.raiz = Path.of(properties.media().storageDir()).toAbsolutePath().normalize();
	}

	String guardar(UUID activoId, String variante, String extension, byte[] contenido) {
		String clave = activoId + "/" + variante + "." + extension;
		Path destino = resolver(clave);
		try {
			Files.createDirectories(destino.getParent());
			Files.write(destino, contenido, StandardOpenOption.CREATE_NEW);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("No se pudo guardar el archivo", ex);
		}
		return clave;
	}

	byte[] leer(String clave) {
		try {
			return Files.readAllBytes(resolver(clave));
		}
		catch (IOException ex) {
			throw new UncheckedIOException("No se pudo leer el archivo", ex);
		}
	}

	/** Impide salir del directorio raíz mediante rutas manipuladas. */
	private Path resolver(String clave) {
		Path ruta = this.raiz.resolve(clave).normalize();
		if (!ruta.startsWith(this.raiz)) {
			throw new IllegalArgumentException("Ruta de almacenamiento no válida");
		}
		return ruta;
	}

}
