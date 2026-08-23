package com.resume.screener.service;

import com.resume.screener.dto.LlmMatchResponse;
import com.resume.screener.dto.MatchRequestDto;
import com.resume.screener.dto.MatchResponseDto;
import com.resume.screener.entity.Candidate;
import com.resume.screener.entity.JobDescription;
import com.resume.screener.entity.MatchResult;
import com.resume.screener.exception.LlmException;
import com.resume.screener.exception.ResourceNotFoundException;
import com.resume.screener.repository.CandidateRepository;
import com.resume.screener.repository.JobDescriptionRepository;
import com.resume.screener.repository.MatchResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class MatchServiceTest {

    @Mock
    private MatchResultRepository matchResultRepository;

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private JobDescriptionRepository jobDescriptionRepository;

    @Mock
    private LlmService llmService;

    @InjectMocks
    private MatchService matchService;

    private Candidate candidate;
    private JobDescription jobDescription;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        candidate = new Candidate();
        candidate.setId(1L);
        candidate.setName("Jane Doe");
        candidate.setEmail("jane.doe@example.com");
        candidate.setSkills("Java, Spring Boot");

        jobDescription = new JobDescription();
        jobDescription.setId(2L);
        jobDescription.setTitle("Java Developer");
        jobDescription.setDescription("Requires Java and Spring Boot.");
    }

    @Test
    public void testMatchAndSave_Success() {
        MatchRequestDto requestDto = new MatchRequestDto(1L, 2L);
        LlmMatchResponse llmResponse = new LlmMatchResponse(8, "SHORTLIST", List.of("Java", "Spring Boot"), List.of("Docker"), "Great match.");

        MatchResult matchResult = new MatchResult();
        matchResult.setId(10L);
        matchResult.setCandidate(candidate);
        matchResult.setJobDescription(jobDescription);
        matchResult.setScore(8);
        matchResult.setDecision("SHORTLIST");
        matchResult.setMatchedSkills("[\"Java\",\"Spring Boot\"]");
        matchResult.setMissingSkills("[\"Docker\"]");
        matchResult.setJustification("Great match.");
        matchResult.setCreatedAt(LocalDateTime.now());

        when(candidateRepository.findById(1L)).thenReturn(Optional.of(candidate));
        when(jobDescriptionRepository.findById(2L)).thenReturn(Optional.of(jobDescription));
        when(llmService.matchCandidateWithJob(candidate, jobDescription)).thenReturn(llmResponse);
        when(matchResultRepository.save(any(MatchResult.class))).thenReturn(matchResult);

        MatchResponseDto responseDto = matchService.matchAndSave(requestDto);

        assertNotNull(responseDto);
        assertEquals(10L, responseDto.id());
        assertEquals(1L, responseDto.candidateId());
        assertEquals(2L, responseDto.jobDescriptionId());
        assertEquals(8, responseDto.score());
        assertEquals("SHORTLIST", responseDto.decision());
        assertEquals(List.of("Java", "Spring Boot"), responseDto.matchedSkills());
        assertEquals(List.of("Docker"), responseDto.missingSkills());
        assertEquals("Great match.", responseDto.justification());

        verify(matchResultRepository, times(1)).save(any(MatchResult.class));
    }

    @Test
    public void testMatchAndSave_CandidateNotFound() {
        MatchRequestDto requestDto = new MatchRequestDto(1L, 2L);
        when(candidateRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> matchService.matchAndSave(requestDto));
        verify(matchResultRepository, never()).save(any(MatchResult.class));
    }

    @Test
    public void testMatchAndSave_JobNotFound() {
        MatchRequestDto requestDto = new MatchRequestDto(1L, 2L);
        when(candidateRepository.findById(1L)).thenReturn(Optional.of(candidate));
        when(jobDescriptionRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> matchService.matchAndSave(requestDto));
        verify(matchResultRepository, never()).save(any(MatchResult.class));
    }

    @Test
    public void testMatchAndSave_LlmFailure() {
        MatchRequestDto requestDto = new MatchRequestDto(1L, 2L);
        when(candidateRepository.findById(1L)).thenReturn(Optional.of(candidate));
        when(jobDescriptionRepository.findById(2L)).thenReturn(Optional.of(jobDescription));
        when(llmService.matchCandidateWithJob(candidate, jobDescription)).thenThrow(new LlmException("LLM failure"));

        assertThrows(LlmException.class, () -> matchService.matchAndSave(requestDto));
        verify(matchResultRepository, never()).save(any(MatchResult.class));
    }
}
