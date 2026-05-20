package com.quizzar.question.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AnswerOptionResponse {
    private UUID id;
    private String text;
    private String label;
    private Boolean isCorrect; // Nullable for public view
}
