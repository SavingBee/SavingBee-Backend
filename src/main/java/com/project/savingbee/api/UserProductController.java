package com.project.savingbee.api;

import com.project.savingbee.domain.userproduct.dto.UserProductRequestDTO;
import com.project.savingbee.domain.userproduct.dto.UserProductResponseDTO;
import com.project.savingbee.domain.userproduct.dto.UserProductSummaryDTO;
import com.project.savingbee.domain.userproduct.service.UserProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

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
            @Validated(UserProductRequestDTO.CreateGroup.class) @RequestBody UserProductRequestDTO request,
            @RequestParam(defaultValue = "1") Long userId) {
        UserProductResponseDTO response = userProductService.registerUserProduct(userId, request);
        return ResponseEntity.status(201).body(response);
    }
    
    /**
     * 사용자 보유 상품 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<UserProductResponseDTO>> getUserProducts(
            @ModelAttribute UserProductRequestDTO request,
            @RequestParam(defaultValue = "1") Long userId) {
        log.info("보유 상품 목록 조회 요청 - userId: {}, filters: {}", userId, request);
        List<UserProductResponseDTO> response = userProductService.getUserProductsList(userId, request);
        log.info("조회 결과 - 상품 개수: {}", response.size());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 단일 보유 상품 조회
     */
    @GetMapping("/{userProductId}")
    public ResponseEntity<UserProductResponseDTO> getUserProduct(
            @PathVariable Long userProductId,
            @RequestParam(defaultValue = "1") Long userId) {
        log.info("단일 보유 상품 조회 요청 - userProductId: {}, userId: {}", userProductId, userId);
        UserProductResponseDTO response = userProductService.getUserProductById(userId, userProductId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 사용자 보유 상품 수정
     */
    @PutMapping("/{userProductId}")
    public ResponseEntity<UserProductResponseDTO> updateUserProduct(
            @PathVariable Long userProductId,
            @Validated(UserProductRequestDTO.PartialUpdateGroup.class) @RequestBody UserProductRequestDTO request,
            @RequestParam(defaultValue = "1") Long userId) {
        log.info("보유 상품 수정 요청 - userProductId: {}, userId: {}", userProductId, userId);
        UserProductResponseDTO response = userProductService.updateUserProductById(userId, userProductId, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 사용자 보유 상품 삭제 (비활성화)
     */
    @DeleteMapping("/{userProductId}")
    public ResponseEntity<Map<String, String>> deleteUserProduct(
            @PathVariable Long userProductId,
            @RequestParam(defaultValue = "1") Long userId) {
        log.info("보유 상품 삭제 요청 - userProductId: {}, userId: {}", userProductId, userId);
        userProductService.deleteUserProductById(userId, userProductId);
        return ResponseEntity.ok(Map.of("message", "보유 상품이 삭제되었습니다."));
    }
    
    /**
     * 사용자 보유 상품 요약 정보
     */
    @GetMapping("/summary")
    public ResponseEntity<UserProductSummaryDTO> getUserProductSummary(
            @RequestParam(defaultValue = "1") Long userId) {
        log.info("보유 상품 요약 정보 조회 요청 - userId: {}", userId);
        UserProductSummaryDTO response = userProductService.getUserProductSummary(userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 만기 임박 상품 조회
     */
    @GetMapping("/maturity/{daysBefore}")
    public ResponseEntity<List<UserProductResponseDTO>> getMaturityProducts(
            @PathVariable int daysBefore,
            @RequestParam(defaultValue = "1") Long userId) {
        log.info("만기 임박 상품 조회 요청 - daysBefore: {}, userId: {}", daysBefore, userId);
        List<UserProductResponseDTO> response = userProductService.getMaturityProducts(daysBefore, userId);
        return ResponseEntity.ok(response);
    }
    
    // 현재 사용자 ID 조회 헬퍼 메서드 (개발용 - 고정 사용자 ID 반환)
    private Long getCurrentUserId() {
        // 개발용으로 고정 사용자 ID 반환 (실제 구현에서는 JWT에서 추출)
        return 1L; // 테스트용 사용자 ID
    }
}
