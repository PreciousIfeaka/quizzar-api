package com.quizzar.question.repository;

import com.quizzar.question.entity.ShortAnswerKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ShortAnswerKeyRepository extends JpaRepository<ShortAnswerKey, UUID> {
}
