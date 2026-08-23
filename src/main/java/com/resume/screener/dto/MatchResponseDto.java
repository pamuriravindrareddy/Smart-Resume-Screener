package com.resume.screener.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MatchResponseDto(
    Long id,
    Long candidateId,
    Long jobDescriptionId,
    int score,
    String decision,
    List<String> matchedSkills,
    List<String> missingSkills,
    String justification,
    LocalDateTime createdAt
) {}
