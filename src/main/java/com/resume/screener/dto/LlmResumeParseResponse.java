package com.resume.screener.dto;

import java.util.List;

public record LlmResumeParseResponse(
    String name,
    String email,
    String phone,
    List<String> skills,
    String experience,
    String education
) {}
