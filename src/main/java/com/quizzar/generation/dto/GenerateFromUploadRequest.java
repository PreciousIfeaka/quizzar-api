package com.quizzar.generation.dto;

import com.quizzar.quiz.entity.QuizMode;
import lombok.Data;

@Data
public class GenerateFromUploadRequest {
    private String quizTitle;
    private String quizDescription;
    private TimingPreference timingPreference;
    private Integer manualTimerSeconds;
    private QuizMode quizMode = QuizMode.OVERALL;
}
