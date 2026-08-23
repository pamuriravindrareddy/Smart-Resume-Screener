package com.resume.screener.dto;

import java.time.LocalDateTime;

public record JobDescriptionResponseDto(
    Long id,
    String title,
    String description,
    LocalDateTime createdAt
) {}
