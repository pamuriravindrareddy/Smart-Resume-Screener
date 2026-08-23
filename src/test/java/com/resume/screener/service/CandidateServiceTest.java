package com.resume.screener.service;

import com.resume.screener.dto.CandidateResponseDto;
import com.resume.screener.dto.LlmResumeParseResponse;
import com.resume.screener.entity.Candidate;
import com.resume.screener.exception.EmptyFileException;
import com.resume.screener.exception.LlmException;
import com.resume.screener.exception.ResourceNotFoundException;
import com.resume.screener.exception.UnsupportedFileTypeException;
import com.resume.screener.repository.CandidateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CandidateServiceTest {

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private PdfService pdfService;

    @Mock
    private TxtService txtService;

    @Mock
    private LlmService llmService;

    @InjectMocks
    private CandidateService candidateService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testUploadAndParseResume_PdfSuccess() {
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", "pdfcontent".getBytes());
        LlmResumeParseResponse llmResponse = new LlmResumeParseResponse("John Doe", "john@example.com", "1234567890", List.of("Java"), "Exp", "Edu");
        
        Candidate candidate = new Candidate();
        candidate.setId(1L);
        candidate.setName("John Doe");
        candidate.setEmail("john@example.com");
        candidate.setPhone("1234567890");
        candidate.setSkills("Java");
        candidate.setExperience("Exp");
        candidate.setEducation("Edu");
        candidate.setCreatedAt(LocalDateTime.now());

        when(pdfService.extractText(file)).thenReturn("extracted raw text");
        when(llmService.parseResume("extracted raw text")).thenReturn(llmResponse);
        when(candidateRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());
        when(candidateRepository.save(any(Candidate.class))).thenReturn(candidate);

        CandidateResponseDto result = candidateService.uploadAndParseResume(file);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("John Doe", result.name());
        assertEquals(List.of("Java"), result.skills());
        verify(pdfService, times(1)).extractText(file);
        verify(llmService, times(1)).parseResume("extracted raw text");
        verify(candidateRepository, times(1)).save(any(Candidate.class));
    }

    @Test
    public void testUploadAndParseResume_TxtSuccess() {
        MockMultipartFile file = new MockMultipartFile("file", "resume.txt", "text/plain", "txtcontent".getBytes());
        LlmResumeParseResponse llmResponse = new LlmResumeParseResponse("Jane Doe", "jane@example.com", "0987654321", List.of("Spring"), "Exp", "Edu");
        
        Candidate candidate = new Candidate();
        candidate.setId(2L);
        candidate.setName("Jane Doe");
        candidate.setEmail("jane.doe@example.com");
        candidate.setPhone("0987654321");
        candidate.setSkills("Spring");
        candidate.setExperience("Exp");
        candidate.setEducation("Edu");

        when(txtService.extractText(file)).thenReturn("extracted text");
        when(llmService.parseResume("extracted text")).thenReturn(llmResponse);
        when(candidateRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());
        when(candidateRepository.save(any(Candidate.class))).thenReturn(candidate);

        CandidateResponseDto result = candidateService.uploadAndParseResume(file);

        assertNotNull(result);
        assertEquals(2L, result.id());
        assertEquals("Jane Doe", result.name());
        verify(txtService, times(1)).extractText(file);
        verify(candidateRepository, times(1)).save(any(Candidate.class));
    }

    @Test
    public void testUploadAndParseResume_EmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[0]);
        assertThrows(EmptyFileException.class, () -> candidateService.uploadAndParseResume(file));
    }

    @Test
    public void testUploadAndParseResume_UnsupportedFileType() {
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", "pngcontent".getBytes());
        assertThrows(UnsupportedFileTypeException.class, () -> candidateService.uploadAndParseResume(file));
    }

    @Test
    public void testUploadAndParseResume_LlmFailure() {
        MockMultipartFile file = new MockMultipartFile("file", "resume.txt", "text/plain", "txtcontent".getBytes());
        when(txtService.extractText(file)).thenReturn("raw text");
        when(llmService.parseResume("raw text")).thenThrow(new LlmException("LLM down"));

        assertThrows(LlmException.class, () -> candidateService.uploadAndParseResume(file));
        verify(candidateRepository, never()).save(any(Candidate.class));
    }

    @Test
    public void testUploadAndParseResume_UpsertBehavior() {
        MockMultipartFile file = new MockMultipartFile("file", "resume.txt", "text/plain", "txtcontent".getBytes());
        LlmResumeParseResponse llmResponse = new LlmResumeParseResponse("Jane Updated", "jane@example.com", "11111", List.of("Spring"), "New Exp", "New Edu");
        
        Candidate existing = new Candidate();
        existing.setId(5L);
        existing.setName("Jane Old");
        existing.setEmail("jane@example.com");
        existing.setSkills("Java");

        when(txtService.extractText(file)).thenReturn("raw text");
        when(llmService.parseResume("raw text")).thenReturn(llmResponse);
        when(candidateRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(existing));
        when(candidateRepository.save(existing)).thenReturn(existing);

        CandidateResponseDto result = candidateService.uploadAndParseResume(file);

        assertNotNull(result);
        assertEquals(5L, result.id());
        assertEquals("Jane Updated", existing.getName());
        assertEquals("New Exp", existing.getExperience());
        verify(candidateRepository, times(1)).save(existing);
    }

    @Test
    public void testGetCandidateById_NotFound() {
        when(candidateRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> candidateService.getCandidateById(1L));
    }
}
