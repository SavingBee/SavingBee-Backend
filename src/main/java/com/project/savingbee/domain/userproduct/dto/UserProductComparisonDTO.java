package com.project.savingbee.domain.userproduct.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * 사용자 보유 상품과 추천 상품 비교 결과를 위한 DTO
 * 보유 상품 기준으로 더 나은 조건의 상품들과 비교 분석
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProductComparisonDTO {

  // 기준이 되는 보유 상품 정보
  private Long userProductId; // 보유 상품 ID
  private String baseProductName; // 보유 상품명
  private String baseBankName; // 보유 은행명
  private BigDecimal baseInterestRate; // 보유 상품 금리
  private BigDecimal baseDepositAmount; // 가입 금액
  private Integer baseTermMonths; // 가입 기간

  // 추천 상품들 (RecommendedProductDTO 대신 간단한 내부 클래스 사용)
  private List<RecommendedProduct> recommendedProducts; // 추천 상품 목록

  // 비교 결과 요약
  private int totalRecommendations; // 총 추천 상품 수
  private BigDecimal maxRateDifference; // 최대 금리 차이
  private BigDecimal maxExtraInterest; // 최대 추가 이자
  private String bestRecommendationReason; // 최고 추천 이유

  /**
   * 추천 상품 정보를 담는 내부 클래스
   */
  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RecommendedProduct {

    private String productCode; // 상품 코드
    private String productName; // 상품명
    private String bankName; // 은행명
    private String productType; // 상품 유형 (DEPOSIT/SAVINGS)
    private BigDecimal maxInterestRate; // 최고 금리
    private Integer termMonths; // 기간 (개월)
    private BigDecimal rateDifference; // 기존 상품 대비 금리 차이
    private BigDecimal estimatedExtraInterest; // 예상 추가 이자 (연간)
    private String reason; // 추천 이유
    private Integer priority; // 추천 우선순위 (1이 가장 높음)
  }
}