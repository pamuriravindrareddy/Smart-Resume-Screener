package com.resume.screener.service;

import com.resume.screener.dto.CandidateResponseDto;
import com.resume.screener.dto.LlmResumeParseResponse;
import com.resume.screener.entity.Candidate;
import com.resume.screener.exception.EmptyFileException;
import com.resume.screener.exception.ResourceNotFoundException;
import com.resume.screener.exception.UnsupportedFileTypeException;
import com.resume.screener.repository.CandidateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final PdfService pdfService;
    private final TxtService txtService;
    private final LlmService llmService;

    public CandidateService(CandidateRepository candidateRepository, 
                            PdfService pdfService, 
                            TxtService txtService, 
                            LlmService llmService) {
        this.candidateRepository = candidateRepository;
        this.pdfService = pdfService;
        this.txtService = txtService;
        this.llmService = llmService;
    }

    @Transactional
    public CandidateResponseDto uploadAndParseResume(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new EmptyFileException("Please select a file to upload.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("Invalid file name.");
        }

        String contentType = file.getContentType();
        boolean isPdf = filename.toLowerCase().endsWith(".pdf") || "application/pdf".equals(contentType);
        boolean isTxt = filename.toLowerCase().endsWith(".txt") || "text/plain".equals(contentType);

        if (!isPdf && !isTxt) {
            throw new UnsupportedFileTypeException("Unsupported file type. Only PDF (.pdf) and Text (.txt) files are supported.");
        }

        // 1. Extract Text
        String extractedText;
        if (isPdf) {
            extractedText = pdfService.extractText(file);
        } else {
            extractedText = txtService.extractText(file);
        }

        // 2. Parse using LLM
        LlmResumeParseResponse parsedData = llmService.parseResume(extractedText);

        // 3. Save Candidate
        Candidate candidate = new Candidate();
        candidate.setName(parsedData.name());
        candidate.setEmail(parsedData.email());
        candidate.setPhone(parsedData.phone());
        candidate.setResumeText(extractedText);
        
        if (parsedData.skills() != null) {
            candidate.setSkills(String.join(", ", parsedData.skills()));
        }
        
        candidate.setExperience(parsedData.experience());
        candidate.setEducation(parsedData.education());

        // Check if candidate with same email already exists.
        // If so, update details instead of duplicating.
        Candidate savedCandidate = candidateRepository.findByEmail(parsedData.email())
                .map(existing -> {
                    existing.setName(parsedData.name());
                    existing.setPhone(parsedData.phone());
                    existing.setResumeText(extractedText);
                    if (parsedData.skills() != null) {
                        existing.setSkills(String.join(", ", parsedData.skills()));
                    }
                    existing.setExperience(parsedData.experience());
                    existing.setEducation(parsedData.education());
                    return candidateRepository.save(existing);
                })
                .orElseGet(() -> candidateRepository.save(candidate));

        // 4. Return DTO
        return mapToDto(savedCandidate);
    }

    @Transactional(readOnly = true)
    public List<CandidateResponseDto> getAllCandidates() {
        return candidateRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CandidateResponseDto getCandidateById(Long id) {
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with ID: " + id));
        return mapToDto(candidate);
    }

    private CandidateResponseDto mapToDto(Candidate candidate) {
        List<String> skillsList = List.of();
        if (candidate.getSkills() != null && !candidate.getSkills().trim().isEmpty()) {
            skillsList = Arrays.stream(candidate.getSkills().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        return new CandidateResponseDto(
                candidate.getId(),
                candidate.getName(),
                candidate.getEmail(),
                candidate.getPhone(),
                skillsList,
                candidate.getExperience(),
                candidate.getEducation(),
                candidate.getCreatedAt()
        );
    }
}
