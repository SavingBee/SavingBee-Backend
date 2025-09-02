package com.project.savingbee.api;

import com.project.savingbee.domain.cart.dto.CartPageResponseDTO;
import com.project.savingbee.domain.cart.dto.CartRequestDTO;
import com.project.savingbee.domain.cart.dto.CartResponseDTO;
import com.project.savingbee.domain.cart.service.CartService;
import com.project.savingbee.domain.user.entity.UserEntity;
import com.project.savingbee.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


import java.util.Map;

/**
 * 장바구니 관리 API 컨트롤러
 * 관심 상품 담기, 조회, 삭제 기능 제공
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {
    
    private final CartService cartService;
    private final UserRepository userRepository;
    
    /**
     * 1. 목록조회(필터/페이징): 사용자의 장바구니 상품 목록 조회
     * - 은행명 필터링 지원
     * - 페이징 처리
     */
    @GetMapping
    public ResponseEntity<CartPageResponseDTO> getCartItems(
            @ModelAttribute CartRequestDTO request) {
        log.info("=== Cart GET Request Debug ===");
        log.info("Request parameters: bankName={}, page={}, size={}", 
                request.getBankName(), request.getPage(), request.getSize());
        
        Long userId = getCurrentUserId();
        log.info("Retrieved userId: {}", userId);
        
        CartPageResponseDTO response = cartService.getCartItems(userId, request);
        log.info("Response: totalElements={}, contentSize={}", 
                response.getTotalElements(), response.getContent().size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 2. 담기: 상품을 장바구니에 추가
     * - 중복 상품 체크
     * - 상품 정보 자동 조회 및 저장
     */
    @PostMapping
    public ResponseEntity<CartResponseDTO> addToCart(
            @Validated(CartRequestDTO.AddGroup.class) @RequestBody CartRequestDTO request) {
        try {
            log.info("Cart add request: productCode={}, productType={}", 
                    request.getProductCode(), request.getProductType());
            
            Long userId = getCurrentUserId();
            log.info("Current user ID: {}", userId);
            
            CartResponseDTO response = cartService.addToCart(userId, request);
            log.info("Cart item added successfully: cartId={}", response.getCartId());
            
            return ResponseEntity.status(201).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request for adding to cart: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while adding to cart", e);
            throw e;
        }
    }
    
    /**
     * 3. 삭제(단건): 개별 상품 삭제
     */
    @DeleteMapping("/{cartId}")
    public ResponseEntity<Map<String, String>> removeFromCart(@PathVariable Long cartId) {
        Long userId = getCurrentUserId();
        cartService.removeFromCart(userId, cartId);
        return ResponseEntity.ok(Map.of("message", "장바구니에서 상품이 삭제되었습니다."));
    }
    
    /**
     * 4. 선택 삭제(복수): 여러 상품 일괄 삭제
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
    

    
    // 현재 사용자 ID 조회 헬퍼 메서드
    private Long getCurrentUserId() {
        log.info("=== getCurrentUserId Debug ===");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Authentication object: {}", authentication);
        log.info("Is authenticated: {}", authentication != null ? authentication.isAuthenticated() : "null");
        
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("Authentication failed - authentication is null or not authenticated");
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }
        
        String username = authentication.getName();
        log.info("Current username from JWT: {}", username);
        log.info("Authentication principal: {}", authentication.getPrincipal());
        log.info("Authentication authorities: {}", authentication.getAuthorities());
        
        // username으로 데이터베이스에서 사용자 조회
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found with username: {}", username);
                    return new IllegalStateException("사용자를 찾을 수 없습니다. Username: " + username);
                });
        
        log.info("Found user with ID: {}, username: {}", user.getUserId(), user.getUsername());
        return user.getUserId();
    }
}
