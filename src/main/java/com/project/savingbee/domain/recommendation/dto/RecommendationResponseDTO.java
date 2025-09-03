package com.project.savingbee.domain.recommendation.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 상품 추천 결과를 위한 DTO
 * 보유 상품 기반 추천 및 비교 분석 결과 표시
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponseDTO {

  private String productCode; // 추천 상품 코드
  private String productName; // 추천 상품명
  private String bankName; // 은행명
  private String productType; // 상품 유형
  private BigDecimal maxInterestRate; // 최고 금리
  private Integer termMonths; // 기간
  private BigDecimal rateDifference; // 기존 상품 대비 금리 차이
  private BigDecimal estimatedExtraInterest; // 예상 추가 이자 (연간)
  private String reason; // 추천 이유
  private Integer priority; // 추천 우선순위

  // 비교 기준이 된 사용자 보유 상품 정보
  private String baseProductName; // 기준 상품명
  private BigDecimal baseInterestRate; // 기준 금리
  private BigDecimal baseDepositAmount; // 기준 가입 금액
}

