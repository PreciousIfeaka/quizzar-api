package com.quizzar.quiz.dto;

import com.quizzar.question.dto.QuestionResponse;
import com.quizzar.quiz.entity.QuizMode;
import com.quizzar.quiz.entity.TimingMode;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PublicQuizResponse {
    private String title;
    private String description;
    private String quizCode;
    private TimingMode timingMode;
    private QuizMode quizMode;
    private Integer timerValueSeconds;
    private List<QuestionResponse> questions;
}
