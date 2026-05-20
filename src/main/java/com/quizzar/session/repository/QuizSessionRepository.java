package com.quizzar.session.repository;

import com.quizzar.session.entity.QuizSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface QuizSessionRepository extends JpaRepository<QuizSession, UUID> {
    List<QuizSession> findByQuizIdAndIsCompletedTrue(UUID quizId);
    int countByQuizId(UUID quizId);
    
    long countByQuizTeacherKeycloakSubject(String keycloakSubject);
    
    List<QuizSession> findByQuizTeacherKeycloakSubjectAndIsCompletedTrue(String keycloakSubject);

    long countDistinctQuizByQuizTeacherKeycloakSubjectAndStartedAtAfter(String keycloakSubject, OffsetDateTime date);
}
