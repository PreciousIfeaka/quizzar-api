package com.quizzar.quiz.util;

import com.quizzar.question.dto.AnswerOptionResponse;
import com.quizzar.question.dto.QuestionResponse;
import com.quizzar.question.entity.Question;
import com.quizzar.quiz.dto.PublicQuizResponse;
import com.quizzar.quiz.dto.QuizResponse;
import com.quizzar.quiz.entity.Quiz;

import java.util.stream.Collectors;

public class QuizMapper {

    public static QuizResponse toDetailResponse(Quiz quiz) {
        return QuizResponse.builder()
            .id(quiz.getId())
            .title(quiz.getTitle())
            .description(quiz.getDescription())
            .quizCode(quiz.getQuizCode())
            .timingMode(quiz.getTimingMode())
            .timerValueSeconds(quiz.getTimerValueSeconds())
            .createdAt(quiz.getCreatedAt())
            .questions(quiz.getQuestions().stream()
                .map(QuizMapper::toQuestionResponse)
                .collect(Collectors.toList()))
            .build();
    }

    public static PublicQuizResponse toPublicResponse(Quiz quiz) {
        return PublicQuizResponse.builder()
            .title(quiz.getTitle())
            .description(quiz.getDescription())
            .quizCode(quiz.getQuizCode())
            .timingMode(quiz.getTimingMode())
            .timerValueSeconds(quiz.getTimerValueSeconds())
            .questions(quiz.getQuestions().stream()
                .map(QuizMapper::toPublicQuestionResponse)
                .collect(Collectors.toList()))
            .build();
    }

    private static QuestionResponse toQuestionResponse(Question question) {
        return QuestionResponse.builder()
            .id(question.getId())
            .questionText(question.getQuestionText())
            .questionType(question.getQuestionType())
            .orderIndex(question.getOrderIndex())
            .points(question.getPoints())
            .options(question.getAnswerOptions().stream()
                .map(opt -> AnswerOptionResponse.builder()
                    .id(opt.getId())
                    .text(opt.getOptionText())
                    .label(opt.getOptionLabel())
                    .isCorrect(opt.isCorrect())
                    .build())
                .collect(Collectors.toList()))
            .build();
    }

    private static QuestionResponse toPublicQuestionResponse(Question question) {
        return QuestionResponse.builder()
            .id(question.getId())
            .questionText(question.getQuestionText())
            .questionType(question.getQuestionType())
            .orderIndex(question.getOrderIndex())
            .points(question.getPoints())
            .options(question.getAnswerOptions().stream()
                .map(opt -> AnswerOptionResponse.builder()
                    .id(opt.getId())
                    .text(opt.getOptionText())
                    .label(opt.getOptionLabel())
                    .isCorrect(null) // STRIP CORRECT ANSWER
                    .build())
                .collect(Collectors.toList()))
            .build();
    }
}
