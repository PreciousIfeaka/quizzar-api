package com.quizzar.quiz.entity;

import com.quizzar.question.entity.Question;
import com.quizzar.teacher.entity.Teacher;
import com.quizzar.document.entity.UploadedDocument;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity 
@Table(name = "quizzes") 
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class Quiz {
    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;
    
    @Column(nullable = false) 
    private String title;
    
    private String description;
    
    @Column(name = "quiz_code", nullable = false, unique = true)
    private String quizCode;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "timing_mode", length = 20, nullable = false)
    @Builder.Default
    private TimingMode timingMode = TimingMode.NONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "quiz_mode", length = 20, nullable = false)
    @Builder.Default
    private QuizMode quizMode = QuizMode.OVERALL;
    
    @Column(name = "timer_value_seconds") 
    private Integer timerValueSeconds;
    
    @Column(name = "ai_suggested_time_seconds") 
    private Integer aiSuggestedTimeSeconds;
    
    @Column(name = "ai_suggested_timing_mode") 
    private String aiSuggestedTimingMode;
    
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<Question> questions = new ArrayList<>();

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UploadedDocument> documents = new ArrayList<>();

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<com.quizzar.session.entity.QuizSession> sessions = new ArrayList<>();
    
    @CreationTimestamp 
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
    
    @UpdateTimestamp 
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
