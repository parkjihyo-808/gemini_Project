package com.busanit401.geminitestproject.controller;


import com.busanit401.geminitestproject.dto.ChatRequestDTO;
import com.busanit401.geminitestproject.dto.ChatResponseDTO;
import com.busanit401.geminitestproject.dto.ImageAnalysisResponseDTO;
import com.busanit401.geminitestproject.service.GeminiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "Gemini AI No JWT API", description = "JWT 없이 테스트하는 Gemini AI API")
public class GeminiController {

    private final GeminiService geminiService;

    @PostMapping("/chat")
    @Operation(summary = "Gemini 텍스트 챗봇")
    public ResponseEntity<ChatResponseDTO> chat(@Valid @RequestBody ChatRequestDTO requestDTO) {
        return ResponseEntity.ok(geminiService.chat(requestDTO));
    }

    @PostMapping(value = "/analyze-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Gemini 이미지 분석")
    public ResponseEntity<ImageAnalysisResponseDTO> analyzeImage(
            @RequestPart MultipartFile image,
            @RequestParam(defaultValue = "이 이미지를 한국어로 설명해주세요.") String prompt) {
        return ResponseEntity.ok(geminiService.analyzeImage(image, prompt));
    }
}
