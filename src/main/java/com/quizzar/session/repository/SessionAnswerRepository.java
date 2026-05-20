package com.quizzar.session.repository;

import com.quizzar.session.entity.SessionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface SessionAnswerRepository extends JpaRepository<SessionAnswer, UUID> {
    List<SessionAnswer> findByQuestionIdAndSessionQuizId(UUID questionId, UUID quizId);
}
