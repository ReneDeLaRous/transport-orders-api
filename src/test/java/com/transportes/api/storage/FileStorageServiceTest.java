package com.transportes.api.storage;

import com.transportes.api.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService storage;

    @BeforeEach
    void setUp() {
        storage = new FileStorageService(tempDir.toString());
    }

    @Test
    void storesAValidPdf() {
        MockMultipartFile pdf = new MockMultipartFile("file", "Contrato.PDF", "application/pdf", "%PDF-1.4".getBytes());

        String name = storage.storePdf(pdf);

        assertTrue(name.endsWith(".pdf"));
        assertTrue(Files.exists(tempDir.resolve(name)));
    }

    @Test
    void storesPngAndJpgImages() {
        MockMultipartFile png = new MockMultipartFile("file", "foto.png", "image/png", new byte[]{1, 2, 3});
        MockMultipartFile jpg = new MockMultipartFile("file", "foto.jpg", "image/jpeg", new byte[]{1, 2, 3});
        MockMultipartFile jpeg = new MockMultipartFile("file", "foto.jpeg", "image/jpeg", new byte[]{1, 2, 3});

        String pngName = storage.storeImage(png);
        String jpgName = storage.storeImage(jpg);
        String jpegName = storage.storeImage(jpeg);

        assertTrue(pngName.endsWith(".png"));
        assertTrue(jpgName.endsWith(".jpg"));
        assertTrue(jpegName.endsWith(".jpeg"));
        assertTrue(Files.exists(tempDir.resolve(pngName)));
    }

    @Test
    void rejectsPdfWithWrongExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "notas.txt", "application/pdf", "hola".getBytes());

        try {
            storage.storePdf(file);
            fail("Se esperaba BadRequestException");
        } catch (BadRequestException e) {
            assertTrue(e.getMessage().contains("pdf"));
        }
    }

    @Test
    void rejectsPdfWithWrongContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "guia.pdf", "text/plain", "hola".getBytes());

        try {
            storage.storePdf(file);
            fail("Se esperaba BadRequestException");
        } catch (BadRequestException e) {
            assertTrue(e.getMessage().contains("application/pdf"));
        }
    }

    @Test
    void rejectsEmptyFiles() {
        MockMultipartFile empty = new MockMultipartFile("file", "guia.pdf", "application/pdf", new byte[0]);

        try {
            storage.storePdf(empty);
            fail("Se esperaba BadRequestException");
        } catch (BadRequestException e) {
            assertTrue(e.getMessage().contains("vacío"));
        }
    }

    @Test
    void rejectsUnsupportedImageFormats() {
        MockMultipartFile gif = new MockMultipartFile("file", "animacion.gif", "image/gif", new byte[]{1, 2, 3});

        try {
            storage.storeImage(gif);
            fail("Se esperaba BadRequestException");
        } catch (BadRequestException e) {
            assertTrue(e.getMessage().contains(".png o .jpg"));
        }
    }

    @Test
    void rejectsImageWhoseContentTypeDoesNotMatchTheExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "foto.png", "image/jpeg", new byte[]{1, 2, 3});

        try {
            storage.storeImage(file);
            fail("Se esperaba BadRequestException");
        } catch (BadRequestException e) {
            assertTrue(e.getMessage().contains("image/png"));
        }
    }

    @Test
    void deleteRemovesTheStoredFileAndIgnoresMissingOnes() throws IOException {
        MockMultipartFile pdf = new MockMultipartFile("file", "guia.pdf", "application/pdf", "%PDF-1.4".getBytes());
        String name = storage.storePdf(pdf);

        storage.delete(name);
        storage.delete("no-existe.pdf");
        storage.delete(null);

        assertFalse(Files.exists(tempDir.resolve(name)));
    }
}
