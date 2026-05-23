package com.quizzar.auth.controller;

import com.quizzar.auth.service.TeacherProvisioningService;
import com.quizzar.common.dto.ApiResponse;
import com.quizzar.teacher.dto.TeacherProfileResponse;
import com.quizzar.teacher.entity.Teacher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final TeacherProvisioningService provisioningService;

    @PostMapping("/me")
    public ApiResponse<TeacherProfileResponse> getMe(@AuthenticationPrincipal Jwt jwt) {
        log.info("Received profile lookup/provisioning request for keycloak subject: {}", jwt.getSubject());
        Teacher teacher = provisioningService.provisionTeacher(jwt);
        TeacherProfileResponse response = TeacherProfileResponse.builder()
            .id(teacher.getId())
            .keycloakSubject(teacher.getKeycloakSubject())
            .email(teacher.getEmail())
            .name(teacher.getName())
            .build();
        return ApiResponse.ok(response);
    }
}
