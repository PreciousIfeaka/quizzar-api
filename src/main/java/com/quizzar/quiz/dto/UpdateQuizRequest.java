package com.quizzar.quiz.dto;

import com.quizzar.quiz.entity.TimingMode;
import lombok.Data;

@Data
public class UpdateQuizRequest {
    private String title;
    private String description;
    private TimingMode timingMode;
    private Integer timerValueSeconds;
}
