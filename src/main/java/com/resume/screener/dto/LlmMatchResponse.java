package com.resume.screener.dto;

import java.util.List;

public record LlmMatchResponse(
    Integer score,
    String decision,
    List<String> matchedSkills,
    List<String> missingSkills,
    String justification
) {}
