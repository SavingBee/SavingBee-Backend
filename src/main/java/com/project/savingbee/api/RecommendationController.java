package com.project.savingbee.api;

import com.project.savingbee.domain.cart.dto.CartComparisonDTO;
import com.project.savingbee.domain.notification.dto.MaturityNotificationDTO;
import com.project.savingbee.domain.recommendation.dto.*;
import com.project.savingbee.domain.recommendation.service.RecommendationService;
import com.project.savingbee.domain.notification.service.MaturityNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 상품 추천 및 비교 분석 API 컨트롤러
 * 보유 상품 기반 추천, 장바구니 비교, 만기 알림 기능 제공
 */
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {
    
    private final RecommendationService recommendationService;
    private final MaturityNotificationService maturityNotificationService;
    
    /**
     * 2. 해당 정보 기반으로 비슷한 상품/더 금리가 높은 상품 파악하여 사용자에게 알려줌
     * 사용자가 연 3.2% 상품을 보유하고 있을 때, 연 4.0% 상품이 있다면:
     * → "현재 상품보다 더 이자 20,000원을 더 받을 수 있는 상품이 있습니다."
     */
    @GetMapping
    public ResponseEntity<List<RecommendationResponseDTO>> getRecommendations() {
        Long userId = getCurrentUserId();
        List<RecommendationResponseDTO> response = recommendationService.getRecommendationsForUser(userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 추천 요약 정보 조회
     */
    @GetMapping("/summary")
    public ResponseEntity<RecommendationSummaryDTO> getRecommendationSummary() {
        Long userId = getCurrentUserId();
        RecommendationSummaryDTO response = recommendationService.getRecommendationSummary(userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 3. 해당 정보 기반으로 장바구니의 상품과 비교기능
     * 예: "현재 보유 상품보다 장바구니에 담긴 상품으로 변경 시, 연 이자 20,000원을 더 받을 수 있습니다."
     */
    @GetMapping("/compare-cart")
    public ResponseEntity<List<CartComparisonDTO>> compareCartWithUserProducts(
            @RequestParam(defaultValue = "5") Integer topN) {
        Long userId = getCurrentUserId();
        List<CartComparisonDTO> response = recommendationService.compareCartWithUserProducts(userId, topN);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 4. 기존에 보유하고 있는 예적금 만기 때 다시 한번 파악 후 추천
     * 만기일 기준 D-30, D-7, D-1 시점에 알림 전송 및 추천
     */
    @GetMapping("/maturity-notifications")
    public ResponseEntity<List<MaturityNotificationDTO>> getMaturityNotifications() {
        Long userId = getCurrentUserId();
        List<MaturityNotificationDTO> response = maturityNotificationService.getUserMaturityNotifications(userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 특정 일수 전 만기 상품들 조회
     */
    @GetMapping("/maturity/{daysBefore}")
    public ResponseEntity<List<MaturityNotificationDTO>> getMaturityNotificationsByDays(@PathVariable int daysBefore) {
        List<MaturityNotificationDTO> response = recommendationService.getMaturityNotifications(daysBefore);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 수동 만기 알림 전송 (관리자용)
     */
    @PostMapping("/maturity-notifications/send/{daysBefore}")
    public ResponseEntity<Map<String, String>> sendManualMaturityNotifications(@PathVariable int daysBefore) {
        maturityNotificationService.sendManualMaturityNotifications(daysBefore);
        return ResponseEntity.ok(Map.of(
                "message", String.format("D-%d 만기 알림이 전송되었습니다.", daysBefore)
        ));
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
