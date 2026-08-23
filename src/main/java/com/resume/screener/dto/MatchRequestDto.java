package com.resume.screener.dto;

import jakarta.validation.constraints.NotNull;

public record MatchRequestDto(
    @NotNull(message = "Candidate ID must not be null")
    Long candidateId,

    @NotNull(message = "Job Description ID must not be null")
    Long jobDescriptionId
) {}
