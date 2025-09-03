package com.project.savingbee.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "signup_verification_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupVerificationToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "email", nullable = false)
  private String email;

  @Column(name = "verification_code", nullable = false)
  private String verificationCode; // 6자리 인증 코드

  @Column(name = "is_used", nullable = false)
  private Boolean isUsed = false; // 사용 여부

  @Column(name = "is_verified", nullable = false)
  private Boolean isVerified = false; // 인증 완료 여부

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt; // 만료 시간 (10분)

  // 회원가입 정보 임시 저장
  @Column(name = "temp_username")
  private String tempUsername;

  @Column(name = "temp_password")
  private String tempPassword;

  @Column(name = "temp_nickname")
  private String tempNickname;
}
