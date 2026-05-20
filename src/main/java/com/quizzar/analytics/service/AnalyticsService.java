package com.quizzar.analytics.service;

import com.quizzar.analytics.dto.*;
import com.quizzar.cache.config.CacheConfig;
import com.quizzar.common.exception.QuizNotFoundException;
import com.quizzar.common.exception.QuizOwnershipException;
import com.quizzar.question.entity.Question;
import com.quizzar.question.repository.QuestionRepository;
import com.quizzar.quiz.entity.Quiz;
import com.quizzar.quiz.repository.QuizRepository;
import com.quizzar.session.entity.QuizSession;
import com.quizzar.session.entity.SessionAnswer;
import com.quizzar.session.repository.QuizSessionRepository;
import com.quizzar.session.repository.SessionAnswerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final QuizRepository quizRepository;
    private final QuizSessionRepository sessionRepository;
    private final SessionAnswerRepository sessionAnswerRepository;
    private final QuestionRepository questionRepository;
    
    @Value("${quizzar.pass-threshold-percent}")
    private double passThreshold;

    @Cacheable(value = CacheConfig.ANALYTICS, key = "'summary-' + #keycloakSubject")
    public SummaryAnalyticsResponse getSummaryAnalytics(String keycloakSubject) {
        long totalQuizzes = quizRepository.countByTeacherKeycloakSubject(keycloakSubject);
        long totalAttempts = sessionRepository.countByQuizTeacherKeycloakSubject(keycloakSubject);
        
        List<QuizSession> completedSessions = sessionRepository
            .findByQuizTeacherKeycloakSubjectAndIsCompletedTrue(keycloakSubject);
            
        double avgScore = completedSessions.isEmpty() ? 0 :
            completedSessions.stream()
                .mapToDouble(s -> (double) s.getTotalScore() / s.getMaxScore() * 100)
                .average().orElse(0);

        OffsetDateTime oneMonthAgo = OffsetDateTime.now().minusMonths(1);
        long activeQuizzes = sessionRepository.countDistinctQuizByQuizTeacherKeycloakSubjectAndStartedAtAfter(
            keycloakSubject, oneMonthAgo);

        return SummaryAnalyticsResponse.builder()
            .totalQuizzes(totalQuizzes)
            .totalAttempts(totalAttempts)
            .averageScore(Math.round(avgScore * 100.0) / 100.0)
            .activeQuizzesThisMonth(activeQuizzes)
            .build();
    }

    @Cacheable(value = CacheConfig.ANALYTICS, key = "#quizId")
    public QuizAnalyticsResponse getAnalytics(UUID quizId, String keycloakSubject) {
        Quiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new QuizNotFoundException("Quiz not found: " + quizId));
        
        if (!quiz.getTeacher().getKeycloakSubject().equals(keycloakSubject)) {
            throw new QuizOwnershipException("You do not own this quiz");
        }
        
        List<QuizSession> completedSessions = sessionRepository
            .findByQuizIdAndIsCompletedTrue(quizId);
        
        int totalAttempts = sessionRepository.countByQuizId(quizId);
        int completedAttempts = completedSessions.size();
        
        double avgScore = completedSessions.isEmpty() ? 0 :
            completedSessions.stream()
                .mapToDouble(s -> (double) s.getTotalScore() / s.getMaxScore() * 100)
                .average().orElse(0);
        
        double highestScore = completedSessions.isEmpty() ? 0 :
            completedSessions.stream()
                .mapToDouble(s -> (double) s.getTotalScore() / s.getMaxScore() * 100)
                .max().orElse(0);
        
        double lowestScore = completedSessions.isEmpty() ? 0 :
            completedSessions.stream()
                .mapToDouble(s -> (double) s.getTotalScore() / s.getMaxScore() * 100)
                .min().orElse(0);
        
        double passRate = completedSessions.isEmpty() ? 0 :
            completedSessions.stream()
                .filter(s -> (double) s.getTotalScore() / s.getMaxScore() * 100 >= passThreshold)
                .count() * 100.0 / completedAttempts;
        
        OptionalDouble avgTime = completedSessions.stream()
            .filter(s -> s.getCompletedAt() != null)
            .mapToLong(s -> ChronoUnit.SECONDS.between(s.getStartedAt(), s.getCompletedAt()))
            .average();
        
        List<Question> questions = questionRepository.findByQuizIdOrderByOrderIndex(quizId);
        List<PerQuestionStat> perQuestionStats = buildPerQuestionStats(questions, quizId);
        
        List<StudentResult> studentResults = completedSessions.stream()
            .map(s -> StudentResult.builder()
                .sessionId(s.getId())
                .studentName(s.getStudentName())
                .totalScore(s.getTotalScore())
                .maxScore(s.getMaxScore())
                .percentageScore((double) s.getTotalScore() / s.getMaxScore() * 100)
                .timeTakenSeconds(s.getCompletedAt() != null 
                    ? ChronoUnit.SECONDS.between(s.getStartedAt(), s.getCompletedAt()) : null)
                .startedAt(s.getStartedAt())
                .completedAt(s.getCompletedAt())
                .build())
            .toList();
        
        Map<String, Long> scoreDistribution = buildScoreDistribution(completedSessions);
        
        return QuizAnalyticsResponse.builder()
            .quizId(quizId)
            .quizTitle(quiz.getTitle())
            .totalAttempts(totalAttempts)
            .completedAttempts(completedAttempts)
            .averageScore(Math.round(avgScore * 100.0) / 100.0)
            .highestScore(Math.round(highestScore * 100.0) / 100.0)
            .lowestScore(Math.round(lowestScore * 100.0) / 100.0)
            .averageTimeTakenSeconds(avgTime.isPresent() ? (long) avgTime.getAsDouble() : null)
            .passRate(Math.round(passRate * 100.0) / 100.0)
            .perQuestionStats(perQuestionStats)
            .studentResults(studentResults)
            .scoreDistribution(scoreDistribution)
            .build();
    }
    
    private List<PerQuestionStat> buildPerQuestionStats(List<Question> questions, UUID quizId) {
        return questions.stream().map(q -> {
            List<SessionAnswer> answers = sessionAnswerRepository
                .findByQuestionIdAndSessionQuizId(q.getId(), quizId);
            long correctCount = answers.stream().filter(SessionAnswer::getIsCorrect).count();
            OptionalDouble avgTime = answers.stream()
                .filter(a -> a.getTimeTakenSeconds() != null)
                .mapToInt(SessionAnswer::getTimeTakenSeconds).average();
            return PerQuestionStat.builder()
                .questionId(q.getId())
                .questionText(q.getQuestionText())
                .totalAnswers(answers.size())
                .correctAnswers((int) correctCount)
                .incorrectAnswers(answers.size() - (int) correctCount)
                .averageTimeTakenSeconds(avgTime.isPresent() ? avgTime.getAsDouble() : null)
                .build();
        }).toList();
    }
    
    private Map<String, Long> buildScoreDistribution(List<QuizSession> sessions) {
        Map<String, Long> dist = new LinkedHashMap<>();
        dist.put("0-20%", 0L); dist.put("21-40%", 0L); dist.put("41-60%", 0L);
        dist.put("61-80%", 0L); dist.put("81-100%", 0L);
        sessions.forEach(s -> {
            double pct = (double) s.getTotalScore() / s.getMaxScore() * 100;
            if (pct <= 20) dist.merge("0-20%", 1L, Long::sum);
            else if (pct <= 40) dist.merge("21-40%", 1L, Long::sum);
            else if (pct <= 60) dist.merge("41-60%", 1L, Long::sum);
            else if (pct <= 80) dist.merge("61-80%", 1L, Long::sum);
            else dist.merge("81-100%", 1L, Long::sum);
        });
        return dist;
    }
}
