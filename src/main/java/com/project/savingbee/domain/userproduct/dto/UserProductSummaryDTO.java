package com.project.savingbee.domain.userproduct.dto;

import lombok.*;

import java.math.BigDecimal;

// 요약 정보 DTO
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProductSummaryDTO {

  private long totalProducts; // 전체 보유 상품 수
  private long activeProducts; // 활성 상품 수
  private long nearMaturityProducts; // 만기 임박 상품 수
  private BigDecimal totalDepositAmount; // 총 가입 금액
  private BigDecimal averageInterestRate; // 평균 금리
  private BigDecimal totalExpectedInterest; // 총 예상 이자
}
