package com.quizzar.auth.service;

import com.quizzar.auth.dto.*;
import com.quizzar.auth.entity.OtpVerification;
import com.quizzar.auth.repository.OtpVerificationRepository;
import com.quizzar.teacher.dto.TeacherProfileResponse;
import com.quizzar.teacher.entity.Teacher;
import com.quizzar.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final TeacherRepository teacherRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String signup(SignUpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Optional<Teacher> existingTeacherOpt = teacherRepository.findByEmail(email);

        Teacher teacher;
        if (existingTeacherOpt.isPresent()) {
            Teacher existing = existingTeacherOpt.get();
            if (existing.isEmailVerified()) {
                throw new IllegalArgumentException("Email is already registered");
            }
            // If they registered but didn't verify, allow updating profile and password
            existing.setName(request.getName().trim());
            existing.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            teacher = teacherRepository.save(existing);
            log.info("Updated unverified teacher credentials for: {}", email);
        } else {
            // New user registration
            teacher = Teacher.builder()
                    .name(request.getName().trim())
                    .email(email)
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .emailVerified(false)
                    .build();
            teacher = teacherRepository.save(teacher);
            log.info("Registered new unverified teacher: {}", email);
        }

        sendNewOtp(email, "EMAIL_VERIFICATION");
        return "Verification OTP sent to your email. Please verify to activate your account.";
    }

    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String otp = request.getOtp().trim();

        OtpVerification verification = otpVerificationRepository
                .findTopByEmailAndOtpCodeAndPurposeOrderByCreatedAtDesc(email, otp, "EMAIL_VERIFICATION")
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired OTP"));

        if (verification.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("OTP has expired. Please request a new one.");
        }

        Teacher teacher = teacherRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));

        teacher.setEmailVerified(true);
        teacherRepository.save(teacher);

        // Clean up OTPs for this email
        otpVerificationRepository.deleteByEmail(email);
        log.info("Email verified successfully for teacher: {}", email);

        // Generate token immediately to log user in
        String accessToken = jwtService.generateToken(teacher.getId().toString(), teacher.getEmail());

        TeacherProfileResponse profile = TeacherProfileResponse.builder()
                .id(teacher.getId())
                .name(teacher.getName())
                .email(teacher.getEmail())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .profile(profile)
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse signin(SignInRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Teacher teacher = teacherRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), teacher.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (!teacher.isEmailVerified()) {
            throw new IllegalArgumentException("Email is not verified. Please verify your email first.");
        }

        String accessToken = jwtService.generateToken(teacher.getId().toString(), teacher.getEmail());

        TeacherProfileResponse profile = TeacherProfileResponse.builder()
                .id(teacher.getId())
                .name(teacher.getName())
                .email(teacher.getEmail())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .profile(profile)
                .build();
    }

    @Transactional
    public String resendOtp(ResendOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Teacher teacher = teacherRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email is not registered"));

        if (teacher.isEmailVerified()) {
            throw new IllegalArgumentException("Email is already verified");
        }

        sendNewOtp(email, "EMAIL_VERIFICATION");
        return "A new verification OTP has been sent to your email.";
    }

    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // Return a generic message for security, but send OTP only if teacher exists
        // and is verified
        teacherRepository.findByEmail(email)
                .filter(Teacher::isEmailVerified)
                .ifPresent(teacher -> sendNewOtp(email, "PASSWORD_RESET"));

        return "If the email is registered, a password reset OTP code has been sent.";
    }

    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String otp = request.getOtp().trim();

        OtpVerification verification = otpVerificationRepository
                .findTopByEmailAndOtpCodeAndPurposeOrderByCreatedAtDesc(email, otp, "PASSWORD_RESET")
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired OTP"));

        if (verification.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("OTP has expired. Please request a new one.");
        }

        Teacher teacher = teacherRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));

        teacher.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        teacherRepository.save(teacher);

        otpVerificationRepository.deleteByEmail(email);
        log.info("Password reset successfully for teacher: {}", email);

        return "Password has been reset successfully. You can now sign in.";
    }

    @Transactional
    public String changePassword(String currentEmail, ChangePasswordRequest request) {
        Teacher teacher = teacherRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), teacher.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect current password");
        }

        teacher.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        teacherRepository.save(teacher);
        log.info("Password updated successfully for teacher: {}", currentEmail);

        return "Password changed successfully.";
    }

    private void sendNewOtp(String email, String purpose) {
        // Purge any old OTPs for the email
        otpVerificationRepository.deleteByEmail(email);

        // Generate 6-digit code
        String code = String.format("%06d", secureRandom.nextInt(1000000));

        OtpVerification otpVerification = OtpVerification.builder()
                .email(email)
                .otpCode(code)
                .purpose(purpose)
                .expiresAt(OffsetDateTime.now().plusMinutes(15))
                .build();

        otpVerificationRepository.save(otpVerification);
        log.info("Generated new OTP code for: {}, purpose: {}", email, purpose);

        // Asynchronously or synchronously send mail (we send synchronously here for
        // simplicity)
        emailService.sendOtpEmail(email, code, purpose);
    }
}
