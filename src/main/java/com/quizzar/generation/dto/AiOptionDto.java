package com.quizzar.generation.dto;

import lombok.Data;

@Data
public class AiOptionDto {
    private String label;
    private String text;
    private boolean isCorrect;
}
