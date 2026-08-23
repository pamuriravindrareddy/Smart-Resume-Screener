package com.resume.screener.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CandidateResponseDto(
    Long id,
    String name,
    String email,
    String phone,
    List<String> skills,
    String experience,
    String education,
    LocalDateTime createdAt
) {}
