package com.resume.screener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.screener.dto.MatchRequestDto;
import com.resume.screener.dto.MatchResponseDto;
import com.resume.screener.service.MatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(MatchController.class)
public class MatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MatchService matchService;

    @Test
    public void testRunMatch_Success() throws Exception {
        MatchRequestDto request = new MatchRequestDto(1L, 2L);
        MatchResponseDto response = new MatchResponseDto(10L, 1L, 2L, 8, "SHORTLIST", List.of("Java"), List.of("Docker"), "Justification", LocalDateTime.now());

        when(matchService.matchAndSave(any(MatchRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/matches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.candidateId").value(1L))
                .andExpect(jsonPath("$.jobDescriptionId").value(2L))
                .andExpect(jsonPath("$.score").value(8))
                .andExpect(jsonPath("$.decision").value("SHORTLIST"));
    }

    @Test
    public void testRunMatch_NullCandidateId() throws Exception {
        MatchRequestDto request = new MatchRequestDto(null, 2L);

        mockMvc.perform(post("/api/matches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Candidate ID must not be null")));
    }

    @Test
    public void testRunMatch_NullJobDescriptionId() throws Exception {
        MatchRequestDto request = new MatchRequestDto(1L, null);

        mockMvc.perform(post("/api/matches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Job Description ID must not be null")));
    }
}
