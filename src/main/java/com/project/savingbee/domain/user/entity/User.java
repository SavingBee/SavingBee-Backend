package com.project.savingbee.domain.user.entity;

import com.project.savingbee.common.entity.ProductAlertSetting;
import com.project.savingbee.common.entity.UserProduct;
import com.project.savingbee.domain.user.dto.UserRequestDTO;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "users" )
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id; // 사용자 ID

  @Column(name = "username", unique = true, nullable = false, updatable = false)
  private String username; //유저 이름

  @Column(name = "password", nullable = false)
  private String password;// 비밀번호

  @Column(name = "is_lock", nullable = false)
  private Boolean isLock; // 계정이 잠겼는지 아닌지 체크

  @Column(name = "is_social", nullable = false)
  private Boolean isSocial;// 이 계정이 소셜로그인 계정인지 자체로그인 계정인지를 체크

  @Enumerated(EnumType.STRING)
  @Column(name = "social_provider_type")
  private SocialProviderType socialProviderType; //소셜로그인을 통해 가입한 계정이면 이 컬럼에 해당 제공자 정보 저장, 자체 회원가입 계정이면 null일 수도 있음.

  @Enumerated(EnumType.STRING)
  @Column(name = "role_type", nullable = false)
  private UserRoleType roleType; //스프링 시큐리티에서 사용할 특정한 유저의 role값.어드민인지 일반 유저인지

  @Column(name = "nickname")
  private String nickname;

  @Column(name = "email")
  private String email;

  @Column(name = "alarm")
  private Boolean alarm; // 알림 ON/OFF

  @CreatedDate
  @Column(name = "created_date", updatable = false)
  private LocalDateTime createdDate; //생성된 날짜

  @LastModifiedDate
  @Column(name = "updated_date")
  private LocalDateTime updatedDate; //최종 수정된 날짜

  // 연관관계
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<UserProduct> userProducts; // 사용자가 등록한 상품들

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<ProductAlertSetting> productAlertSettings; // 상품 알림 설정들

  public void updateUser(UserRequestDTO dto) {
    this.email = dto.getEmail();
    this.nickname = dto.getNickname();
  }
}