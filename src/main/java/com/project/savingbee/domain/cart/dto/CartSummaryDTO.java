package com.project.savingbee.domain.cart.dto;

import lombok.*;
import java.math.BigDecimal;

/**
 * 장바구니 요약/통계 정보를 위한 DTO
 * 장바구니에 담긴 상품들의 통계 데이터 제공
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartSummaryDTO {
    private long totalCount; // 총 담은 상품 수
    private long depositCount; // 예금 상품 수
    private long savingsCount; // 적금 상품 수
    private BigDecimal maxInterestRate; // 최고 금리
    private String maxInterestProductName; // 최고 금리 상품명
    private BigDecimal avgInterestRate; // 평균 금리
}

