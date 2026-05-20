package com.quizzar.generation.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiQuizGenerationResult {

    private List<AiQuestionDto> questions;
    private String aiSuggestedTimingMode;   // "NONE" | "PER_QUESTION" | "OVERALL"
    private Integer aiSuggestedTimeSeconds;
    private String aiTimingReasoning;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiQuestionDto {
        private String questionText;
        private String questionType;        // "MCQ" | "TRUE_FALSE" | "SHORT_ANSWER"
        private Integer orderIndex;
        private Integer points;
        private List<AiOptionDto> options;          // null for SHORT_ANSWER
        private List<String> acceptedAnswers;       // null for MCQ and TRUE_FALSE
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiOptionDto {
        private String label;       // "A", "B", "C", "D", "True", "False"
        private String text;

        @JsonProperty("isCorrect")
        private boolean isCorrect;
    }
}
