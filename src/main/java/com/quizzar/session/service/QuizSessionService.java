package com.quizzar.session.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizzar.cache.config.CacheConfig;
import com.quizzar.common.exception.QuizAlreadyCompletedException;
import com.quizzar.common.exception.QuizNotFoundException;
import com.quizzar.common.exception.QuizOwnershipException;
import com.quizzar.common.exception.SessionNotFoundException;
import com.quizzar.generation.service.AiGenerationService;
import com.quizzar.question.entity.AnswerOption;
import com.quizzar.question.entity.Question;
import com.quizzar.question.entity.QuestionType;
import com.quizzar.question.entity.ShortAnswerKey;
import com.quizzar.question.repository.AnswerOptionRepository;
import com.quizzar.question.repository.QuestionRepository;
import com.quizzar.quiz.entity.Quiz;
import com.quizzar.quiz.repository.QuizRepository;
import com.quizzar.session.dto.*;
import com.quizzar.session.entity.QuizSession;
import com.quizzar.session.entity.SessionAnswer;
import com.quizzar.session.repository.QuizSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class QuizSessionService {
    private final AnswerOptionRepository answerOptionRepository;

    private final QuizSessionRepository sessionRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final AiGenerationService aiGenerationService;

    public StartSessionResponse startSession(String quizCode, StartSessionRequest request, String ipAddress) {
        Quiz quiz = quizRepository.findByQuizCode(quizCode)
            .orElseThrow(() -> new QuizNotFoundException("Quiz not found: " + quizCode));
        
        QuizSession session = QuizSession.builder()
            .quiz(quiz)
            .studentName(request.getStudentName().trim())
            .ipAddress(ipAddress)
            .startedAt(OffsetDateTime.now())
            .isCompleted(false)
            .build();
        
        session = sessionRepository.save(session);
        return new StartSessionResponse(session.getId(), quiz.getTimingMode(), quiz.getTimerValueSeconds());
    }

    @CacheEvict(value = CacheConfig.ANALYTICS, key = "#result.quizId")
    @Transactional
    public QuizResultResponse submitAnswers(String quizCode, UUID sessionId, SubmitAnswersRequest request) {
        QuizSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found: " + sessionId));

        if (session.isCompleted()) {
            throw new QuizAlreadyCompletedException("This quiz session has already been submitted");
        }

        if (!session.getQuiz().getQuizCode().equals(quizCode)) {
            throw new QuizOwnershipException("Session does not match this quiz");
        }

        List<Question> questions = questionRepository.findByQuizIdOrderByOrderIndex(session.getQuiz().getId());
        int totalScore = 0;
        int maxScore = questions.stream().mapToInt(Question::getPoints).sum();

        List<ShortAnswerSubmission> shortAnswersSubmissions = new ArrayList<>();

        for (AnswerSubmission submission : request.getAnswers()) {
            Question question = questions.stream()
                    .filter(q -> q.getId().equals(submission.getQuestionId()))
                    .findFirst().orElse(null);

            if (question == null) continue;

            if (question.getQuestionType().equals(QuestionType.SHORT_ANSWER)) {
                boolean localCheck = evaluateAnswer(question, submission);

                if (localCheck) {
                    totalScore += question.getPoints();
                    SessionAnswer sessionAnswer = SessionAnswer.builder()
                            .session(session)
                            .question(question)
                            .selectedOption(null)
                            .answerText(submission.getAnswerText())
                            .isCorrect(true)
                            .timeTakenSeconds(submission.getTimeTakenSeconds())
                            .build();
                    session.getSessionAnswers().add(sessionAnswer);
                } else {
                    List<String> acceptedAnswers = question.getShortAnswerKeys().stream()
                            .map(ShortAnswerKey::getAcceptedAnswer).toList();

                    shortAnswersSubmissions.add(
                            ShortAnswerSubmission.builder()
                                    .submissionId(question.getId())
                                    .acceptedAnswers(String.join(", ", acceptedAnswers))
                                    .answerText(submission.getAnswerText())
                                    .questionText(question.getQuestionText())
                                    .timeTakenSecs(submission.getTimeTakenSeconds())
                                    .build()
                    );
                }
                continue;
            }

            boolean isCorrect = evaluateAnswer(question, submission);
            if (isCorrect) totalScore += question.getPoints();

            AnswerOption selectedOption = null;
            if (submission.getSelectedOptionId() != null) {
                selectedOption = answerOptionRepository.findById(submission.getSelectedOptionId())
                        .orElseThrow(() -> new SessionNotFoundException("Selected option not found: " + submission.getSelectedOptionId()));
            }

            SessionAnswer sessionAnswer = SessionAnswer.builder()
                    .session(session)
                    .question(question)
                    .selectedOption(selectedOption)
                    .answerText(submission.getAnswerText())
                    .isCorrect(isCorrect)
                    .timeTakenSeconds(submission.getTimeTakenSeconds())
                    .build();

            session.getSessionAnswers().add(sessionAnswer);
        }

        if (!shortAnswersSubmissions.isEmpty()) {
            String shortAnswersRawJson = convertShortAnswersDtoToJson(shortAnswersSubmissions);

            Map<UUID, Boolean> aiResults = aiGenerationService.gradeMultipleShortAnswers(shortAnswersRawJson);

            for (ShortAnswerSubmission submission : shortAnswersSubmissions) {
                Question question = questions.stream()
                        .filter(q -> q.getId().equals(submission.getSubmissionId()))
                        .findFirst().orElse(null);
                if (question == null) continue;

                boolean isAiCorrect = Boolean.TRUE.equals(aiResults.get(submission.getSubmissionId()));
                if (isAiCorrect) {
                    totalScore += question.getPoints();
                }

                SessionAnswer sessionAnswer = SessionAnswer.builder()
                        .session(session)
                        .question(question)
                        .selectedOption(null)
                        .answerText(submission.getAnswerText())
                        .isCorrect(isAiCorrect)
                        .timeTakenSeconds(submission.getTimeTakenSecs())
                        .build();

                session.getSessionAnswers().add(sessionAnswer);
            }
        }

        session.setCompleted(true);
        session.setCompletedAt(OffsetDateTime.now());
        session.setTotalScore(totalScore);
        session.setMaxScore(maxScore);
        sessionRepository.save(session);

        return buildQuizResultResponse(session);
    }

    public QuizResultResponse getSessionResults(UUID sessionId, String teacherKeycloakSubject) {
        QuizSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new SessionNotFoundException("Session not found: " + sessionId));

        if (teacherKeycloakSubject != null) {
            if (!session.getQuiz().getTeacher().getKeycloakSubject().equals(teacherKeycloakSubject)) {
                throw new QuizOwnershipException("You do not have permission to view these results");
            }
        }
        
        return buildQuizResultResponse(session);
    }

    private QuizResultResponse buildQuizResultResponse(QuizSession session) {
        List<DetailedSessionAnswerResponse> details = session.getSessionAnswers().stream()
            .map(this::mapToDetailedResponse)
            .toList();

        double percentage = session.getMaxScore() > 0 
            ? (double) session.getTotalScore() / session.getMaxScore() * 100 
            : 0;

        return QuizResultResponse.builder()
            .sessionId(session.getId())
            .quizId(session.getQuiz().getId())
            .studentName(session.getStudentName())
            .totalScore(session.getTotalScore())
            .maxScore(session.getMaxScore())
            .percentageScore(percentage)
            .passed(percentage >= 50)
            .completedAt(session.getCompletedAt())
            .details(details)
            .build();
    }

    private DetailedSessionAnswerResponse mapToDetailedResponse(SessionAnswer answer) {
        Question question = answer.getQuestion();
        AnswerOption correctOption = question.getAnswerOptions().stream()
            .filter(AnswerOption::isCorrect)
            .findFirst().orElse(null);

        return DetailedSessionAnswerResponse.builder()
            .questionId(question.getId())
            .questionText(question.getQuestionText())
            .questionType(question.getQuestionType().name())
            .selectedOptionLabel(answer.getSelectedOption() != null ? answer.getSelectedOption().getOptionLabel() : null)
            .selectedOptionText(answer.getSelectedOption() != null ? answer.getSelectedOption().getOptionText() : null)
            .selectedAnswerText(answer.getAnswerText())
            .correctOptionLabel(correctOption != null ? correctOption.getOptionLabel() : null)
            .correctOptionText(correctOption != null ? correctOption.getOptionText() : null)
            .correctShortAnswerKeys(question.getShortAnswerKeys().stream().map(ShortAnswerKey::getAcceptedAnswer).toList())
            .isCorrect(answer.getIsCorrect())
            .pointsEarned(answer.getIsCorrect() ? question.getPoints() : 0)
            .maxPoints(question.getPoints())
            .build();
    }
    
    private boolean evaluateAnswer(Question question, AnswerSubmission submission) {
        return switch (question.getQuestionType()) {
            case MCQ, TRUE_FALSE -> {
                if (submission.getSelectedOptionId() == null) yield false;
                yield question.getAnswerOptions().stream()
                    .anyMatch(opt -> opt.getId().equals(submission.getSelectedOptionId()) && opt.isCorrect());
            }
            case SHORT_ANSWER -> {
                if (!StringUtils.hasText(submission.getAnswerText())) yield false;
                List<String> acceptedAnswers = question.getShortAnswerKeys().stream()
                    .map(ShortAnswerKey::getAcceptedAnswer).toList();
                yield acceptedAnswers.stream().anyMatch(submission.getAnswerText()::equalsIgnoreCase);
            }
        };
    }

    private String convertShortAnswersDtoToJson(List<ShortAnswerSubmission> submissions) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(submissions);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize DTO list for Gemini context", e);
        }
    }
}
