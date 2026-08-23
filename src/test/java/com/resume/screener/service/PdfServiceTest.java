package com.resume.screener.service;

import com.resume.screener.exception.PdfException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class PdfServiceTest {

    private final PdfService pdfService = new PdfService();

    @Test
    public void testExtractText_Success() throws IOException {
        byte[] pdfBytes;
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText("John Doe Resume");
                contentStream.endText();
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                document.save(baos);
                pdfBytes = baos.toByteArray();
            }
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", pdfBytes);

        String text = pdfService.extractText(file);
        assertEquals("John Doe Resume", text);
    }

    @Test
    public void testExtractText_EmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", new byte[0]);

        assertThrows(PdfException.class, () -> pdfService.extractText(file));
    }

    @Test
    public void testExtractText_CorruptedPdf() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "corrupted data".getBytes());

        assertThrows(PdfException.class, () -> pdfService.extractText(file));
    }
}
