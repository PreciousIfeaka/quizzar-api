package com.quizzar.question.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity 
@Table(name = "answer_options")
@Getter
@Setter
@NoArgsConstructor 
@AllArgsConstructor
@Builder
public class AnswerOption {
    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
    
    @Column(name = "option_text", nullable = false) 
    private String optionText;
    
    @Column(name = "is_correct", nullable = false) 
    @Builder.Default
    private boolean isCorrect = false;
    
    @Column(name = "option_label", nullable = false) 
    private String optionLabel;
}
