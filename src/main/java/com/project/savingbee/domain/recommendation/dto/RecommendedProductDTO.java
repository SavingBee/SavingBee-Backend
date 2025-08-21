package com.project.savingbee.domain.recommendation.dto;

import lombok.*;
import java.math.BigDecimal;

/**
 * 추천된 개별 상품 정보를 위한 DTO
 * 상품 상세 정보와 추천 근거 제공
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedProductDTO {

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

    // 비교 기준이 된 사용자 보유 상품 정보
    private String baseProductName; // 기준 상품명
    private BigDecimal baseInterestRate; // 기준 금리
    private BigDecimal baseDepositAmount; // 기준 가입 금액

    /**
     * 추천 우선순위에 따른 등급 반환
     */
    public String getPriorityLevel() {
        if (priority == null) return "NORMAL";

        switch (priority) {
            case 1: return "HIGHEST";
            case 2: return "HIGH";
            case 3: return "MEDIUM";
            default: return "NORMAL";
        }
    }

    /**
     * 금리 차이가 양수인지 확인 (더 유리한 상품인지)
     */
    public boolean isBetterRate() {
        return rateDifference != null && rateDifference.compareTo(BigDecimal.ZERO) > 0;
    }
}