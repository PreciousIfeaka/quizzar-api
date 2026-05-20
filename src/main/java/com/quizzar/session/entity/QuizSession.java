package com.quizzar.session.entity;

import com.quizzar.quiz.entity.Quiz;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity 
@Table(name = "quiz_sessions") 
@Getter
@Setter
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class QuizSession {
    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;
    
    @Column(name = "student_name", nullable = false) 
    private String studentName;
    
    @Column(name = "started_at", nullable = false) 
    @Builder.Default
    private OffsetDateTime startedAt = OffsetDateTime.now();
    
    @Column(name = "completed_at") 
    private OffsetDateTime completedAt;
    
    @Column(name = "total_score") 
    private Integer totalScore;
    
    @Column(name = "max_score") 
    private Integer maxScore;
    
    @Column(name = "ip_address") 
    private String ipAddress;
    
    @Column(name = "is_completed", nullable = false) 
    @Builder.Default
    private boolean isCompleted = false;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SessionAnswer> sessionAnswers = new ArrayList<>();
}
