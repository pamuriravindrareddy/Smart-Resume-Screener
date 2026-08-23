package com.resume.screener.service;

import com.resume.screener.dto.JobDescriptionRequestDto;
import com.resume.screener.dto.JobDescriptionResponseDto;
import com.resume.screener.entity.JobDescription;
import com.resume.screener.exception.ResourceNotFoundException;
import com.resume.screener.repository.JobDescriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class JobDescriptionService {

    private final JobDescriptionRepository jobDescriptionRepository;

    public JobDescriptionService(JobDescriptionRepository jobDescriptionRepository) {
        this.jobDescriptionRepository = jobDescriptionRepository;
    }

    @Transactional
    public JobDescriptionResponseDto createJobDescription(JobDescriptionRequestDto requestDto) {
        JobDescription jobDescription = new JobDescription();
        jobDescription.setTitle(requestDto.title());
        jobDescription.setDescription(requestDto.description());

        JobDescription savedJob = jobDescriptionRepository.save(jobDescription);
        return mapToDto(savedJob);
    }

    @Transactional(readOnly = true)
    public JobDescriptionResponseDto getJobDescriptionById(Long id) {
        JobDescription jobDescription = jobDescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job description not found with ID: " + id));
        return mapToDto(jobDescription);
    }

    @Transactional(readOnly = true)
    public List<JobDescriptionResponseDto> getAllJobDescriptions() {
        return jobDescriptionRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private JobDescriptionResponseDto mapToDto(JobDescription job) {
        return new JobDescriptionResponseDto(
                job.getId(),
                job.getTitle(),
                job.getDescription(),
                job.getCreatedAt()
        );
    }
}
