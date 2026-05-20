package com.quizzar.quiz.dto;

import com.quizzar.question.dto.QuestionResponse;
import com.quizzar.quiz.entity.TimingMode;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class QuizResponse {
    private UUID id;
    private String title;
    private String description;
    private String quizCode;
    private TimingMode timingMode;
    private Integer timerValueSeconds;
    private List<QuestionResponse> questions;
    private OffsetDateTime createdAt;
}
