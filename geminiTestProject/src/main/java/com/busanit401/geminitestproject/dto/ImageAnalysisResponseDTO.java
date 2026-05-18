package com.busanit401.geminitestproject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageAnalysisResponseDTO {

    private String description;
    private String filename;
    private String mimeType;
    private String model;
    private boolean implemented;
}