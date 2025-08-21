package com.project.savingbee.api;

import com.project.savingbee.domain.cart.dto.*;
import com.project.savingbee.domain.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 장바구니 관리 API 컨트롤러
 * 관심 상품 담기, 조회, 삭제 및 보유 상품과의 비교 분석 기능 제공
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {
    
    private final CartService cartService;
    
    /**
     * 1. 목록조회(필터/정렬/페이징): 사용자의 장바구니 상품 목록 조회
     */
    @GetMapping
    public ResponseEntity<CartPageResponseDTO> getCartItems(
            @ModelAttribute CartRequestDTO request) {
        Long userId = getCurrentUserId();
        CartPageResponseDTO response = cartService.getCartItems(userId, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 2. 담기: 상품을 장바구니에 추가
     */
    @PostMapping
    public ResponseEntity<CartResponseDTO> addToCart(
            @Validated(CartRequestDTO.AddGroup.class) @RequestBody CartRequestDTO request) {
        Long userId = getCurrentUserId();
        CartResponseDTO response = cartService.addToCart(userId, request);
        return ResponseEntity.status(201).body(response);
    }
    
    /**
     * 3. 삭제(단건): 장바구니에서 해당 항목 제거
     */
    @DeleteMapping("/{cartId}")
    public ResponseEntity<Map<String, String>> removeFromCart(@PathVariable Long cartId) {
        Long userId = getCurrentUserId();
        cartService.removeFromCart(userId, cartId);
        return ResponseEntity.ok(Map.of("message", "장바구니에서 상품이 삭제되었습니다."));
    }
    
    /**
     * 4. 선택 삭제(복수): 선택 항목 일괄 삭제
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Map<String, Object>> removeMultipleFromCart(
            @Validated(CartRequestDTO.DeleteMultipleGroup.class) @RequestBody CartRequestDTO request) {
        Long userId = getCurrentUserId();
        int deletedCount = cartService.removeMultipleFromCart(userId, request);
        return ResponseEntity.ok(Map.of(
                "message", "선택한 상품들이 삭제되었습니다.",
                "deletedCount", deletedCount
        ));
    }
    
    /**
     * 5. 전체 비우기: 장바구니 초기화
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearCart() {
        Long userId = getCurrentUserId();
        int deletedCount = cartService.clearCart(userId);
        return ResponseEntity.ok(Map.of(
                "message", "장바구니가 비워졌습니다.",
                "deletedCount", deletedCount
        ));
    }
    
    /**
     * 6. 요약/통계: 총 담은 상품 수, 최고 금리
     */
    @GetMapping("/summary")
    public ResponseEntity<CartSummaryDTO> getCartSummary() {
        Long userId = getCurrentUserId();
        CartSummaryDTO response = cartService.getCartSummary(userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 7. 보유 상품과 비교: 장바구니 상품 vs 사용자가 등록한 보유 상품 금리/이자 비교(상위 n개)
     */
    @GetMapping("/compare")
    public ResponseEntity<List<CartComparisonDTO>> compareWithUserProducts(
            @RequestParam(defaultValue = "5") Integer topN) {
        Long userId = getCurrentUserId();
        List<CartComparisonDTO> response = cartService.compareWithUserProducts(userId, topN);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 8. 최근 담은 순 정렬
     */
    @GetMapping("/recent")
    public ResponseEntity<List<CartResponseDTO>> getCartItemsByRecent() {
        Long userId = getCurrentUserId();
        List<CartResponseDTO> response = cartService.getCartItemsByRecent(userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 9. 은행명 필터
     */
    @GetMapping("/bank/{bankName}")
    public ResponseEntity<List<CartResponseDTO>> getCartItemsByBank(@PathVariable String bankName) {
        Long userId = getCurrentUserId();
        List<CartResponseDTO> response = cartService.getCartItemsByBank(userId, bankName);
        return ResponseEntity.ok(response);
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
