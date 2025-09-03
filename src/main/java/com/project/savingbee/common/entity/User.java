package com.project.savingbee.common.entity;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id; // 사용자 ID

  private String email; // 로그인 이메일(중복불가)

  private String password; // 암호화된 비밀번호

  private String name; // 사용자 이름

  private String role; // 사용자 권한

  private Boolean alarm; // 알림 ON/OFF

  @CreationTimestamp
  private LocalDateTime createdAt; // 가입일시

  @UpdateTimestamp
  private LocalDateTime updatedAt; // 정보 수정일

  // 연관관계
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<UserProduct> userProducts; // 사용자가 등록한 상품들

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<ProductAlertSetting> productAlertSettings; // 상품 알림 설정들
}