package com.project.savingbee.common.entity;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;

import com.project.savingbee.domain.user.entity.User;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAlertSetting {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id; // Key

  private Long userId; // 사용자 ID

  private Boolean productTypeDeposit; // 예금

  private Boolean productTypeSaving; // 적금

  @Column(precision = 4, scale = 2)
  private BigDecimal minInterestRate; // 연이율

  private Boolean interestCalcSimple; // 이자계산방식_단리

  private Boolean interestCalcCompound; // 이자계산방식_복리

  private Integer maxSaveTerm; // 예치 기간

  private BigInteger minAmount; // 최소 가입 금액

  private BigInteger maxLimit; // 최대 한도

  private Boolean rsrvTypeFlexible; // 자유적립

  private Boolean rsrvTypeFixed; // 정액적립

  @Enumerated(EnumType.STRING)
  private AlertType alertType; // 알림 유형

  @CreationTimestamp
  private LocalDateTime createdAt; // 등록일시

  @UpdateTimestamp
  private LocalDateTime updatedAt; // 수정일시

  private LocalDateTime lastEvaluatedAt; // 해당 알림 설정이 마지막으로 상품을 비교 확인한 시각

  // 외래키 관계
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "userId", referencedColumnName = "id", insertable = false, updatable = false)
  private User user; // 사용자

  // Enum 정의
  public enum AlertType {
    EMAIL, SMS, PUSH
  }
}
