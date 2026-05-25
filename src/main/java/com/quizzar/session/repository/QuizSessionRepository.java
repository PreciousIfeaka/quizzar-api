package com.quizzar.session.repository;

import com.quizzar.session.entity.QuizSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizSessionRepository extends JpaRepository<QuizSession, UUID> {
    List<QuizSession> findByQuizIdAndIsCompletedTrue(UUID quizId);
    int countByQuizId(UUID quizId);
    
    long countByQuizTeacherKeycloakSubject(String keycloakSubject);
    
    List<QuizSession> findByQuizTeacherKeycloakSubjectAndIsCompletedTrue(String keycloakSubject);

    long countDistinctQuizByQuizTeacherKeycloakSubjectAndStartedAtAfter(String keycloakSubject, OffsetDateTime date);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from QuizSession s where s.id = :id")
    Optional<QuizSession> findByIdForUpdate(@Param("id") UUID id);
}
