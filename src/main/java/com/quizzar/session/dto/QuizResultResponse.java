package com.quizzar.session.dto;

import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class QuizResultResponse {
    private UUID sessionId;
    private UUID quizId;
    private String studentName;
    private Integer totalScore;
    private Integer maxScore;
    private Double percentageScore;
    private boolean passed;
    private OffsetDateTime completedAt;
    private List<DetailedSessionAnswerResponse> details;
}
