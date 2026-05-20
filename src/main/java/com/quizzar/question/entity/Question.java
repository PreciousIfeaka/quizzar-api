package com.quizzar.question.entity;

import com.quizzar.quiz.entity.Quiz;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity 
@Table(name = "questions")
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
@Getter
@Setter
public class Question {
    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;
    
    @Column(name = "question_text", nullable = false) 
    private String questionText;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", length = 20, nullable = false)
    private QuestionType questionType;
    
    @Column(name = "order_index", nullable = false) 
    private Integer orderIndex;
    
    @Column(nullable = false) 
    @Builder.Default
    private Integer points = 1;
    
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AnswerOption> answerOptions = new ArrayList<>();
    
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ShortAnswerKey> shortAnswerKeys = new ArrayList<>();
    
    @CreationTimestamp 
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
