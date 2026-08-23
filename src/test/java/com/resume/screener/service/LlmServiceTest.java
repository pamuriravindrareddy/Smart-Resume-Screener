package com.resume.screener.service;

import com.resume.screener.dto.LlmMatchResponse;
import com.resume.screener.exception.LlmException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LlmServiceTest {

    private LlmService llmService;

    @BeforeEach
    public void setUp() {
        llmService = new LlmService(new RestTemplate());
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
}
