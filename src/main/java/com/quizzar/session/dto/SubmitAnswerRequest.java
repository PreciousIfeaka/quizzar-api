package com.quizzar.session.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class SubmitAnswerRequest {
    private UUID questionId;
    private UUID selectedOptionId;
    private String answerText;
    private Integer timeTakenSeconds;
}
