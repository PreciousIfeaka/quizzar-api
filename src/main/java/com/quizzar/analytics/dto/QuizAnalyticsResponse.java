package com.quizzar.analytics.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class QuizAnalyticsResponse {
    private UUID quizId;
    private String quizTitle;
    private int totalAttempts;
    private int completedAttempts;
    private double averageScore;
    private double highestScore;
    private double lowestScore;
    private Long averageTimeTakenSeconds;
    private double passRate;
    private List<PerQuestionStat> perQuestionStats;
    private List<StudentResult> studentResults;
    private Map<String, Long> scoreDistribution;
}
