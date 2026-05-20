package com.quizzar.analytics.dto;

import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class StudentResult {
    private UUID sessionId;
    private String studentName;
    private Integer totalScore;
    private Integer maxScore;
    private Double percentageScore;
    private Long timeTakenSeconds;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
}
