package com.quizzar.analytics.controller;

import com.quizzar.analytics.dto.QuizAnalyticsResponse;
import com.quizzar.analytics.dto.SummaryAnalyticsResponse;
import com.quizzar.analytics.service.AnalyticsService;
import com.quizzar.auth.util.SecurityUtils;
import com.quizzar.common.dto.ApiResponse;
import com.quizzar.session.dto.QuizResultResponse;
import com.quizzar.session.service.QuizSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final QuizSessionService sessionService;
    private final SecurityUtils securityUtils;

    @GetMapping("/summary")
    public ApiResponse<SummaryAnalyticsResponse> getSummaryAnalytics() {
        String subject = securityUtils.getCurrentKeycloakSubject();
        return ApiResponse.ok(analyticsService.getSummaryAnalytics(subject));
    }

    @GetMapping("/quizzes/{quizId}")
    public ApiResponse<QuizAnalyticsResponse> getQuizAnalytics(@PathVariable UUID quizId) {
        String subject = securityUtils.getCurrentKeycloakSubject();
        return ApiResponse.ok(analyticsService.getAnalytics(quizId, subject));
    }

    @GetMapping("/sessions/{sessionId}/results")
    public ApiResponse<QuizResultResponse> getSessionResults(@PathVariable UUID sessionId) {
        String subject = securityUtils.getCurrentKeycloakSubject();
        return ApiResponse.ok(sessionService.getSessionResults(sessionId, subject));
    }
}
