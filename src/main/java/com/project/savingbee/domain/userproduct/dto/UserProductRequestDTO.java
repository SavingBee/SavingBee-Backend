package com.project.savingbee.domain.userproduct.dto;

import com.project.savingbee.common.entity.UserProduct;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 사용자 보유 예적금 상품 등록/수정 요청을 위한 DTO
 * 실제 보유 상품 정보 입력 시 사용
 */
@Getter
@Setter
public class UserProductRequestDTO {
    
    // 검증 그룹 정의
    public interface CreateGroup {}
    public interface UpdateGroup {}
    public interface DeleteGroup {}
    
    // 사용자명 (인증 정보에서 자동 설정)
    private String username;
    
    @NotBlank(groups = {CreateGroup.class, UpdateGroup.class})
    @Size(max = 100)
    private String bankName; // 은행명
    
    @NotBlank(groups = {CreateGroup.class, UpdateGroup.class})
    @Size(max = 200)
    private String productName; // 상품명
    
    @NotNull(groups = {CreateGroup.class, UpdateGroup.class})
    private UserProduct.ProductType productType; // 상품 유형
    
    @NotNull(groups = {CreateGroup.class, UpdateGroup.class})
    @DecimalMin(value = "0.01", message = "금리는 0.01% 이상이어야 합니다")
    @DecimalMax(value = "99.99", message = "금리는 99.99% 이하여야 합니다")
    @Digits(integer = 2, fraction = 2)
    private BigDecimal interestRate; // 가입 금리
    
    @NotNull(groups = {CreateGroup.class, UpdateGroup.class})
    @DecimalMin(value = "1", message = "가입 금액은 1원 이상이어야 합니다")
    private BigDecimal depositAmount; // 가입 금액
    
    @NotNull(groups = {CreateGroup.class, UpdateGroup.class})
    @Min(value = 1, message = "가입 기간은 1개월 이상이어야 합니다")
    @Max(value = 120, message = "가입 기간은 120개월 이하여야 합니다")
    private Integer termMonths; // 가입 기간 (개월)
    
    @NotNull(groups = {CreateGroup.class, UpdateGroup.class})
    @PastOrPresent(message = "가입일은 현재 날짜 이전이어야 합니다")
    private LocalDate joinDate; // 가입일
    
    @NotNull(groups = {CreateGroup.class, UpdateGroup.class})
    @Future(message = "만기일은 미래 날짜여야 합니다")
    private LocalDate maturityDate; // 만기일
    
    @Size(max = 500)
    private String specialConditions; // 우대 조건
    
    // 필터링용 파라미터
    private UserProduct.ProductType filterProductType; // 상품 타입 필터
    private String filterBankName; // 은행명 필터
    private Boolean includeInactive = false; // 비활성 상품 포함 여부
    
    // 페이징 파라미터
    private Integer page = 0; // 페이지 번호
    private Integer size = 10; // 페이지 크기
    private String sortBy = "createdAt"; // 정렬 기준
    private String sortDirection = "DESC"; // 정렬 방향
}
