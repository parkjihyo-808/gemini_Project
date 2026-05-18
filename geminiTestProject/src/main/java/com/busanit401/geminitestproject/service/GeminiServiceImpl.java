package com.busanit401.geminitestproject.service;

import com.busanit401.geminitestproject.dto.ChatRequestDTO;
import com.busanit401.geminitestproject.dto.ChatResponseDTO;
import com.busanit401.geminitestproject.dto.ImageAnalysisResponseDTO;
import com.busanit401.geminitestproject.exception.GeminiApiException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@Log4j2
public class GeminiServiceImpl implements GeminiService {

    private final WebClient webClient;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.model}")
    private String model;

    public GeminiServiceImpl(
            WebClient.Builder webClientBuilder,
            @Value("${gemini.api.base-url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public ChatResponseDTO chat(ChatRequestDTO requestDTO) {
        validateApiKey();

        String prompt = requestDTO.getPrompt().trim();
        log.info("Gemini chat request. model={}, promptLength={}", model, prompt.length());

        try {
            Map<String, Object> response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .bodyValue(buildChatRequestBody(prompt))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            return ChatResponseDTO.builder()
                    .reply(extractText(response))
                    .model(model)
                    .implemented(true)
                    .build();
        } catch (WebClientResponseException e) {
            throw toGeminiApiException(e);
        } catch (Exception e) {
            throw new IllegalStateException("Gemini 응답 처리 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public ImageAnalysisResponseDTO analyzeImage(MultipartFile image, String prompt) {
        validateApiKey();
        validateImage(image);

        String normalizedPrompt = prompt == null || prompt.isBlank()
                ? "이 이미지를 한국어로 설명해주세요."
                : prompt.trim();

        try {
            Map<String, Object> response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .bodyValue(buildImageRequestBody(image, normalizedPrompt))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            return ImageAnalysisResponseDTO.builder()
                    .description(extractText(response))
                    .filename(image.getOriginalFilename())
                    .mimeType(resolveImageMimeType(image))
                    .model(model)
                    .implemented(true)
                    .build();
        } catch (WebClientResponseException e) {
            throw toGeminiApiException(e);
        } catch (IOException e) {
            throw new IllegalStateException("업로드한 이미지를 읽는 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            throw new IllegalStateException("Gemini 이미지 분석 처리 중 오류가 발생했습니다.", e);
        }
    }

    private Map<String, Object> buildChatRequestBody(String prompt) {
        return Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", prompt))
                        )
                ),
                "generationConfig", Map.of(
                        "temperature", 0.7,
                        "maxOutputTokens", 1024
                )
        );
    }

    private Map<String, Object> buildImageRequestBody(MultipartFile image, String prompt) throws IOException {
        String mimeType = resolveImageMimeType(image);
        if (!mimeType.startsWith("image/")) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다. PNG, JPG, WEBP 등을 사용해주세요.");
        }

        String encodedImage = Base64.getEncoder().encodeToString(image.getBytes());

        return Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(
                                        Map.of("text", prompt),
                                        Map.of(
                                                "inline_data", Map.of(
                                                        "mime_type", mimeType,
                                                        "data", encodedImage
                                                )
                                        )
                                )
                        )
                ),
                "generationConfig", Map.of(
                        "temperature", 0.4,
                        "maxOutputTokens", 1024
                )
        );
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> response) {
        if (response == null) {
            throw new IllegalStateException("Gemini 응답이 비어 있습니다.");
        }

        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException("Gemini candidates 응답이 없습니다.");
        }

        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        Object text = parts.get(0).get("text");

        if (!(text instanceof String reply) || reply.isBlank()) {
            throw new IllegalStateException("Gemini text 응답이 비어 있습니다.");
        }

        return reply;
    }

    private void validateApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("gemini.api.key 설정이 필요합니다.");
        }
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("image 파일은 비어 있을 수 없습니다.");
        }
    }

    private String resolveImageMimeType(MultipartFile image) {
        String contentType = image.getContentType();
        if (contentType != null && !contentType.isBlank() && !"application/octet-stream".equals(contentType)) {
            return contentType;
        }

        return MediaTypeFactory.getMediaType(image.getOriginalFilename())
                .map(MediaType::toString)
                .orElse("application/octet-stream");
    }

    private GeminiApiException toGeminiApiException(WebClientResponseException e) {
        if (e.getStatusCode().value() == 429) {
            return new GeminiApiException(HttpStatus.BAD_GATEWAY, "RATE_LIMITED", "AI 요청이 잠시 많습니다. 잠시 후 다시 시도해주세요.");
        }

        if (e.getStatusCode().is5xxServerError()) {
            return new GeminiApiException(HttpStatus.BAD_GATEWAY, "UPSTREAM_TEMPORARY_ERROR", "Gemini 서비스가 일시적으로 불안정합니다.");
        }

        return new GeminiApiException(HttpStatus.BAD_GATEWAY, "GEMINI_HTTP_ERROR", "Gemini API 호출에 실패했습니다.");
    }
}
