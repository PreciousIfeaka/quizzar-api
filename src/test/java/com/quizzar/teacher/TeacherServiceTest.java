package com.quizzar.teacher;

import com.quizzar.storage.service.S3StorageService;
import com.quizzar.teacher.dto.TeacherProfileResponse;
import com.quizzar.teacher.dto.UpdateProfileRequest;
import com.quizzar.teacher.entity.Teacher;
import com.quizzar.teacher.repository.TeacherRepository;
import com.quizzar.teacher.service.TeacherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private S3StorageService s3StorageService;

    @InjectMocks
    private TeacherService teacherService;

    private UUID teacherId;
    private Teacher teacher;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        teacher = Teacher.builder()
                .id(teacherId)
                .name("Jane Doe")
                .email("jane@example.com")
                .avatarUrl("teachers/" + teacherId + "/avatar/profile.jpg")
                .build();
    }

    @Test
    void getProfile_ReturnsProfileWithRawAvatarUrl() {
        when(teacherRepository.findById(teacherId)).thenReturn(Optional.of(teacher));

        TeacherProfileResponse response = teacherService.getProfile(teacherId.toString());

        assertNotNull(response);
        assertEquals(teacherId, response.getId());
        assertEquals("jane@example.com", response.getEmail());
        assertEquals("teachers/" + teacherId + "/avatar/profile.jpg", response.getAvatarUrl());
    }

    @Test
    void getProfileWithPresignedUrl_WhenAvatarUrlIsS3Key_GeneratesPresignedUrl() {
        when(teacherRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(s3StorageService.generatePresignedUrl(anyString())).thenReturn("http://presigned-url.com/avatar.jpg");

        TeacherProfileResponse response = teacherService.getProfileWithPresignedUrl(teacherId.toString());

        assertNotNull(response);
        assertEquals("http://presigned-url.com/avatar.jpg", response.getAvatarUrl());
        verify(s3StorageService).generatePresignedUrl("teachers/" + teacherId + "/avatar/profile.jpg");
    }

    @Test
    void getProfileWithPresignedUrl_WhenAvatarUrlIsHttp_ReturnsItAsIs() {
        teacher.setAvatarUrl("http://external-url.com/avatar.jpg");
        when(teacherRepository.findById(teacherId)).thenReturn(Optional.of(teacher));

        TeacherProfileResponse response = teacherService.getProfileWithPresignedUrl(teacherId.toString());

        assertNotNull(response);
        assertEquals("http://external-url.com/avatar.jpg", response.getAvatarUrl());
        verifyNoInteractions(s3StorageService);
    }

    @Test
    void updateProfile_SavesUpdatedNameAndAvatarUrl() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .name("Jane Smith")
                .avatarUrl("https://external-url.com/avatar2.jpg")
                .build();
        when(teacherRepository.findById(teacherId)).thenReturn(Optional.of(teacher));

        TeacherProfileResponse response = teacherService.updateProfile(teacherId, request);

        assertNotNull(response);
        assertEquals("Jane Smith", response.getName());
        assertEquals("https://external-url.com/avatar2.jpg", response.getAvatarUrl());
        assertEquals("Jane Smith", teacher.getName());
        assertEquals("https://external-url.com/avatar2.jpg", teacher.getAvatarUrl());
        verify(teacherRepository).save(teacher);
    }
}
