package com.resume.screener.service;

import com.resume.screener.dto.JobDescriptionRequestDto;
import com.resume.screener.dto.JobDescriptionResponseDto;
import com.resume.screener.entity.JobDescription;
import com.resume.screener.exception.ResourceNotFoundException;
import com.resume.screener.repository.JobDescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class JobDescriptionServiceTest {

    @Mock
    private JobDescriptionRepository jobDescriptionRepository;

    @InjectMocks
    private JobDescriptionService jobDescriptionService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCreateJobDescription_Success() {
        JobDescriptionRequestDto requestDto = new JobDescriptionRequestDto("Software Engineer", "Java, Spring Boot");
        JobDescription job = new JobDescription();
        job.setId(1L);
        job.setTitle(requestDto.title());
        job.setDescription(requestDto.description());
        job.setCreatedAt(LocalDateTime.now());

        when(jobDescriptionRepository.save(any(JobDescription.class))).thenReturn(job);

        JobDescriptionResponseDto response = jobDescriptionService.createJobDescription(requestDto);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Software Engineer", response.title());
        assertEquals("Java, Spring Boot", response.description());
        verify(jobDescriptionRepository, times(1)).save(any(JobDescription.class));
    }

    @Test
    public void testGetJobDescriptionById_Success() {
        JobDescription job = new JobDescription();
        job.setId(1L);
        job.setTitle("Software Engineer");
        job.setDescription("Java, Spring Boot");
        job.setCreatedAt(LocalDateTime.now());

        when(jobDescriptionRepository.findById(1L)).thenReturn(Optional.of(job));

        JobDescriptionResponseDto response = jobDescriptionService.getJobDescriptionById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Software Engineer", response.title());
        verify(jobDescriptionRepository, times(1)).findById(1L);
    }

    @Test
    public void testGetJobDescriptionById_NotFound() {
        when(jobDescriptionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> jobDescriptionService.getJobDescriptionById(1L));
        verify(jobDescriptionRepository, times(1)).findById(1L);
    }

    @Test
    public void testGetAllJobDescriptions_Success() {
        JobDescription job1 = new JobDescription();
        job1.setId(1L);
        job1.setTitle("Software Engineer");
        job1.setDescription("Java, Spring Boot");

        JobDescription job2 = new JobDescription();
        job2.setId(2L);
        job2.setTitle("QA Engineer");
        job2.setDescription("Selenium");

        when(jobDescriptionRepository.findAll()).thenReturn(java.util.List.of(job1, job2));

        java.util.List<JobDescriptionResponseDto> response = jobDescriptionService.getAllJobDescriptions();

        assertNotNull(response);
        assertEquals(2, response.size());
        assertEquals("Software Engineer", response.get(0).title());
        assertEquals("QA Engineer", response.get(1).title());
        verify(jobDescriptionRepository, times(1)).findAll();
    }
}
