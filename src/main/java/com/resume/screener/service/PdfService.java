package com.resume.screener.service;

import com.resume.screener.exception.PdfException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class PdfService {

    public String extractText(MultipartFile file) {
        if (file.isEmpty()) {
            throw new PdfException("Failed to extract text: Uploaded PDF file is empty.");
        }
        try {
            byte[] bytes = file.getBytes();
            try (PDDocument document = Loader.loadPDF(bytes)) {
                if (document.isEncrypted()) {
                    throw new PdfException("Failed to extract text: The PDF file is encrypted.");
                }
                
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);
                
                if (text == null || text.trim().isEmpty()) {
                    throw new PdfException("Failed to extract text: The PDF file contains no readable text.");
                }
                
                return text.trim();
            }
        } catch (IOException e) {
            throw new PdfException("Failed to read bytes from PDF file: " + e.getMessage(), e);
        } catch (PdfException e) {
            throw e;
        } catch (Exception e) {
            throw new PdfException("Corrupted or unreadable PDF file: " + e.getMessage(), e);
        }
    }
}
