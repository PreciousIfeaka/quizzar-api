package com.quizzar.teacher.controller;

import com.quizzar.auth.util.SecurityUtils;
import com.quizzar.common.dto.ApiResponse;
import com.quizzar.storage.dto.PresignedUrlResponse;
import com.quizzar.storage.service.S3StorageService;
import com.quizzar.teacher.dto.TeacherProfileResponse;
import com.quizzar.teacher.dto.UpdateProfileRequest;
import com.quizzar.teacher.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
@Slf4j
public class TeacherController {

    private final TeacherService teacherService;
    private final S3StorageService s3StorageService;
    private final SecurityUtils securityUtils;

    @PutMapping("/profile")
    public ApiResponse<TeacherProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        UUID teacherId = securityUtils.getCurrentTeacherId();
        log.info("Received update profile request for teacher: {}", teacherId);
        TeacherProfileResponse profile = teacherService.updateProfile(teacherId, request);
        return ApiResponse.ok(profile);
    }

    @PostMapping("/avatar/presigned-url")
    public ApiResponse<PresignedUrlResponse> getAvatarUploadUrl(
            @RequestParam("filename") String filename,
            @RequestParam("contentType") String contentType) {
        UUID teacherId = securityUtils.getCurrentTeacherId();
        log.info("Received presigned avatar upload URL request for teacher: {} with filename: {} and contentType: {}", 
                teacherId, filename, contentType);
        PresignedUrlResponse response = s3StorageService.generatePresignedUploadUrl(teacherId, filename, contentType, true);
        return ApiResponse.ok(response);
    }
}
