package com.transportes.api.storage;

import com.transportes.api.common.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

/**
 * Guarda los archivos en una carpeta del disco. En la base solo se guarda el nombre generado.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final Path root;

    public FileStorageService(@Value("${app.storage.dir}") String directory) {
        this.root = Paths.get(directory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear la carpeta de archivos: " + root, e);
        }
    }

    public String storePdf(MultipartFile file) {
        validate(file, "pdf", "application/pdf");
        return save(file, "pdf");
    }

    public String storeImage(MultipartFile file) {
        String extension = extensionOf(file);
        if ("png".equals(extension)) {
            validate(file, "png", "image/png");
        } else if ("jpg".equals(extension) || "jpeg".equals(extension)) {
            validate(file, extension, "image/jpeg");
        } else {
            throw new BadRequestException("La imagen debe ser .png o .jpg");
        }
        return save(file, extension);
    }

    /**
     * Borra un archivo guardado antes. Si no existe o falla, solo se deja un aviso en el log.
     */
    public void delete(String storedName) {
        if (storedName == null || storedName.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(root.resolve(storedName).normalize());
        } catch (IOException e) {
            log.warn("No se pudo borrar el archivo {}: {}", storedName, e.getMessage());
        }
    }

    private void validate(MultipartFile file, String expectedExtension, String expectedContentType) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("El archivo está vacío o no se envió");
        }

        String extension = extensionOf(file);
        if (!expectedExtension.equals(extension)) {
            throw new BadRequestException("Tipo de archivo no permitido. Se esperaba un archivo ." + expectedExtension);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.equalsIgnoreCase(expectedContentType)) {
            throw new BadRequestException("El contenido del archivo debe ser " + expectedContentType);
        }
    }

    private String extensionOf(MultipartFile file) {
        if (file == null || file.getOriginalFilename() == null) {
            return "";
        }
        String name = StringUtils.cleanPath(file.getOriginalFilename());
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String save(MultipartFile file, String extension) {
        String storedName = UUID.randomUUID() + "." + extension;
        Path target = root.resolve(storedName);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Error guardando el archivo {}", storedName, e);
            throw new IllegalStateException("No se pudo guardar el archivo", e);
        }

        log.atInfo()
                .addKeyValue("file", storedName)
                .addKeyValue("sizeBytes", file.getSize())
                .log("file stored");
        return storedName;
    }
}
