package com.project.savingbee.api;

import com.project.savingbee.domain.userproduct.dto.UserProductRequestDTO;
import com.project.savingbee.domain.userproduct.dto.UserProductResponseDTO;
import com.project.savingbee.domain.userproduct.dto.UserProductSummaryDTO;
import com.project.savingbee.domain.userproduct.service.UserProductService;
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
 * 사용자 보유 예적금 상품 관리 API 컨트롤러
 * 실제 보유 상품 등록, 조회, 수정, 삭제 기능 제공
 */
@RestController
@RequestMapping("/api/user-products")
@RequiredArgsConstructor
@Slf4j
public class UserProductController {
    
    private final UserProductService userProductService;
    
    /**
     * 1. 사용자가 실제 보유하고 있는 예적금 관련 정보 등록
     * 예) "KB국민은행 / 정기예금 / 가입 기간 12개월 / 금리 3.2% / 가입일 2025-10-01 / 만기일 2026-10-01"
     */
    @PostMapping
    public ResponseEntity<UserProductResponseDTO> registerUserProduct(
            @Validated(UserProductRequestDTO.CreateGroup.class) @RequestBody UserProductRequestDTO request) {
        Long userId = getCurrentUserId();
        UserProductResponseDTO response = userProductService.registerUserProduct(userId, request);
        return ResponseEntity.status(201).body(response);
    }
    
    /**
     * 사용자 보유 상품 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<UserProductResponseDTO>> getUserProducts(
            @ModelAttribute UserProductRequestDTO request) {
        Long userId = getCurrentUserId();
        List<UserProductResponseDTO> response = userProductService.getUserProductsList(userId, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 단일 보유 상품 조회
     */
    @GetMapping("/{userProductId}")
    public ResponseEntity<UserProductResponseDTO> getUserProduct(@PathVariable Long userProductId) {
        Long userId = getCurrentUserId();
        UserProductResponseDTO response = userProductService.getUserProductById(userId, userProductId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 사용자 보유 상품 수정
     */
    @PutMapping("/{userProductId}")
    public ResponseEntity<UserProductResponseDTO> updateUserProduct(
            @PathVariable Long userProductId,
            @Validated(UserProductRequestDTO.UpdateGroup.class) @RequestBody UserProductRequestDTO request) {
        Long userId = getCurrentUserId();
        UserProductResponseDTO response = userProductService.updateUserProductById(userId, userProductId, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 사용자 보유 상품 삭제 (비활성화)
     */
    @DeleteMapping("/{userProductId}")
    public ResponseEntity<Map<String, String>> deleteUserProduct(@PathVariable Long userProductId) {
        Long userId = getCurrentUserId();
        userProductService.deleteUserProductById(userId, userProductId);
        return ResponseEntity.ok(Map.of("message", "보유 상품이 삭제되었습니다."));
    }
    
    /**
     * 사용자 보유 상품 요약 정보
     */
    @GetMapping("/summary")
    public ResponseEntity<UserProductSummaryDTO> getUserProductSummary() {
        Long userId = getCurrentUserId();
        UserProductSummaryDTO response = userProductService.getUserProductSummary(userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 만기 임박 상품 조회
     */
    @GetMapping("/maturity/{daysBefore}")
    public ResponseEntity<List<UserProductResponseDTO>> getMaturityProducts(@PathVariable int daysBefore) {
        List<UserProductResponseDTO> response = userProductService.getMaturityProducts(daysBefore);
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
