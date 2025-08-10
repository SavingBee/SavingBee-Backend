package com.project.savingbee.common.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.*;

import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "user_product")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProduct {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long userId; // 예적금 상품을 등록한 사용자ID

  private String bankName; // 은행명

  private String productName; // 상품명

  @Column(precision = 4, scale = 2)
  private BigDecimal interestRate; // 사용자 가입 금리

  private LocalDate joinDate; // 상품 가입일

  private LocalDate maturityDate; // 상품 만기일

  @CreationTimestamp
  private LocalDateTime createdAt; // 등록 시간

  private Long id; // 사용자 ID

  // 외래키 관계
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id", referencedColumnName = "id", insertable = false, updatable = false)
  private User user; // 사용자

  // 연관관계
  @OneToMany(mappedBy = "userProduct", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<MaturityAlarmLog> alarmLogs; // 만기 알림 로그들
}
