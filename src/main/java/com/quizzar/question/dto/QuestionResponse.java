package com.quizzar.question.dto;

import com.quizzar.question.entity.QuestionType;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class QuestionResponse {
    private UUID id;
    private String questionText;
    private QuestionType questionType;
    private Integer orderIndex;
    private Integer points;
    private List<AnswerOptionResponse> options;
}
