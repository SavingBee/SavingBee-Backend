package com.project.savingbee.domain.user.repository;

import com.project.savingbee.domain.user.entity.FindUsernameVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface FindUsernameVerificationTokenRepository extends JpaRepository<FindUsernameVerificationToken, Long> {
    
    Optional<FindUsernameVerificationToken> findByEmailAndVerificationCodeAndIsUsedFalseAndExpiresAtAfter(
        String email, String verificationCode, LocalDateTime now);
    
    Optional<FindUsernameVerificationToken> findByEmailAndIsVerifiedTrueAndIsUsedFalseAndExpiresAtAfter(
        String email, LocalDateTime now);
    
    void deleteByEmail(String email);
    
    void deleteByExpiresAtBefore(LocalDateTime now); // 만료된 토큰 삭제용
}
