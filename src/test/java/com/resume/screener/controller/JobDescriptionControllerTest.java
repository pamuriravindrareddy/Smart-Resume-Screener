package com.resume.screener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.screener.dto.JobDescriptionRequestDto;
import com.resume.screener.dto.JobDescriptionResponseDto;
import com.resume.screener.service.JobDescriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(JobDescriptionController.class)
public class JobDescriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JobDescriptionService jobDescriptionService;

    @Test
    public void testCreateJobDescription_Success() throws Exception {
        JobDescriptionRequestDto request = new JobDescriptionRequestDto("Software Engineer", "Java, Spring Boot");
        JobDescriptionResponseDto response = new JobDescriptionResponseDto(1L, "Software Engineer", "Java, Spring Boot", LocalDateTime.now());

        when(jobDescriptionService.createJobDescription(any(JobDescriptionRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Software Engineer"));
    }

    @Test
    public void testCreateJobDescription_BlankTitle() throws Exception {
        JobDescriptionRequestDto request = new JobDescriptionRequestDto("   ", "Java, Spring Boot");

        mockMvc.perform(post("/api/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Job title must not be blank")));
    }

    @Test
    public void testCreateJobDescription_TitleTooLong() throws Exception {
        String longTitle = "a".repeat(256);
        JobDescriptionRequestDto request = new JobDescriptionRequestDto(longTitle, "Java, Spring Boot");

        mockMvc.perform(post("/api/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Job title must not exceed 255 characters")));
    }

    @Test
    public void testCreateJobDescription_BlankDescription() throws Exception {
        JobDescriptionRequestDto request = new JobDescriptionRequestDto("Software Engineer", "\t\n  ");

        mockMvc.perform(post("/api/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Job description must not be blank")));
    }
}
