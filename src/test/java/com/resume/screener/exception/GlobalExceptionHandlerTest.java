package com.resume.screener.exception;

import com.resume.screener.dto.ErrorResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

public class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    public void testHandleGeneralException_SecureMessage() {
        RuntimeException sensitiveException = new RuntimeException("Sensitive database constraint violation on column 'credit_card'");

        ResponseEntity<ErrorResponseDto> response = exceptionHandler.handleGeneralException(sensitiveException);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        ErrorResponseDto body = response.getBody();
        assertNotNull(body);
        assertEquals(500, body.status());
        assertEquals("Internal Server Error", body.error());
        
        // Assert that the sensitive message is NOT leaked to the client
        assertNotEquals(sensitiveException.getMessage(), body.message());
        assertTrue(body.message().contains("unexpected internal error occurred"));
    }
}
