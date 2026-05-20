package com.quizzar.analytics.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class PerQuestionStat {
    private UUID questionId;
    private String questionText;
    private int totalAnswers;
    private int correctAnswers;
    private int incorrectAnswers;
    private Double averageTimeTakenSeconds;
}
