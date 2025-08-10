package com.project.savingbee.common.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

import java.math.BigDecimal;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsInterestRates {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long rateId; // 금리옵션 고유ID

  private String intrRateType; // 이자율유형(S:단리, M:복리)

  private String rsrvType; // 적립유형(S:정액적립식, F:자유적립식)

  private Integer saveTrm; // 저축기간(월)

  @Column(precision = 5, scale = 2)
  private BigDecimal intrRate; // 기본금리(%)

  @Column(precision = 5, scale = 2)
  private BigDecimal intrRate2; // 최고우대금리(%)

  @CreationTimestamp
  private LocalDateTime createdAt; // 등록일시

  @UpdateTimestamp
  private LocalDateTime updatedAt; // 수정일시

  private String finPrdtCd; // 금융상품 코드

  private BigDecimal monthlyLimitMin; // 월 최소 적립 금액

  private BigDecimal monthlyLimitMax; // 월 최대 적립 금액

  // 외래키 관계
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "finPrdtCd", referencedColumnName = "finPrdtCd", insertable = false, updatable = false)
  private SavingsProducts savingsProduct; // 적금 상품
}
