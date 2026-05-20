package com.quizzar.question.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity 
@Table(name = "short_answer_keys")
@Setter
@Getter
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class ShortAnswerKey {
    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
    
    @Column(name = "accepted_answer", nullable = false) 
    private String acceptedAnswer;
    
    @Column(name = "is_case_sensitive", nullable = false) 
    @Builder.Default
    private boolean isCaseSensitive = false;
}
