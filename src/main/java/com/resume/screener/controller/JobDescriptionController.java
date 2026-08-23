package com.resume.screener.controller;

import com.resume.screener.dto.JobDescriptionRequestDto;
import com.resume.screener.dto.JobDescriptionResponseDto;
import com.resume.screener.service.JobDescriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
public class JobDescriptionController {

    private final JobDescriptionService jobDescriptionService;

    public JobDescriptionController(JobDescriptionService jobDescriptionService) {
        this.jobDescriptionService = jobDescriptionService;
    }

    @PostMapping
    public ResponseEntity<JobDescriptionResponseDto> createJobDescription(@Valid @RequestBody JobDescriptionRequestDto requestDto) {
        JobDescriptionResponseDto response = jobDescriptionService.createJobDescription(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobDescriptionResponseDto> getJobDescriptionById(@PathVariable("id") Long id) {
        JobDescriptionResponseDto response = jobDescriptionService.getJobDescriptionById(id);
        return ResponseEntity.ok(response);
    }
}
