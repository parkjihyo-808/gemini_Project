package com.busanit401.geminitestproject.service;


import com.busanit401.geminitestproject.dto.ChatRequestDTO;
import com.busanit401.geminitestproject.dto.ChatResponseDTO;
import com.busanit401.geminitestproject.dto.ImageAnalysisResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface GeminiService {

    ChatResponseDTO chat(ChatRequestDTO requestDTO);

    ImageAnalysisResponseDTO analyzeImage(MultipartFile image, String prompt);
}