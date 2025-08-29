package com.project.savingbee.domain.user.repository;

import com.project.savingbee.domain.user.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    Optional<PasswordResetToken> findByUsernameAndVerificationCodeAndIsUsedFalseAndExpiresAtAfter(
        String username, String verificationCode, LocalDateTime now);
    
    Optional<PasswordResetToken> findByUsernameAndIsVerifiedTrueAndIsUsedFalseAndExpiresAtAfter(
        String username, LocalDateTime now);
    
    void deleteByUsernameAndEmail(String username, String email);
    
    void deleteByExpiresAtBefore(LocalDateTime now); // 만료된 토큰 삭제용
}
