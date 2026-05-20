package com.quizzar.session.entity;

import com.quizzar.question.entity.Question;
import com.quizzar.question.entity.AnswerOption;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity 
@Table(name = "session_answers") 
@Getter
@Setter
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class SessionAnswer {
    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private QuizSession session;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id") 
    private AnswerOption selectedOption;
    
    @Column(name = "answer_text") 
    private String answerText;
    
    @Column(name = "is_correct") 
    private Boolean isCorrect;
    
    @Column(name = "time_taken_seconds") 
    private Integer timeTakenSeconds;
}
