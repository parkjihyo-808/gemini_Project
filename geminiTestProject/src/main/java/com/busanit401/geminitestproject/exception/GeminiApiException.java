package com.busanit401.geminitestproject.exception;


import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class GeminiApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public GeminiApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
