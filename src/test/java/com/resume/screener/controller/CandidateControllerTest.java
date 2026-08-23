package com.resume.screener.controller;

import com.resume.screener.dto.CandidateResponseDto;
import com.resume.screener.exception.EmptyFileException;
import com.resume.screener.exception.ResourceNotFoundException;
import com.resume.screener.exception.UnsupportedFileTypeException;
import com.resume.screener.service.CandidateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CandidateController.class)
public class CandidateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CandidateService candidateService;

    @Test
    public void testUploadResume_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", "pdf bytes".getBytes());
        CandidateResponseDto response = new CandidateResponseDto(1L, "John Doe", "john@example.com", "1234567890", List.of("Java"), "Exp", "Edu", LocalDateTime.now());

        when(candidateService.uploadAndParseResume(any())).thenReturn(response);

        mockMvc.perform(multipart("/api/resumes/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    public void testUploadResume_EmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[0]);

        when(candidateService.uploadAndParseResume(any())).thenThrow(new EmptyFileException("Uploaded text file is empty."));

        mockMvc.perform(multipart("/api/resumes/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Uploaded text file is empty."));
    }

    @Test
    public void testUploadResume_UnsupportedFileType() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", "png bytes".getBytes());

        when(candidateService.uploadAndParseResume(any())).thenThrow(new UnsupportedFileTypeException("Unsupported file type. Only PDF (.pdf) and Text (.txt) files are supported."));

        mockMvc.perform(multipart("/api/resumes/upload").file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.message").value("Unsupported file type. Only PDF (.pdf) and Text (.txt) files are supported."));
    }

    @Test
    public void testGetAllCandidates_Success() throws Exception {
        CandidateResponseDto c1 = new CandidateResponseDto(1L, "John", "john@example.com", "111", List.of("Java"), "Exp", "Edu", LocalDateTime.now());
        CandidateResponseDto c2 = new CandidateResponseDto(2L, "Jane", "jane@example.com", "222", List.of("Python"), "Exp", "Edu", LocalDateTime.now());

        when(candidateService.getAllCandidates()).thenReturn(List.of(c1, c2));

        mockMvc.perform(get("/api/candidates")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].name").value("John"))
                .andExpect(jsonPath("$[1].name").value("Jane"));
    }

    @Test
    public void testGetCandidateById_Success() throws Exception {
        CandidateResponseDto c1 = new CandidateResponseDto(1L, "John", "john@example.com", "111", List.of("Java"), "Exp", "Edu", LocalDateTime.now());

        when(candidateService.getCandidateById(1L)).thenReturn(c1);

        mockMvc.perform(get("/api/candidates/1")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("John"));
    }

    @Test
    public void testGetCandidateById_NotFound() throws Exception {
        when(candidateService.getCandidateById(1L)).thenThrow(new ResourceNotFoundException("Candidate not found with ID: 1"));

        mockMvc.perform(get("/api/candidates/1")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Candidate not found with ID: 1"));
    }
}
