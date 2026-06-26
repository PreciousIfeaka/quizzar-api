package com.quizzar.auth.controller;

import com.quizzar.auth.dto.*;
import com.quizzar.auth.service.AuthService;
import com.quizzar.auth.service.EmailService;
import com.quizzar.auth.util.SecurityUtils;
import com.quizzar.common.dto.ApiResponse;
import com.quizzar.teacher.dto.TeacherProfileResponse;
import com.quizzar.teacher.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final TeacherService teacherService;
    private final EmailService emailService;
    private final SecurityUtils securityUtils;

    @PostMapping("/signup")
    public ApiResponse<String> signup(@Valid @RequestBody SignUpRequest request) {
        log.info("Received signup request for email: {}", request.getEmail());
        String responseMessage = authService.signup(request);
        return ApiResponse.ok(responseMessage);
    }

    @PostMapping("/verify-email")
    public ApiResponse<AuthResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        log.info("Received verify-email request for email: {}", request.getEmail());
        AuthResponse response = authService.verifyEmail(request);
        return ApiResponse.ok(response);
    }

    @PostMapping("/signin")
    public ApiResponse<AuthResponse> signin(@Valid @RequestBody SignInRequest request) {
        log.info("Received signin request for email: {}", request.getEmail());
        AuthResponse response = authService.signin(request);
        return ApiResponse.ok(response);
    }

    @PostMapping("/google/signin")
    public ApiResponse<AuthResponse> googleSignin(@Valid @RequestBody GoogleTokenRequest request) {
        log.info("Received google id token");
        AuthResponse response = authService.googleSignin(request.getToken());
        return ApiResponse.ok(response);
    }

    @PostMapping("/resend-otp")
    public ApiResponse<String> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        log.info("Received resend-otp request for email: {}", request.getEmail());
        String responseMessage = authService.resendOtp(request);
        return ApiResponse.ok(responseMessage);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Received forgot-password request for email: {}", request.getEmail());
        String responseMessage = authService.forgotPassword(request);
        return ApiResponse.ok(responseMessage);
    }

    @PostMapping("/send-feedback")
    public ApiResponse<String> sendFeedBack(@Valid @RequestBody FeedbackRequest request) {
        log.info("Received app feeback request");

        emailService.sendFeedbackEmail(request, securityUtils.getCurrentEmail());
        return ApiResponse.ok("Successfully sent feedback email");
    }

    @PostMapping("/reset-password")
    public ApiResponse<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Received reset-password request for email: {}", request.getEmail());
        String responseMessage = authService.resetPassword(request);
        return ApiResponse.ok(responseMessage);
    }

    @PostMapping("/change-password")
    public ApiResponse<String> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest request) {
        String email = jwt.getClaimAsString("email");
        log.info("Received change-password request for email: {}", email);
        String responseMessage = authService.changePassword(email, request);
        return ApiResponse.ok(responseMessage);
    }

    @PostMapping("/me")
    public ApiResponse<TeacherProfileResponse> getMe(@AuthenticationPrincipal Jwt jwt) {
        log.info("Received profile lookup request for subject: {}", jwt.getSubject());
        TeacherProfileResponse profile = teacherService.getProfileWithPresignedUrl(jwt.getSubject());
        return ApiResponse.ok(profile);
    }
}
