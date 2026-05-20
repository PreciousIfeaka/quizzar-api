package com.quizzar.generation.dto;

import lombok.Data;

@Data
public class GenerateFromUploadRequest {
    private String quizTitle;
    private String quizDescription;
    private TimingPreference timingPreference;
    private Integer manualTimerSeconds;
}
