package com.quizzar.generation.dto;

import com.quizzar.quiz.entity.QuizMode;
import lombok.Data;

@Data
public class GenerateFromPasteRequest {
    private String quizTitle;
    private String quizDescription;
    private String rawText;
    private TimingPreference timingPreference;
    private Integer manualTimerSeconds;
    private QuizMode quizMode = QuizMode.OVERALL;
}
