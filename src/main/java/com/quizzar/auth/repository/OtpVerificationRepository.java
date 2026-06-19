package com.quizzar.auth.repository;

import com.quizzar.auth.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, UUID> {
    Optional<OtpVerification> findTopByEmailAndOtpCodeAndPurposeOrderByCreatedAtDesc(
            String email, String otpCode, String purpose);
    void deleteByEmail(String email);
}
