package com.quizzar.session;

import com.quizzar.generation.service.AiGenerationService;
import com.quizzar.question.entity.AnswerOption;
import com.quizzar.question.entity.Question;
import com.quizzar.question.entity.QuestionType;
import com.quizzar.question.entity.ShortAnswerKey;
import com.quizzar.question.repository.AnswerOptionRepository;
import com.quizzar.question.repository.QuestionRepository;
import com.quizzar.quiz.entity.Quiz;
import com.quizzar.session.dto.QuestionResultResponse;
import com.quizzar.session.dto.QuizResultResponse;
import com.quizzar.session.dto.SubmitAnswerRequest;
import com.quizzar.session.entity.QuizSession;
import com.quizzar.session.entity.SessionAnswer;
import com.quizzar.session.repository.QuizSessionRepository;
import com.quizzar.session.service.QuizSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizSessionServiceTest {

        @Mock
        private AnswerOptionRepository answerOptionRepository;
        @Mock
        private QuizSessionRepository sessionRepository;
        @Mock
        private QuestionRepository questionRepository;
        @Mock
        private AiGenerationService aiGenerationService;

        @InjectMocks
        private QuizSessionService quizSessionService;

        private UUID sessionId;
        private String quizCode;
        private Quiz quiz;
        private QuizSession session;

        @BeforeEach
        void setUp() {
                sessionId = UUID.randomUUID();
                quizCode = "CODE1234";
                quiz = Quiz.builder()
                                .id(UUID.randomUUID())
                                .quizCode(quizCode)
                                .build();
                session = QuizSession.builder()
                                .id(sessionId)
                                .quiz(quiz)
                                .isCompleted(false)
                                .sessionAnswers(new ArrayList<>())
                                .build();
        }

        @Test
        void submitSingleAnswer_McqCorrect_ReturnsIsCorrectTrue() {
                UUID questionId = UUID.randomUUID();
                UUID selectedOptionId = UUID.randomUUID();

                Question question = Question.builder()
                                .id(questionId)
                                .quiz(quiz)
                                .questionType(QuestionType.MCQ)
                                .points(5)
                                .answerOptions(new ArrayList<>())
                                .build();

                AnswerOption correctOption = AnswerOption.builder()
                                .id(selectedOptionId)
                                .question(question)
                                .optionLabel("A")
                                .optionText("Paris")
                                .isCorrect(true)
                                .build();
                question.getAnswerOptions().add(correctOption);

                SubmitAnswerRequest request = new SubmitAnswerRequest();
                request.setQuestionId(questionId);
                request.setSelectedOptionId(selectedOptionId);
                request.setTimeTakenSeconds(10);

                when(sessionRepository.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));
                when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
                when(answerOptionRepository.findById(selectedOptionId)).thenReturn(Optional.of(correctOption));

                QuestionResultResponse response = quizSessionService.submitSingleAnswer(quizCode, sessionId, request);

                assertTrue(response.isCorrect());
                assertEquals(5, response.getPointsEarned());
                assertEquals("A", response.getCorrectOptionLabel());
                assertEquals("Paris", response.getCorrectOptionText());
                verify(sessionRepository).save(session);
                assertEquals(1, session.getSessionAnswers().size());
                assertTrue(session.getSessionAnswers().get(0).getIsCorrect());
        }

        @Test
        void submitSingleAnswer_ShortAnswerLocalMatch_DoesNotCallGemini() {
                UUID questionId = UUID.randomUUID();
                Question question = Question.builder()
                                .id(questionId)
                                .quiz(quiz)
                                .questionType(QuestionType.SHORT_ANSWER)
                                .points(3)
                                .shortAnswerKeys(new ArrayList<>())
                                .answerOptions(new ArrayList<>())
                                .build();

                ShortAnswerKey key = ShortAnswerKey.builder()
                                .question(question)
                                .acceptedAnswer("Berlin")
                                .build();
                question.getShortAnswerKeys().add(key);

                SubmitAnswerRequest request = new SubmitAnswerRequest();
                request.setQuestionId(questionId);
                request.setAnswerText("berlin"); // case-insensitive check should match
                request.setTimeTakenSeconds(5);

                when(sessionRepository.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));
                when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

                QuestionResultResponse response = quizSessionService.submitSingleAnswer(quizCode, sessionId, request);

                assertTrue(response.isCorrect());
                assertEquals(3, response.getPointsEarned());
                verify(aiGenerationService, never()).gradeShortAnswer(any(), any(), any());
                verify(sessionRepository).save(session);
        }

        @Test
        void submitSingleAnswer_ShortAnswerLocalMismatch_CallsGeminiAndReturnsTrue() {
                UUID questionId = UUID.randomUUID();
                Question question = Question.builder()
                                .id(questionId)
                                .quiz(quiz)
                                .questionText("What is the capital of Germany?")
                                .questionType(QuestionType.SHORT_ANSWER)
                                .points(3)
                                .shortAnswerKeys(new ArrayList<>())
                                .answerOptions(new ArrayList<>())
                                .build();

                ShortAnswerKey key = ShortAnswerKey.builder()
                                .question(question)
                                .acceptedAnswer("Berlin")
                                .build();
                question.getShortAnswerKeys().add(key);

                SubmitAnswerRequest request = new SubmitAnswerRequest();
                request.setQuestionId(questionId);
                request.setAnswerText("It is Berlin."); // exact match fails
                request.setTimeTakenSeconds(5);

                when(sessionRepository.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));
                when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
                when(aiGenerationService.gradeShortAnswer(eq("What is the capital of Germany?"), eq("It is Berlin."),
                                anyList()))
                                .thenReturn(true);

                QuestionResultResponse response = quizSessionService.submitSingleAnswer(quizCode, sessionId, request);

                assertTrue(response.isCorrect());
                assertEquals(3, response.getPointsEarned());
                verify(aiGenerationService).gradeShortAnswer(any(), any(), any());
        }

        @Test
        void submitSingleAnswer_DoubleAnswer_ThrowsException() {
                UUID questionId = UUID.randomUUID();
                Question question = Question.builder()
                                .id(questionId)
                                .quiz(quiz)
                                .questionType(QuestionType.MCQ)
                                .answerOptions(new ArrayList<>())
                                .build();

                SessionAnswer existingAnswer = SessionAnswer.builder()
                                .question(question)
                                .build();
                session.getSessionAnswers().add(existingAnswer);

                SubmitAnswerRequest request = new SubmitAnswerRequest();
                request.setQuestionId(questionId);

                when(sessionRepository.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));
                when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

                assertThrows(IllegalStateException.class,
                                () -> quizSessionService.submitSingleAnswer(quizCode, sessionId, request));
        }

        @Test
        void completeSession_AggregatesScoreCorrectly() {
                Question question1 = Question.builder()
                                .id(UUID.randomUUID())
                                .questionType(QuestionType.MCQ)
                                .answerOptions(new ArrayList<>())
                                .shortAnswerKeys(new ArrayList<>())
                                .points(4)
                                .build();
                Question question2 = Question.builder()
                                .id(UUID.randomUUID())
                                .questionType(QuestionType.SHORT_ANSWER)
                                .answerOptions(new ArrayList<>())
                                .shortAnswerKeys(new ArrayList<>())
                                .points(6)
                                .build();

                when(questionRepository.findByQuizIdOrderByOrderIndex(quiz.getId()))
                                .thenReturn(Arrays.asList(question1, question2));

                SessionAnswer sa1 = SessionAnswer.builder().question(question1).isCorrect(true).build();
                SessionAnswer sa2 = SessionAnswer.builder().question(question2).isCorrect(false).build();

                session.getSessionAnswers().add(sa1);
                session.getSessionAnswers().add(sa2);

                when(sessionRepository.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));

                QuizResultResponse result = quizSessionService.completeSession(quizCode, sessionId);

                assertTrue(session.isCompleted());
                assertEquals(4, session.getTotalScore());
                assertEquals(10, session.getMaxScore());
                assertEquals(40.0, result.getPercentageScore());
                assertFalse(result.isPassed());
        }
}
