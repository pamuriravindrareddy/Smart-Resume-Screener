package com.resume.screener.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.screener.dto.LlmResumeParseResponse;
import com.resume.screener.dto.LlmMatchResponse;
import com.resume.screener.entity.Candidate;
import com.resume.screener.entity.JobDescription;
import com.resume.screener.exception.LlmException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LlmService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openrouter.api.key}")
    private String apiKey;

    @Value("${openrouter.api.url}")
    private String apiUrl;

    @Value("${openrouter.model}")
    private String model;

    @Value("${openrouter.max-tokens:2048}")
    private Integer maxTokens;

    @Value("${openrouter.api.referer:http://localhost:8080}")
    private String referer;

    public LlmService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public LlmResumeParseResponse parseResume(String resumeText) {
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.startsWith("your_")) {
            throw new LlmException("OpenRouter API key is not configured. Please set the OPENROUTER_API_KEY environment variable.");
        }

        String systemPrompt = """
                You are an expert resume parsing system. Analyze the raw text of the candidate's resume and extract structured information.
                You must output a valid JSON object matching this schema exactly:
                {
                  "name": "Full Name (default 'Unknown' if not found)",
                  "email": "Email Address (default 'Unknown' if not found)",
                  "phone": "Phone Number (default 'Unknown' if not found)",
                  "skills": ["Skill1", "Skill2", ...],
                  "experience": "Synthesized summary of work experience and companies",
                  "education": "Synthesized summary of degree, institution, graduation year"
                }
                Rules:
                1. Extract only information actually present in the resume. Do not invent information.
                2. If a field is unavailable, use 'Unknown' or an empty array.
                3. Skills should be returned as a clean JSON array of strings.
                4. Return JSON only. Do NOT return Markdown code blocks (like ```json). Do NOT return explanations outside the JSON object.
                """;

        String userPrompt = "Resume Text:\n" + resumeText;

        try {
            // Build OpenRouter request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            if (maxTokens != null) {
                requestBody.put("max_tokens", maxTokens);
            }
            
            Map<String, Object> responseFormat = new HashMap<>();
            responseFormat.put("type", "json_object");
            requestBody.put("response_format", responseFormat);

            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));

            // Build Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("HTTP-Referer", referer);
            headers.set("X-Title", "Smart Resume Screener");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Send request
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(apiUrl, entity, String.class);
            String responseBody = responseEntity.getBody();

            if (responseBody == null || responseBody.trim().isEmpty()) {
                throw new LlmException("Received empty response from OpenRouter API.");
            }

            // Parse response body
            JsonNode rootNode = objectMapper.readTree(responseBody);
            
            // Handle error in OpenRouter response
            if (rootNode.has("error")) {
                String errorMsg = rootNode.get("error").get("message").asText();
                throw new LlmException("OpenRouter API returned error: " + errorMsg);
            }

            JsonNode choicesNode = rootNode.get("choices");
            if (choicesNode == null || !choicesNode.isArray() || choicesNode.isEmpty()) {
                throw new LlmException("Invalid response format from OpenRouter API (choices array missing or empty).");
            }

            String content = choicesNode.get(0).get("message").get("content").asText();
            if (content == null || content.trim().isEmpty()) {
                throw new LlmException("Received empty content inside OpenRouter completion response.");
            }

            // Clean LLM response
            content = sanitizeJson(content);

            // Parse content into LlmResumeParseResponse DTO
            LlmResumeParseResponse parseResponse = objectMapper.readValue(content, LlmResumeParseResponse.class);
            
            // Validate essential fields
            if (parseResponse.name() == null || parseResponse.email() == null) {
                throw new LlmException("LLM parsing result is missing required fields (name, email).");
            }

            return parseResponse;

        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("Failed to communicate with OpenRouter or parse LLM response: " + e.getMessage(), e);
        }
    }

    private String sanitizeJson(String content) {
        if (content == null) {
            throw new LlmException("LLM response content is null.");
        }
        int firstBrace = content.indexOf('{');
        int lastBrace = content.lastIndexOf('}');
        if (firstBrace == -1 || lastBrace == -1 || lastBrace < firstBrace) {
            throw new LlmException("Failed to locate a valid JSON object in LLM response.");
        }
        return content.substring(firstBrace, lastBrace + 1).trim();
    }

    public LlmMatchResponse matchCandidateWithJob(Candidate candidate, JobDescription jobDescription) {
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.startsWith("your_")) {
            throw new LlmException("OpenRouter API key is not configured. Please set the OPENROUTER_API_KEY environment variable.");
        }

        String systemPrompt = """
                You are an expert recruiter evaluating candidate suitability for a job.
                Analyze the candidate's skills, experience, and education, and compare them with the target Job Title and Job Description.
                You must output a valid JSON object matching this schema exactly:
                {
                  "score": <Integer from 1 to 10>,
                  "decision": "<Either 'SHORTLIST' or 'REJECT'>",
                  "matchedSkills": ["SkillA", "SkillB", ...],
                  "missingSkills": ["SkillC", "SkillD", ...],
                  "justification": "<A concise, evidence-based explanation of why this score and decision was chosen. Mention key strengths and critical missing skills.>"
                }
                Rules:
                1. Only use information actually present in the candidate profile and job description. Do not invent skills, experience, education, or requirements.
                2. The score must be strictly evidence-based from 1 to 10.
                3. matchedSkills must contain only skills actually supported by the candidate that match the job description.
                4. missingSkills must contain important job requirements/skills requested in the job description that are absent from the candidate.
                5. The justification must explicitly explain the main strengths and weaknesses.
                6. Return JSON only. Do NOT return Markdown code blocks (like ```json). Do NOT return explanations outside the JSON object.
                """;

        String userPrompt = String.format("""
                Candidate Profile:
                Name: %s
                Skills: %s
                Experience: %s
                Education: %s

                Job Requirements:
                Title: %s
                Description: %s
                """,
                candidate.getName(),
                candidate.getSkills(),
                candidate.getExperience(),
                candidate.getEducation(),
                jobDescription.getTitle(),
                jobDescription.getDescription()
        );

        try {
            // Build OpenRouter request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            if (maxTokens != null) {
                requestBody.put("max_tokens", maxTokens);
            }
            
            Map<String, Object> responseFormat = new HashMap<>();
            responseFormat.put("type", "json_object");
            requestBody.put("response_format", responseFormat);

            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));

            // Build Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("HTTP-Referer", referer);
            headers.set("X-Title", "Smart Resume Screener");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Send request
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(apiUrl, entity, String.class);
            String responseBody = responseEntity.getBody();

            if (responseBody == null || responseBody.trim().isEmpty()) {
                throw new LlmException("Received empty response from OpenRouter API.");
            }

            // Parse response body
            JsonNode rootNode = objectMapper.readTree(responseBody);
            
            // Handle error in OpenRouter response
            if (rootNode.has("error")) {
                String errorMsg = rootNode.get("error").get("message").asText();
                throw new LlmException("OpenRouter API returned error: " + errorMsg);
            }

            JsonNode choicesNode = rootNode.get("choices");
            if (choicesNode == null || !choicesNode.isArray() || choicesNode.isEmpty()) {
                throw new LlmException("Invalid response format from OpenRouter API (choices array missing or empty).");
            }

            String content = choicesNode.get(0).get("message").get("content").asText();
            if (content == null || content.trim().isEmpty()) {
                throw new LlmException("Received empty content inside OpenRouter completion response.");
            }

            // Clean LLM response
            content = sanitizeJson(content);

            // Parse content into LlmMatchResponse DTO
            LlmMatchResponse matchResponse = objectMapper.readValue(content, LlmMatchResponse.class);
            
            // Validate LLM response constraints
            validateMatchResponse(matchResponse);

            return matchResponse;

        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("Failed to communicate with OpenRouter or parse LLM match response: " + e.getMessage(), e);
        }
    }

    void validateMatchResponse(LlmMatchResponse response) {
        if (response == null) {
            throw new LlmException("LLM match response is null.");
        }
        if (response.score() == null || response.score() < 1 || response.score() > 10) {
            throw new LlmException("LLM match response validation failed: Score must be between 1 and 10 inclusive.");
        }
        if (response.decision() == null || (!"SHORTLIST".equals(response.decision()) && !"REJECT".equals(response.decision()))) {
            throw new LlmException("LLM match response validation failed: Decision must be exactly 'SHORTLIST' or 'REJECT'.");
        }
        if (response.matchedSkills() == null) {
            throw new LlmException("LLM match response validation failed: Matched skills list is missing.");
        }
        if (response.missingSkills() == null) {
            throw new LlmException("LLM match response validation failed: Missing skills list is missing.");
        }
        if (response.justification() == null || response.justification().trim().isEmpty()) {
            throw new LlmException("LLM match response validation failed: Justification must be present and non-empty.");
        }
    }
}
