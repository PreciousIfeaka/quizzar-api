package com.quizzar.teacher.service;

import com.quizzar.cache.config.CacheConfig;
import com.quizzar.storage.service.S3StorageService;
import com.quizzar.teacher.dto.TeacherProfileResponse;
import com.quizzar.teacher.dto.UpdateProfileRequest;
import com.quizzar.teacher.entity.Teacher;
import com.quizzar.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final S3StorageService s3StorageService;

    @Cacheable(value = CacheConfig.TEACHER_PROFILE, key = "#subject")
    @Transactional(readOnly = true)
    public TeacherProfileResponse getProfile(String subject) {
        java.util.UUID teacherId = java.util.UUID.fromString(subject);
        Teacher teacher = teacherRepository.findById(teacherId)
            .orElseThrow(() -> new RuntimeException("Teacher not found"));
        
        return TeacherProfileResponse.builder()
            .id(teacher.getId())
            .name(teacher.getName())
            .email(teacher.getEmail())
            .avatarUrl(teacher.getAvatarUrl())
            .build();
    }

    @Transactional(readOnly = true)
    public TeacherProfileResponse getProfileWithPresignedUrl(String subject) {
        TeacherProfileResponse profile = getProfile(subject);
        String avatarUrl = profile.getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.startsWith("http://") && !avatarUrl.startsWith("https://")) {
            avatarUrl = s3StorageService.generatePresignedUrl(avatarUrl);
        }
        return TeacherProfileResponse.builder()
            .id(profile.getId())
            .name(profile.getName())
            .email(profile.getEmail())
            .avatarUrl(avatarUrl)
            .build();
    }

    @Transactional
    @CacheEvict(value = CacheConfig.TEACHER_PROFILE, key = "#teacherId.toString()")
    public TeacherProfileResponse updateProfile(UUID teacherId, UpdateProfileRequest request) {
        Teacher teacher = teacherRepository.findById(teacherId)
            .orElseThrow(() -> new RuntimeException("Teacher not found"));
        
        teacher.setName(request.getName());
        teacher.setAvatarUrl(request.getAvatarUrl());
        teacherRepository.save(teacher);
        
        return getProfileWithPresignedUrl(teacherId.toString());
    }
}
