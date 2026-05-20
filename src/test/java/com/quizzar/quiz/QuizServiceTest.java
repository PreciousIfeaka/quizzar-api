package com.quizzar.quiz;

import com.quizzar.common.exception.QuizNotFoundException;
import com.quizzar.common.exception.QuizOwnershipException;
import com.quizzar.document.repository.UploadedDocumentRepository;
import com.quizzar.quiz.entity.Quiz;
import com.quizzar.quiz.repository.QuizRepository;
import com.quizzar.quiz.service.QuizService;
import com.quizzar.storage.service.S3StorageService;
import com.quizzar.teacher.entity.Teacher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QuizServiceTest {

    @Mock
    private QuizRepository quizRepository;
    @Mock
    private UploadedDocumentRepository uploadedDocumentRepository;
    @Mock
    private S3StorageService s3StorageService;

    @InjectMocks
    private QuizService quizService;

    private UUID quizId;
    private String teacherSubject;
    private Quiz quiz;

    @BeforeEach
    void setUp() {
        quizId = UUID.randomUUID();
        teacherSubject = "teacher-123";
        Teacher teacher = Teacher.builder().keycloakSubject(teacherSubject).build();
        quiz = Quiz.builder().id(quizId).teacher(teacher).build();
    }

    @Test
    void getQuizById_WhenExistsAndOwner_ReturnsQuiz() {
        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));
        
        var response = quizService.getQuizById(quizId, teacherSubject);
        
        assertNotNull(response);
        assertEquals(quizId, response.getId());
    }

    @Test
    void getQuizById_WhenNotOwner_ThrowsException() {
        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));
        
        assertThrows(QuizOwnershipException.class, () -> 
            quizService.getQuizById(quizId, "other-teacher"));
    }

    @Test
    void getQuizById_WhenNotFound_ThrowsException() {
        when(quizRepository.findById(quizId)).thenReturn(Optional.empty());
        
        assertThrows(QuizNotFoundException.class, () -> 
            quizService.getQuizById(quizId, teacherSubject));
    }

    @Test
    void deleteQuiz_WhenOwner_DeletesFromS3AndDb() {
        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));
        when(uploadedDocumentRepository.findAllByQuizId(quizId)).thenReturn(java.util.List.of());
        
        quizService.deleteQuiz(quizId, teacherSubject);
        
        verify(quizRepository).delete(quiz);
    }
}
