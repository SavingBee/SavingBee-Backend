package com.project.savingbee.domain.cart.dto;

import com.project.savingbee.common.entity.Cart;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 장바구니 관련 요청을 위한 DTO
 * 상품 담기, 삭제, 필터링 등 장바구니 조작에 사용
 */
@Getter
@Setter
public class CartRequestDTO {
    
    // 검증 그룹 정의
    public interface AddGroup {}
    public interface DeleteGroup {}
    public interface DeleteMultipleGroup {}
    public interface CompareGroup {}
    
    @NotBlank(groups = {AddGroup.class})
    private String productCode; // 상품코드
    
    @NotNull(groups = {AddGroup.class})
    private Cart.ProductType productType; // DEPOSIT or SAVINGS
    
    @NotEmpty(groups = {DeleteMultipleGroup.class})
    private List<Long> cartIds; // 선택 삭제할 장바구니 ID 목록
    
    // 필터링 파라미터
    private String bankName; // 은행명 필터 (선택사항)
    private Cart.ProductType filterProductType; // 상품 타입 필터
    private String sortBy = "recent"; // 정렬 기준 (recent, interest, bank)
    
    // 페이징 파라미터
    private Integer page = 0; // 페이지 번호 (기본값: 0)
    private Integer size = 10; // 페이지 크기 (기본값: 10)
    
    // 비교 분석 파라미터
    private Integer topN = 5; // 비교 시 상위 N개 (기본값: 5)
    private Boolean includeInactive = false; // 비활성 상품 포함 여부
}
