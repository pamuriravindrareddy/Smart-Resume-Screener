package com.resume.screener.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.screener.dto.LlmMatchResponse;
import com.resume.screener.dto.LlmResumeParseResponse;
import com.resume.screener.entity.Candidate;
import com.resume.screener.entity.JobDescription;
import com.resume.screener.exception.LlmException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LlmServiceTest {

    private LlmService llmService;
    private RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        restTemplate = mock(RestTemplate.class);
        llmService = new LlmService(restTemplate);
        ReflectionTestUtils.setField(llmService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(llmService, "apiUrl", "https://api.test/completions");
        ReflectionTestUtils.setField(llmService, "model", "test-model");
        ReflectionTestUtils.setField(llmService, "referer", "http://test-referer");
    }

    @Test
    public void testValidateMatchResponse_Success() {
        LlmMatchResponse valid = new LlmMatchResponse(8, "SHORTLIST", List.of("Java"), List.of("Docker"), "Strong candidate.");
        assertDoesNotThrow(() -> llmService.validateMatchResponse(valid));
    }

    @Test
    public void testValidateMatchResponse_NullScore() {
        LlmMatchResponse invalid = new LlmMatchResponse(null, "SHORTLIST", List.of("Java"), List.of("Docker"), "Strong candidate.");
        assertThrows(LlmException.class, () -> llmService.validateMatchResponse(invalid));
    }

    @Test
    public void testValidateMatchResponse_ScoreTooLow() {
        LlmMatchResponse invalid = new LlmMatchResponse(0, "SHORTLIST", List.of("Java"), List.of("Docker"), "Strong candidate.");
        assertThrows(LlmException.class, () -> llmService.validateMatchResponse(invalid));
    }

    @Test
    public void testValidateMatchResponse_ScoreTooHigh() {
        LlmMatchResponse invalid = new LlmMatchResponse(11, "SHORTLIST", List.of("Java"), List.of("Docker"), "Strong candidate.");
        assertThrows(LlmException.class, () -> llmService.validateMatchResponse(invalid));
    }

    @Test
    public void testValidateMatchResponse_InvalidDecision() {
        LlmMatchResponse invalid = new LlmMatchResponse(7, "MAYBE", List.of("Java"), List.of("Docker"), "Strong candidate.");
        assertThrows(LlmException.class, () -> llmService.validateMatchResponse(invalid));
    }

    @Test
    public void testValidateMatchResponse_NullMatchedSkills() {
        LlmMatchResponse invalid = new LlmMatchResponse(7, "SHORTLIST", null, List.of("Docker"), "Strong candidate.");
        assertThrows(LlmException.class, () -> llmService.validateMatchResponse(invalid));
    }

    @Test
    public void testValidateMatchResponse_NullMissingSkills() {
        LlmMatchResponse invalid = new LlmMatchResponse(7, "SHORTLIST", List.of("Java"), null, "Strong candidate.");
        assertThrows(LlmException.class, () -> llmService.validateMatchResponse(invalid));
    }

    @Test
    public void testValidateMatchResponse_BlankJustification() {
        LlmMatchResponse invalid = new LlmMatchResponse(7, "SHORTLIST", List.of("Java"), List.of("Docker"), "   ");
        assertThrows(LlmException.class, () -> llmService.validateMatchResponse(invalid));
    }

    @Test
    public void testParseResume_Success() {
        String mockContent = "{\"name\": \"John Doe\", \"email\": \"john@example.com\", \"phone\": \"123\", \"skills\": [\"Java\"], \"experience\": \"Exp\", \"education\": \"Edu\"}";
        JsonNode responseNode = createMockEnvelope(mockContent);

        when(restTemplate.postForEntity(anyString(), any(), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(responseNode, HttpStatus.OK));

        LlmResumeParseResponse response = llmService.parseResume("Resume raw text");
        assertNotNull(response);
        assertEquals("John Doe", response.name());
        assertEquals("john@example.com", response.email());
    }

    @Test
    public void testParseResume_MissingChoices() {
        JsonNode responseNode = mapper.createObjectNode().put("id", "completion-id");

        when(restTemplate.postForEntity(anyString(), any(), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(responseNode, HttpStatus.OK));

        assertThrows(LlmException.class, () -> llmService.parseResume("Resume raw text"));
    }

    @Test
    public void testParseResume_MissingMessage() {
        JsonNode responseNode = mapper.createObjectNode();
        ((com.fasterxml.jackson.databind.node.ObjectNode) responseNode).set("choices", mapper.createArrayNode().add(mapper.createObjectNode()));

        when(restTemplate.postForEntity(anyString(), any(), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(responseNode, HttpStatus.OK));

        assertThrows(LlmException.class, () -> llmService.parseResume("Resume raw text"));
    }

    @Test
    public void testParseResume_MissingContent() {
        JsonNode responseNode = mapper.createObjectNode();
        JsonNode messageNode = mapper.createObjectNode();
        ((com.fasterxml.jackson.databind.node.ObjectNode) responseNode).set("choices", mapper.createArrayNode().add(
                mapper.createObjectNode().set("message", messageNode)
        ));

        when(restTemplate.postForEntity(anyString(), any(), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(responseNode, HttpStatus.OK));

        assertThrows(LlmException.class, () -> llmService.parseResume("Resume raw text"));
    }

    @Test
    public void testParseResume_MalformedContentJson() {
        JsonNode responseNode = createMockEnvelope("not a valid json object");

        when(restTemplate.postForEntity(anyString(), any(), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(responseNode, HttpStatus.OK));

        assertThrows(LlmException.class, () -> llmService.parseResume("Resume raw text"));
    }

    @Test
    public void testMatchCandidateWithJob_Success() {
        Candidate candidate = new Candidate();
        candidate.setName("Jane");
        candidate.setSkills("Java");
        candidate.setExperience("Exp");
        candidate.setEducation("Edu");

        JobDescription job = new JobDescription();
        job.setTitle("Dev");
        job.setDescription("Java");

        String mockContent = "{\"score\": 8, \"decision\": \"SHORTLIST\", \"matchedSkills\": [\"Java\"], \"missingSkills\": [], \"justification\": \"Good candidate.\"}";
        JsonNode responseNode = createMockEnvelope(mockContent);

        when(restTemplate.postForEntity(anyString(), any(), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(responseNode, HttpStatus.OK));

        LlmMatchResponse response = llmService.matchCandidateWithJob(candidate, job);
        assertNotNull(response);
        assertEquals(8, response.score());
        assertEquals("SHORTLIST", response.decision());
        assertEquals(List.of("Java"), response.matchedSkills());
    }

    private JsonNode createMockEnvelope(String assistantContent) {
        com.fasterxml.jackson.databind.node.ObjectNode root = mapper.createObjectNode();
        com.fasterxml.jackson.databind.node.ArrayNode choices = mapper.createArrayNode();
        com.fasterxml.jackson.databind.node.ObjectNode choice = mapper.createObjectNode();
        com.fasterxml.jackson.databind.node.ObjectNode message = mapper.createObjectNode();

        message.put("content", assistantContent);
        choice.set("message", message);
        choices.add(choice);
        root.set("choices", choices);

        return root;
    }
}
