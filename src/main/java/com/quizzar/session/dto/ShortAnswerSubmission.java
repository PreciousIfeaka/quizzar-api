package com.quizzar.session.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ShortAnswerSubmission {
    private UUID submissionId;
    private String questionText;
    private String answerText;
    private String acceptedAnswers;
    private int timeTakenSecs;
}
