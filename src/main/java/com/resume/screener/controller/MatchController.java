package com.resume.screener.controller;

import com.resume.screener.dto.MatchRequestDto;
import com.resume.screener.dto.MatchResponseDto;
import com.resume.screener.service.MatchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping
    public ResponseEntity<MatchResponseDto> runMatch(@Valid @RequestBody MatchRequestDto requestDto) {
        MatchResponseDto response = matchService.matchAndSave(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchResponseDto> getMatchById(@PathVariable("id") Long id) {
        MatchResponseDto response = matchService.getMatchById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<MatchResponseDto>> getFilteredMatches(
            @RequestParam(value = "candidateId", required = false) Long candidateId,
            @RequestParam(value = "jobDescriptionId", required = false) Long jobDescriptionId) {
        List<MatchResponseDto> response = matchService.getFilteredMatches(candidateId, jobDescriptionId);
        return ResponseEntity.ok(response);
    }
}
