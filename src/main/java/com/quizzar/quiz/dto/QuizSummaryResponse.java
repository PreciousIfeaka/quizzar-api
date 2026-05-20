package com.quizzar.quiz.dto;

import com.quizzar.quiz.entity.TimingMode;
import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class QuizSummaryResponse {
    private UUID id;
    private String title;
    private String description;
    private String quizCode;
    private int questionCount;
    private TimingMode timingMode;
    private OffsetDateTime createdAt;
}
