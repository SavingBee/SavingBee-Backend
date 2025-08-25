package com.project.savingbee.domain.product.dto;

import com.project.savingbee.common.entity.UserProduct;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 상품 가입 요청 DTO
 * 사용자가 예적금 상품에 가입할 때 필요한 정보
 */
@Getter
@Setter
public class ProductJoinRequestDTO {

    public interface JoinGroup {} // 상품 가입시

    @NotBlank(groups = {JoinGroup.class}, message = "상품 코드는 필수입니다")
    private String productCode; // 금융상품코드 (예: finPrdtCd)

    @NotNull(groups = {JoinGroup.class}, message = "상품 유형은 필수입니다")
    private UserProduct.ProductType productType; // DEPOSIT 또는 SAVINGS

    @NotNull(groups = {JoinGroup.class}, message = "가입 금액은 필수입니다")
    @DecimalMin(value = "1", message = "가입 금액은 1원 이상이어야 합니다")
    @DecimalMax(value = "999999999999999", message = "가입 금액이 너무 큽니다")
    private BigDecimal depositAmount; // 가입 금액

    @NotNull(groups = {JoinGroup.class}, message = "가입 기간은 필수입니다")
    @Min(value = 1, message = "가입 기간은 1개월 이상이어야 합니다")
    @Max(value = 120, message = "가입 기간은 120개월 이하여야 합니다")
    private Integer termMonths; // 가입 기간 (개월)

    @NotNull(groups = {JoinGroup.class}, message = "가입일은 필수입니다")
    private LocalDate joinDate; // 가입일 (보통 오늘 날짜)

    @DecimalMin(value = "0.01", message = "금리는 0.01% 이상이어야 합니다")
    @DecimalMax(value = "50.00", message = "금리는 50% 이하여야 합니다")
    private BigDecimal interestRate; // 가입 금리 (선택사항, 자동 계산 가능)

    private String specialConditions; // 우대 조건 (선택사항)

    private Long cartId; // 장바구니 ID (장바구니에서 가입하는 경우)
}
