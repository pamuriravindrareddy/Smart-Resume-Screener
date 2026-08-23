package com.resume.screener.service;

import com.resume.screener.exception.EmptyFileException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Service
public class TxtService {

    public String extractText(MultipartFile file) {
        if (file.isEmpty()) {
            throw new EmptyFileException("Uploaded text file is empty.");
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String text = reader.lines().collect(Collectors.joining("\n"));
            if (text.trim().isEmpty()) {
                throw new EmptyFileException("Uploaded text file contains no readable text.");
            }
            return text.trim();
        } catch (EmptyFileException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read text file: " + e.getMessage(), e);
        }
    }
}
