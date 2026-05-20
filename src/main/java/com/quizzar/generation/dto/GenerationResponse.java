package com.quizzar.generation.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class GenerationResponse {
    private UUID quizId;
    private String quizCode;
    private String shareUrl;
    private AiTimingSuggestion aiTimingSuggestion;
}
