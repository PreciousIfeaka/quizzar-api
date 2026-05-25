package com.quizzar.session.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class QuestionResultResponse {
    private boolean isCorrect;
    private int pointsEarned;
    private String correctOptionLabel;
    private String correctOptionText;
    private List<String> correctShortAnswerKeys;
}
