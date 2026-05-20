package com.quizzar.generation.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiQuestionDto {
    private String questionText;
    private String questionType;
    private Integer orderIndex;
    private Integer points;
    private List<AiOptionDto> options;
    private List<String> acceptedAnswers;
}
