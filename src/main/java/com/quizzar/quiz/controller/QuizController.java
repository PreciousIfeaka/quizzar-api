package com.quizzar.quiz.controller;

import com.quizzar.auth.util.SecurityUtils;
import com.quizzar.common.dto.ApiResponse;
import com.quizzar.common.dto.PageResponse;
import com.quizzar.quiz.dto.*;
import com.quizzar.quiz.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ApiResponse<PageResponse<QuizSummaryResponse>> getAllQuizzes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        java.util.UUID teacherId = securityUtils.getCurrentTeacherId();
 
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
 
        Pageable pageable = PageRequest.of(page, size, sort);
 
        return ApiResponse.ok(quizService.getAllQuizzes(teacherId, pageable));
    }

    @GetMapping("/{quizId}")
    public ApiResponse<QuizResponse> getQuiz(@PathVariable UUID quizId) {
        java.util.UUID teacherId = securityUtils.getCurrentTeacherId();
        return ApiResponse.ok(quizService.getQuizById(quizId, teacherId));
    }

    @PatchMapping("/{quizId}")
    public ApiResponse<QuizResponse> updateQuiz(
            @PathVariable UUID quizId, 
            @RequestBody UpdateQuizRequest request) {
        java.util.UUID teacherId = securityUtils.getCurrentTeacherId();
        return ApiResponse.ok(quizService.updateQuiz(quizId, request, teacherId));
    }

    @DeleteMapping("/{quizId}")
    public ApiResponse<Void> deleteQuiz(@PathVariable UUID quizId) {
        java.util.UUID teacherId = securityUtils.getCurrentTeacherId();
        quizService.deleteQuiz(quizId, teacherId);
        return ApiResponse.ok(null, "Quiz deleted successfully");
    }

    @GetMapping("/{quizId}/link")
    public ApiResponse<QuizLinkResponse> getQuizLink(@PathVariable UUID quizId) {
        java.util.UUID teacherId = securityUtils.getCurrentTeacherId();
        return ApiResponse.ok(quizService.getQuizLink(quizId, teacherId));
    }

    @PostMapping("/{quizId}/regenerate-code")
    public ApiResponse<QuizLinkResponse> regenerateCode(@PathVariable UUID quizId) {
        java.util.UUID teacherId = securityUtils.getCurrentTeacherId();
        return ApiResponse.ok(quizService.regenerateQuizCode(quizId, teacherId));
    }
}
