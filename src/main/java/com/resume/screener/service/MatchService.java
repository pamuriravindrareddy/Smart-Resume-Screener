package com.resume.screener.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MatchService {

    private final MatchResultRepository matchResultRepository;
    private final CandidateRepository candidateRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    public MatchService(MatchResultRepository matchResultRepository,
                        CandidateRepository candidateRepository,
                        JobDescriptionRepository jobDescriptionRepository,
                        LlmService llmService) {
        this.matchResultRepository = matchResultRepository;
        this.candidateRepository = candidateRepository;
        this.jobDescriptionRepository = jobDescriptionRepository;
        this.llmService = llmService;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public MatchResponseDto matchAndSave(MatchRequestDto requestDto) {
        Candidate candidate = candidateRepository.findById(requestDto.candidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with ID: " + requestDto.candidateId()));

        JobDescription jobDescription = jobDescriptionRepository.findById(requestDto.jobDescriptionId())
                .orElseThrow(() -> new ResourceNotFoundException("Job description not found with ID: " + requestDto.jobDescriptionId()));

        // Run LLM semantic matching
        LlmMatchResponse llmResponse = llmService.matchCandidateWithJob(candidate, jobDescription);

        // Serialize skills arrays into JSON text
        String matchedSkillsJson;
        String missingSkillsJson;
        try {
            matchedSkillsJson = objectMapper.writeValueAsString(llmResponse.matchedSkills());
            missingSkillsJson = objectMapper.writeValueAsString(llmResponse.missingSkills());
        } catch (Exception e) {
            throw new LlmException("Failed to serialize matched/missing skills to JSON text: " + e.getMessage(), e);
        }

        // Save MatchResult
        MatchResult matchResult = new MatchResult();
        matchResult.setCandidate(candidate);
        matchResult.setJobDescription(jobDescription);
        matchResult.setScore(llmResponse.score());
        matchResult.setDecision(llmResponse.decision());
        matchResult.setMatchedSkills(matchedSkillsJson);
        matchResult.setMissingSkills(missingSkillsJson);
        matchResult.setJustification(llmResponse.justification());

        MatchResult savedResult = matchResultRepository.save(matchResult);

        return mapToDto(savedResult);
    }

    @Transactional(readOnly = true)
    public MatchResponseDto getMatchById(Long id) {
        MatchResult matchResult = matchResultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match result not found with ID: " + id));
        return mapToDto(matchResult);
    }

    private MatchResponseDto mapToDto(MatchResult match) {
        List<String> matchedSkillsList = List.of();
        List<String> missingSkillsList = List.of();
        try {
            if (match.getMatchedSkills() != null) {
                matchedSkillsList = objectMapper.readValue(match.getMatchedSkills(), new TypeReference<List<String>>() {});
            }
            if (match.getMissingSkills() != null) {
                missingSkillsList = objectMapper.readValue(match.getMissingSkills(), new TypeReference<List<String>>() {});
            }
        } catch (Exception e) {
            // Fallback in case of mapping error
            matchedSkillsList = List.of();
            missingSkillsList = List.of();
        }

        return new MatchResponseDto(
                match.getId(),
                match.getCandidate().getId(),
                match.getJobDescription().getId(),
                match.getScore(),
                match.getDecision(),
                matchedSkillsList,
                missingSkillsList,
                match.getJustification(),
                match.getCreatedAt()
        );
    }
}
