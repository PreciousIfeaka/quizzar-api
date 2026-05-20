package com.quizzar.generation.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiQuizGenerationResult {
    private List<AiQuestionDto> questions;
    private String aiSuggestedTimingMode;
    private Integer aiSuggestedTimeSeconds;
    private String aiTimingReasoning;
}
