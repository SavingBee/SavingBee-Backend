package com.project.savingbee.api;

import com.project.savingbee.domain.product.dto.ProductJoinRequestDTO;
import com.project.savingbee.domain.product.dto.ProductJoinResponseDTO;
import com.project.savingbee.domain.product.service.ProductJoinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 상품 가입 API 컨트롤러
 * 예적금 상품 가입 처리
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductJoinController {

    private final ProductJoinService productJoinService;

    /**
     * 상품 가입
     * 예적금 상품에 가입하여 보유 상품으로 등록
     */
    @PostMapping("/join")
    public ResponseEntity<ProductJoinResponseDTO> joinProduct(
            @Validated(ProductJoinRequestDTO.JoinGroup.class) @RequestBody ProductJoinRequestDTO request) {
        Long userId = getCurrentUserId();
        ProductJoinResponseDTO response = productJoinService.joinProduct(userId, request);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * 특정 상품 가입 (상품 코드로)
     * URL에 상품 코드를 포함하는 방식
     */
    @PostMapping("/{productCode}/join")
    public ResponseEntity<ProductJoinResponseDTO> joinProductByCode(
            @PathVariable String productCode,
            @Validated(ProductJoinRequestDTO.JoinGroup.class) @RequestBody ProductJoinRequestDTO request) {
        // URL의 상품 코드로 설정
        request.setProductCode(productCode);
        
        Long userId = getCurrentUserId();
        ProductJoinResponseDTO response = productJoinService.joinProduct(userId, request);
        return ResponseEntity.status(201).body(response);
    }

    // 현재 사용자 ID 조회 헬퍼 메서드
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }
        
        // 실제 프로젝트에서는 JWT 토큰에서 사용자 ID를 추출하는 로직 필요
        // 현재는 임시로 username을 Long으로 변환 (실제 구현 시 수정 필요)
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("사용자 ID를 확인할 수 없습니다.");
        }
    }
}
