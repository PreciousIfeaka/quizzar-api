package com.quizzar.auth;

import com.quizzar.auth.dto.*;
import com.quizzar.auth.entity.OtpVerification;
import com.quizzar.auth.repository.OtpVerificationRepository;
import com.quizzar.auth.service.AuthService;
import com.quizzar.auth.service.EmailService;
import com.quizzar.auth.service.JwtService;
import com.quizzar.teacher.entity.Teacher;
import com.quizzar.teacher.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private OtpVerificationRepository otpVerificationRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private SignUpRequest signUpRequest;
    private Teacher teacher;

    @BeforeEach
    void setUp() {
        signUpRequest = new SignUpRequest();
        signUpRequest.setName("John Doe");
        signUpRequest.setEmail("john@example.com");
        signUpRequest.setPassword("securePassword");

        teacher = Teacher.builder()
                .id(UUID.randomUUID())
                .name("John Doe")
                .email("john@example.com")
                .passwordHash("hashedPassword")
                .emailVerified(false)
                .build();
    }

    @Test
    void signup_WhenNewUser_CreatesUnverifiedTeacherAndSendsOtp() {
        when(teacherRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(teacherRepository.save(any(Teacher.class))).thenReturn(teacher);

        String result = authService.signup(signUpRequest);

        assertNotNull(result);
        assertTrue(result.contains("Verification OTP"));
        verify(teacherRepository).save(any(Teacher.class));
        verify(otpVerificationRepository).save(any(OtpVerification.class));
        verify(emailService).sendOtpEmail(eq("john@example.com"), anyString(), eq("EMAIL_VERIFICATION"));
    }

    @Test
    void signup_WhenEmailAlreadyExistsAndVerified_ThrowsException() {
        teacher.setEmailVerified(true);
        when(teacherRepository.findByEmail(anyString())).thenReturn(Optional.of(teacher));

        assertThrows(IllegalArgumentException.class, () -> authService.signup(signUpRequest));
    }

    @Test
    void verifyEmail_WhenOtpValid_MarksEmailVerifiedAndReturnsToken() {
        VerifyEmailRequest verifyReq = new VerifyEmailRequest();
        verifyReq.setEmail("john@example.com");
        verifyReq.setOtp("123456");

        OtpVerification otpVerification = OtpVerification.builder()
                .email("john@example.com")
                .otpCode("123456")
                .purpose("EMAIL_VERIFICATION")
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .build();

        when(otpVerificationRepository.findTopByEmailAndOtpCodeAndPurposeOrderByCreatedAtDesc(anyString(), anyString(),
                anyString()))
                .thenReturn(Optional.of(otpVerification));
        when(teacherRepository.findByEmail(anyString())).thenReturn(Optional.of(teacher));
        when(jwtService.generateToken(anyString(), anyString())).thenReturn("jwtToken");

        AuthResponse response = authService.verifyEmail(verifyReq);

        assertNotNull(response);
        assertEquals("jwtToken", response.getAccessToken());
        assertTrue(teacher.isEmailVerified());
        verify(teacherRepository).save(teacher);
        verify(otpVerificationRepository).deleteByEmail("john@example.com");
    }

    @Test
    void signin_WhenCredentialsValidAndVerified_ReturnsToken() {
        SignInRequest signInReq = new SignInRequest();
        signInReq.setEmail("john@example.com");
        signInReq.setPassword("securePassword");

        teacher.setEmailVerified(true);

        when(teacherRepository.findByEmail(anyString())).thenReturn(Optional.of(teacher));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtService.generateToken(anyString(), anyString())).thenReturn("jwtToken");

        AuthResponse response = authService.signin(signInReq);

        assertNotNull(response);
        assertEquals("jwtToken", response.getAccessToken());
    }

    @Test
    void signin_WhenUnverified_ThrowsException() {
        SignInRequest signInReq = new SignInRequest();
        signInReq.setEmail("john@example.com");
        signInReq.setPassword("securePassword");

        when(teacherRepository.findByEmail(anyString())).thenReturn(Optional.of(teacher));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.signin(signInReq));
    }

    @Test
    void forgotPassword_WhenUserExistsAndVerified_SendsOtp() {
        ForgotPasswordRequest forgotReq = new ForgotPasswordRequest();
        forgotReq.setEmail("john@example.com");

        teacher.setEmailVerified(true);
        when(teacherRepository.findByEmail(anyString())).thenReturn(Optional.of(teacher));

        String result = authService.forgotPassword(forgotReq);

        assertNotNull(result);
        verify(otpVerificationRepository).save(any(OtpVerification.class));
        verify(emailService).sendOtpEmail(eq("john@example.com"), anyString(), eq("PASSWORD_RESET"));
    }

    @Test
    void resetPassword_WhenOtpValid_UpdatesPassword() {
        ResetPasswordRequest resetReq = new ResetPasswordRequest();
        resetReq.setEmail("john@example.com");
        resetReq.setOtp("123456");
        resetReq.setNewPassword("newSecurePassword");

        OtpVerification otpVerification = OtpVerification.builder()
                .email("john@example.com")
                .otpCode("123456")
                .purpose("PASSWORD_RESET")
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .build();

        when(otpVerificationRepository.findTopByEmailAndOtpCodeAndPurposeOrderByCreatedAtDesc(anyString(), anyString(),
                anyString()))
                .thenReturn(Optional.of(otpVerification));
        when(teacherRepository.findByEmail(anyString())).thenReturn(Optional.of(teacher));
        when(passwordEncoder.encode("newSecurePassword")).thenReturn("newHashedPassword");

        String result = authService.resetPassword(resetReq);

        assertNotNull(result);
        assertEquals("newHashedPassword", teacher.getPasswordHash());
        verify(teacherRepository).save(teacher);
        verify(otpVerificationRepository).deleteByEmail("john@example.com");
    }
}
