package com.quizzar.teacher.service;

import com.quizzar.cache.config.CacheConfig;
import com.quizzar.teacher.dto.TeacherProfileResponse;
import com.quizzar.teacher.entity.Teacher;
import com.quizzar.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;

    @Cacheable(value = CacheConfig.TEACHER_PROFILE, key = "#keycloakSubject")
    @Transactional(readOnly = true)
    public TeacherProfileResponse getProfile(String keycloakSubject) {
        Teacher teacher = teacherRepository.findByKeycloakSubject(keycloakSubject)
            .orElseThrow(() -> new RuntimeException("Teacher not found"));
        
        return TeacherProfileResponse.builder()
            .id(teacher.getId())
            .name(teacher.getName())
            .email(teacher.getEmail())
            .keycloakSubject(teacher.getKeycloakSubject())
            .build();
    }
}
