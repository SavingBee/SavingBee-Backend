package com.project.savingbee.domain.product.dto;

import com.project.savingbee.common.entity.UserProduct;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 상품 가입 응답 DTO
 * 상품 가입 완료 후 반환되는 정보
 */
@Getter
@Builder
public class ProductJoinResponseDTO {
    
    private Long userProductId; // 보유 상품 ID
    private String bankName; // 은행명
    private String productName; // 상품명
    private UserProduct.ProductType productType; // 상품 유형
    private BigDecimal interestRate; // 가입 금리
    private BigDecimal depositAmount; // 가입 금액
    private Integer termMonths; // 가입 기간
    private LocalDate joinDate; // 가입일
    private LocalDate maturityDate; // 만기일
    private String specialConditions; // 우대 조건
    private LocalDateTime createdAt; // 가입 처리 시간
    
    /**
     * UserProduct 엔티티로부터 DTO 생성
     */
    public static ProductJoinResponseDTO from(UserProduct userProduct) {
        return ProductJoinResponseDTO.builder()
                .userProductId(userProduct.getUserProductId())
                .bankName(userProduct.getBankName())
                .productName(userProduct.getProductName())
                .productType(userProduct.getProductType())
                .interestRate(userProduct.getInterestRate())
                .depositAmount(userProduct.getDepositAmount())
                .termMonths(userProduct.getTermMonths())
                .joinDate(userProduct.getJoinDate())
                .maturityDate(userProduct.getMaturityDate())
                .specialConditions(userProduct.getSpecialConditions())
                .createdAt(userProduct.getCreatedAt())
                .build();
    }
}
