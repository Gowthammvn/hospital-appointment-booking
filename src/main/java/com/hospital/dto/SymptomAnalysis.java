package com.hospital.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Result of NLP symptom analysis")
public record SymptomAnalysis(
        @Schema(example = "Cardiology") String department,
        @Schema(description = "Probability of the top department, 0-1", example = "0.62") double confidence,
        @Schema(description = "Probability per department") Map<String, Double> scores
) {
}
