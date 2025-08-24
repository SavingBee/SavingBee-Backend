package com.project.savingbee.domain.userproduct.dto;

import com.project.savingbee.common.entity.UserProduct;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 사용자 보유 예적금 상품 응답을 위한 DTO
 * 보유 상품 조회 및 추천 결과 표시에 활용
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProductResponseDTO {
    
    private Long userProductId;
    private Long userId;
    private String bankName;
    private String productName;
    private UserProduct.ProductType productType;
    private BigDecimal interestRate;
    private BigDecimal depositAmount;
    private Integer termMonths;
    private LocalDate joinDate;
    private LocalDate maturityDate;
    private String specialConditions;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 계산된 필드들
    private Long daysToMaturity; // 만기까지 남은 일수
    private BigDecimal expectedInterest; // 예상 이자
    private String maturityStatus; // 만기 상태 (ACTIVE, NEAR_MATURITY, EXPIRED)
    
    // 정적 팩토리 메서드
    public static UserProductResponseDTO from(UserProduct userProduct) {
        UserProductResponseDTO dto = UserProductResponseDTO.builder()
                .userProductId(userProduct.getUserProductId())
                .userId(userProduct.getUserId())
                .bankName(userProduct.getBankName())
                .productName(userProduct.getProductName())
                .productType(userProduct.getProductType())
                .interestRate(userProduct.getInterestRate())
                .depositAmount(userProduct.getDepositAmount())
                .termMonths(userProduct.getTermMonths())
                .joinDate(userProduct.getJoinDate())
                .maturityDate(userProduct.getMaturityDate())
                .specialConditions(userProduct.getSpecialConditions())
                .isActive(userProduct.getIsActive())
                .createdAt(userProduct.getCreatedAt())
                .updatedAt(userProduct.getUpdatedAt())
                .build();
        
        // 계산된 필드 설정
        dto.calculateDerivedFields();
        return dto;
    }
    
    // 계산된 필드들을 설정하는 메서드
    private void calculateDerivedFields() {
        LocalDate now = LocalDate.now();
        
        // 만기까지 남은 일수 계산
        if (maturityDate != null) {
            this.daysToMaturity = (long) now.until(maturityDate).getDays();
            
            // 만기 상태 설정
            if (daysToMaturity < 0) {
                this.maturityStatus = "EXPIRED";
            } else if (daysToMaturity <= 30) {
                this.maturityStatus = "NEAR_MATURITY";
            } else {
                this.maturityStatus = "ACTIVE";
            }
        }
        
        // 예상 이자 계산 (단순 계산)
        if (depositAmount != null && interestRate != null && termMonths != null) {
            this.expectedInterest = depositAmount
                    .multiply(interestRate)
                    .multiply(new BigDecimal(termMonths))
                    .divide(new BigDecimal(1200), 2, BigDecimal.ROUND_HALF_UP);
        }
    }
    
    public static List<UserProductResponseDTO> fromList(List<UserProduct> userProducts) {
        return userProducts.stream()
                .map(UserProductResponseDTO::from)
                .collect(Collectors.toList());
    }
}

