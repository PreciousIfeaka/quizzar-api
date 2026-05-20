package com.quizzar.teacher.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity 
@Table(name = "teachers") 
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class Teacher {
    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "keycloak_subject", nullable = false, unique = true)
    private String keycloakSubject;
    
    @Column(nullable = false) 
    private String email;
    
    @Column(nullable = false) 
    private String name;
    
    @CreationTimestamp 
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
    
    @UpdateTimestamp 
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
