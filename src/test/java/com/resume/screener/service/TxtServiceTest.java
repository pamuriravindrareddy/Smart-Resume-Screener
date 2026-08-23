package com.resume.screener.service;

import com.resume.screener.exception.EmptyFileException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class TxtServiceTest {

    private final TxtService txtService = new TxtService();

    @Test
    public void testExtractText_Success() {
        String content = "Hello World\nJava Developer";
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.txt", "text/plain", content.getBytes(StandardCharsets.UTF_8));

        String extracted = txtService.extractText(file);
        assertEquals("Hello World\nJava Developer", extracted);
    }

    @Test
    public void testExtractText_EmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.txt", "text/plain", new byte[0]);

        assertThrows(EmptyFileException.class, () -> txtService.extractText(file));
    }

    @Test
    public void testExtractText_WhitespaceOnly() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.txt", "text/plain", "   \n  ".getBytes(StandardCharsets.UTF_8));

        assertThrows(EmptyFileException.class, () -> txtService.extractText(file));
    }
}
