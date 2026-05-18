package com.busanit401.geminitestproject.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiErrorResponseDTO {

    private final String timestamp;
    private final int status;
    private final String error;
    private final String code;
    private final String message;
    private final String path;
}