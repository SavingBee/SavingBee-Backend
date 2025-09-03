package com.project.savingbee.domain.cart.dto;

import lombok.*;

import java.math.BigDecimal;

// 비교 결과 DTO
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartComparisonDTO {

  private CartResponseDTO cartProduct; // 장바구니 상품
  private String userProductName; // 사용자 보유 상품명
  private BigDecimal userProductRate; // 사용자 보유 상품 금리
  private BigDecimal rateDifference; // 금리 차이
  private BigDecimal estimatedExtraInterest; // 예상 추가 이자 (연간)
  private String recommendation; // 추천 메시지
  private String comparisonType; // 비교 유형 (BETTER, SAME, WORSE)
}
