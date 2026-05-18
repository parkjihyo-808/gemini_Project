package com.busanit401.geminitestproject.handler;

import com.busanit401.geminitestproject.dto.AiErrorResponseDTO;
import com.busanit401.geminitestproject.exception.GeminiApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GeminiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AiErrorResponseDTO> handleValidation(
            MethodArgumentNotValidException e,
            HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().isEmpty()
                ? "요청값이 올바르지 않습니다."
                : e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AiErrorResponseDTO> handleBadRequest(
            IllegalArgumentException e,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", e.getMessage(), request);
    }

    @ExceptionHandler(GeminiApiException.class)
    public ResponseEntity<AiErrorResponseDTO> handleGeminiApiException(
            GeminiApiException e,
            HttpServletRequest request) {
        return buildErrorResponse(e.getStatus(), e.getCode(), e.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<AiErrorResponseDTO> handleIllegalState(
            IllegalStateException e,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "AI_INTERNAL_ERROR", e.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AiErrorResponseDTO> handleUnexpected(
            Exception e,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "UNEXPECTED_ERROR", "알 수 없는 오류가 발생했습니다.", request);
    }

    private ResponseEntity<AiErrorResponseDTO> buildErrorResponse(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request) {
        AiErrorResponseDTO body = AiErrorResponseDTO.builder()
                .timestamp(OffsetDateTime.now().toString())
                .status(status.value())
                .error(status.getReasonPhrase())
                .code(code)
                .message(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(status).body(body);
    }
}
