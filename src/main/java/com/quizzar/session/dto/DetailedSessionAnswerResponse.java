package com.quizzar.session.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class DetailedSessionAnswerResponse {
    private UUID questionId;
    private String questionText;
    private String questionType;
    private String selectedOptionLabel; // A, B, C, D (for MCQ/TF)
    private String selectedOptionText;  // (for MCQ/TF)
    private String selectedAnswerText;  // (for SHORT_ANSWER)
    private String correctOptionLabel;  // A, B, C, D (for MCQ/TF)
    private String correctOptionText;   // (for MCQ/TF)
    private List<String> correctShortAnswerKeys; // (for SHORT_ANSWER)
    private boolean isCorrect;
    private Integer pointsEarned;
    private Integer maxPoints;
}
