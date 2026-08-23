package com.resume.screener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JobDescriptionRequestDto(
    @NotBlank(message = "Job title must not be blank")
    @Size(max = 255, message = "Job title must not exceed 255 characters")
    String title,

    @NotBlank(message = "Job description must not be blank")
    String description
) {}
