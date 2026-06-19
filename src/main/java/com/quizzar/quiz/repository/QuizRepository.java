package com.quizzar.quiz.repository;

import com.quizzar.quiz.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, UUID> {
    Optional<Quiz> findByQuizCode(String quizCode);
    boolean existsByQuizCode(String quizCode);
    Page<Quiz> findAllByTeacherId(UUID teacherId, Pageable pageable);
    long countByTeacherId(UUID teacherId);
}
