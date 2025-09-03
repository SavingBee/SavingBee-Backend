package com.project.savingbee.domain.recommendation.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

// 추천 요약 DTO
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationSummaryDTO {

  private int totalRecommendations; // 총 추천 상품 수
  private BigDecimal maxPotentialGain; // 최대 예상 수익 증가
  private BigDecimal totalPotentialGain; // 총 예상 수익 증가
  private List<RecommendationResponseDTO> topRecommendations; // 상위 추천 상품들
  private String summaryMessage; // 요약 메시지
}
