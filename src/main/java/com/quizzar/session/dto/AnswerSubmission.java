package com.quizzar.session.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class AnswerSubmission {
    private UUID questionId;
    private UUID selectedOptionId;
    private String answerText;
    private Integer timeTakenSeconds;
}
