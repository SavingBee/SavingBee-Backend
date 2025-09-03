package com.project.savingbee.common.entity;

import com.project.savingbee.domain.user.entity.UserEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 사용자가 실제 보유하고 있는 예적금 상품 정보를 저장하는 엔티티 추천 시스템의 기준이 되는 핵심 데이터
 */
@Entity
@Table(name = "user_product")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProduct {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_product_id", insertable = false, updatable = false)
  private Long userProductId; // 보유 상품 고유 ID

  @Column(name = "user_id")
  private Long userId; // 사용자 ID

  private String bankName; // 은행명

  private String productName; // 상품명

  @Enumerated(EnumType.STRING)
  private ProductType productType; // 상품 유형 (DEPOSIT/SAVINGS)

  @Column(precision = 5, scale = 2)
  private BigDecimal interestRate; // 사용자 가입 금리

  @Column(precision = 15, scale = 0)
  private BigDecimal depositAmount; // 가입 금액

  private Integer termMonths; // 가입 기간 (개월)

  private LocalDate joinDate; // 상품 가입일

  private LocalDate maturityDate; // 상품 만기일

  private String specialConditions; // 우대 조건

  private Boolean isActive; // 활성 상태 (만료 여부)

  @CreationTimestamp
  private LocalDateTime createdAt; // 등록 시간

  @UpdateTimestamp
  private LocalDateTime updatedAt; // 수정 시간

  // 외래키 관계
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", referencedColumnName = "user_id", insertable = false, updatable = false)
  private UserEntity userEntity; // 사용자

  // 연관관계
  @OneToMany(mappedBy = "userProduct", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<MaturityAlarmLog> alarmLogs; // 만기 알림 로그들

  // 상품 타입 Enum
  public enum ProductType {
    DEPOSIT, SAVINGS
  }
}
