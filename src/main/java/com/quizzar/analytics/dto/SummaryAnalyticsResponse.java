package com.quizzar.analytics.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SummaryAnalyticsResponse {
    private long totalQuizzes;
    private long totalAttempts;
    private double averageScore;
    private long activeQuizzesThisMonth;
}
