package com.quizzar.document.entity;

import com.quizzar.quiz.entity.Quiz;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity 
@Table(name = "uploaded_documents")
@NoArgsConstructor 
@AllArgsConstructor
@Getter
@Setter
@Builder
public class UploadedDocument {
    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;
    
    @Column(name = "s3_key", nullable = false) 
    private String s3Key;
    
    @Column(name = "original_filename", nullable = false) 
    private String originalFilename;
    
    @Column(name = "content_type", nullable = false) 
    private String contentType;
    
    @Column(name = "size_bytes", nullable = false) 
    private Long sizeBytes;
    
    @CreationTimestamp 
    @Column(name = "uploaded_at", updatable = false)
    private OffsetDateTime uploadedAt;
}
