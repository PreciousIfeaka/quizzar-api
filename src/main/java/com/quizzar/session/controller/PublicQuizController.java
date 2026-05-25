package com.quizzar.session.controller;

import com.quizzar.common.dto.ApiResponse;
import com.quizzar.quiz.dto.PublicQuizResponse;
import com.quizzar.quiz.service.QuizService;
import com.quizzar.session.dto.*;
import com.quizzar.session.service.QuizSessionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/public/quiz")
@RequiredArgsConstructor
public class PublicQuizController {

    private final QuizService quizService;
    private final QuizSessionService sessionService;

    @GetMapping("/{quizCode}")
    public ApiResponse<PublicQuizResponse> getPublicQuiz(@PathVariable String quizCode) {
        return ApiResponse.ok(quizService.getPublicQuiz(quizCode));
    }

    @PostMapping("/{quizCode}/start")
    public ApiResponse<StartSessionResponse> startSession(
            @PathVariable String quizCode,
            @RequestBody StartSessionRequest request,
            HttpServletRequest servletRequest) {
        String ipAddress = getClientIp(servletRequest);
        return ApiResponse.ok(sessionService.startSession(quizCode, request, ipAddress));
    }

    @PostMapping("/{quizCode}/sessions/{sessionId}/submit")
    public ApiResponse<QuizResultResponse> submitAnswers(
            @PathVariable String quizCode,
            @PathVariable UUID sessionId,
            @RequestBody SubmitAnswersRequest request) {
        return ApiResponse.ok(sessionService.submitAnswers(quizCode, sessionId, request));
    }

    @PostMapping("/{quizCode}/sessions/{sessionId}/submit-answer")
    public ApiResponse<QuestionResultResponse> submitSingleAnswer(
            @PathVariable String quizCode,
            @PathVariable UUID sessionId,
            @RequestBody SubmitAnswerRequest request) {
        return ApiResponse.ok(sessionService.submitSingleAnswer(quizCode, sessionId, request));
    }

    @PostMapping("/{quizCode}/sessions/{sessionId}/complete")
    public ApiResponse<QuizResultResponse> completeSession(
            @PathVariable String quizCode,
            @PathVariable UUID sessionId) {
        return ApiResponse.ok(sessionService.completeSession(quizCode, sessionId));
    }

    @GetMapping("/{quizCode}/sessions/{sessionId}/results")
    public ApiResponse<QuizResultResponse> getSessionResults(
            @PathVariable String quizCode,
            @PathVariable UUID sessionId) {
        return ApiResponse.ok(sessionService.getSessionResults(sessionId, null));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
