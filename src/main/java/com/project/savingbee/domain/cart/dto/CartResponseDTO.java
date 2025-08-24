package com.project.savingbee.domain.cart.dto;

import com.project.savingbee.common.entity.Cart;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 장바구니 응답을 위한 DTO
 * 장바구니 조회 및 비교 분석 결과 표시에 활용
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponseDTO {

    private Long cartId;
    private String productCode;
    private Cart.ProductType productType;
    private String bankName;
    private String productName;
    private BigDecimal maxInterestRate;
    private Integer termMonths;
    private LocalDateTime createdAt;

    // 정적 팩토리 메서드
    public static CartResponseDTO from(Cart cart) {
        return CartResponseDTO.builder()
                .cartId(cart.getCartId())
                .productCode(cart.getProductCode())
                .productType(cart.getProductType())
                .bankName(cart.getBankName())
                .productName(cart.getProductName())
                .maxInterestRate(cart.getMaxInterestRate())
                .termMonths(cart.getTermMonths())
                .createdAt(cart.getCreatedAt())
                .build();
    }
}