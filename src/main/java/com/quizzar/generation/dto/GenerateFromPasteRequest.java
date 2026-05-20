package com.quizzar.generation.dto;

import lombok.Data;

@Data
public class GenerateFromPasteRequest {
    private String quizTitle;
    private String quizDescription;
    private String rawText;
    private TimingPreference timingPreference;
    private Integer manualTimerSeconds;
}
