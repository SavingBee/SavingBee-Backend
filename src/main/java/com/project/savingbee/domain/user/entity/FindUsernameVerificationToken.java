package com.project.savingbee.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "find_username_verification_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FindUsernameVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "verification_code", nullable = false)
    private String verificationCode; // 6자리 인증 코드

    @Column(name = "is_used", nullable = false)
    @Builder.Default
    private Boolean isUsed = false; // 사용 여부

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false; // 인증 완료 여부

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt; // 만료 시간 (10분)

    // 조회된 사용자 정보 임시 저장
    @Column(name = "found_username")
    private String foundUsername; // 찾은 아이디
}
