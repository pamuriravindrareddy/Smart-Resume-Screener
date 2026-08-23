package com.resume.screener.controller;

import com.resume.screener.dto.CandidateResponseDto;
import com.resume.screener.service.CandidateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CandidateController {

    private final CandidateService candidateService;

    public CandidateController(CandidateService candidateService) {
        this.candidateService = candidateService;
    }

    @PostMapping(value = "/resumes/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CandidateResponseDto> uploadResume(@RequestParam("file") MultipartFile file) {
        CandidateResponseDto response = candidateService.uploadAndParseResume(file);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/candidates")
    public ResponseEntity<List<CandidateResponseDto>> getAllCandidates() {
        List<CandidateResponseDto> candidates = candidateService.getAllCandidates();
        return ResponseEntity.ok(candidates);
    }

    @GetMapping("/candidates/{id}")
    public ResponseEntity<CandidateResponseDto> getCandidateById(@PathVariable("id") Long id) {
        CandidateResponseDto candidate = candidateService.getCandidateById(id);
        return ResponseEntity.ok(candidate);
    }
}
