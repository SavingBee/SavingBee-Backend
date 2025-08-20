package com.project.savingbee.domain.recommendation.service;

import com.project.savingbee.common.entity.*;
import com.project.savingbee.common.repository.*;
import com.project.savingbee.domain.cart.dto.CartComparisonDTO;
import com.project.savingbee.domain.cart.dto.CartResponseDTO;
import com.project.savingbee.domain.notification.dto.MaturityNotificationDTO;
import com.project.savingbee.domain.recommendation.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 상품 추천 및 비교 분석 서비스
 * 사용자 보유 상품 기반으로 더 나은 상품을 찾아 추천
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RecommendationService {
    
    private final UserProductRepository userProductRepository;
    private final DepositProductsRepository depositRepository;
    private final SavingsProductsRepository savingsRepository;
    private final DepositInterestRatesRepository depositRatesRepository;
    private final SavingsInterestRatesRepository savingsRatesRepository;
    private final CartRepository cartRepository;
    
    /**
     * 2. 해당 정보 기반으로 비슷한 상품/더 금리가 높은 상품 파악하여 사용자에게 알려줌
     * 사용자가 연 3.2% 상품을 보유하고 있을 때, 연 4.0% 상품이 있다면:
     * → "현재 상품보다 더 이자 20,000원을 더 받을 수 있는 상품이 있습니다."
     */
    public List<RecommendationResponseDTO> getRecommendationsForUser(Long userId) {
        List<UserProduct> userProducts = userProductRepository.findByUserIdAndIsActiveTrue(userId);
        
        if (userProducts.isEmpty()) {
            log.info("추천 대상 보유 상품이 없습니다: userId={}", userId);
            return Collections.emptyList();
        }
        
        List<RecommendationResponseDTO> allRecommendations = new ArrayList<>();
        
        for (UserProduct userProduct : userProducts) {
            List<RecommendationResponseDTO> productRecommendations = 
                    findBetterProducts(userProduct);
            allRecommendations.addAll(productRecommendations);
        }
        
        // 우선순위별 정렬 및 중복 제거
        return allRecommendations.stream()
                .sorted(Comparator.comparing(RecommendationResponseDTO::getPriority)
                        .thenComparing(RecommendationResponseDTO::getRateDifference, Comparator.reverseOrder()))
                .limit(10) // 상위 10개만
                .collect(Collectors.toList());
    }
    
    /**
     * 3. 해당 정보 기반으로 장바구니의 상품과 비교기능
     * 예: "현재 보유 상품보다 장바구니에 담긴 상품으로 변경 시, 연 이자 20,000원을 더 받을 수 있습니다."
     */
    public List<CartComparisonDTO> compareCartWithUserProducts(Long userId, Integer topN) {
        List<Cart> cartItems = cartRepository.findByUserIdOrderByMaxInterestRateDesc(userId);
        List<UserProduct> userProducts = userProductRepository.findByUserIdAndIsActiveTrue(userId);
        
        if (userProducts.isEmpty()) {
            return Collections.emptyList();
        }
        
        return cartItems.stream()
                .limit(topN != null ? topN : 5)
                .map(cartItem -> compareCartItemWithUserProducts(cartItem, userProducts))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(CartComparisonDTO::getRateDifference, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }
    
    /**
     * 4. 기존에 보유하고 있는 예적금 만기 때 다시 한번 파악 후 추천
     * 만기일 기준 D-30, D-7, D-1 시점에 알림 전송 및 추천
     */
    public List<MaturityNotificationDTO> getMaturityNotifications(int daysBefore) {
        LocalDate targetDate = LocalDate.now().plusDays(daysBefore);
        List<UserProduct> maturingProducts = userProductRepository.findByMaturityDate(targetDate);
        
        return maturingProducts.stream()
                .map(this::createMaturityNotification)
                .collect(Collectors.toList());
    }
    
    /**
     * 추천 요약 정보 생성
     */
    public RecommendationSummaryDTO getRecommendationSummary(Long userId) {
        List<RecommendationResponseDTO> recommendations = getRecommendationsForUser(userId);
        
        if (recommendations.isEmpty()) {
            return RecommendationSummaryDTO.builder()
                    .totalRecommendations(0)
                    .maxPotentialGain(BigDecimal.ZERO)
                    .totalPotentialGain(BigDecimal.ZERO)
                    .topRecommendations(Collections.emptyList())
                    .summaryMessage("현재 추천할 수 있는 더 나은 상품이 없습니다.")
                    .build();
        }
        
        BigDecimal maxGain = recommendations.stream()
                .map(RecommendationResponseDTO::getEstimatedExtraInterest)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        
        BigDecimal totalGain = recommendations.stream()
                .map(RecommendationResponseDTO::getEstimatedExtraInterest)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        String summaryMessage = String.format("총 %d개의 더 나은 상품을 발견했습니다. " +
                "최대 연 %,.0f원까지 더 받을 수 있습니다.",
                recommendations.size(), maxGain.doubleValue());
        
        return RecommendationSummaryDTO.builder()
                .totalRecommendations(recommendations.size())
                .maxPotentialGain(maxGain)
                .totalPotentialGain(totalGain)
                .topRecommendations(recommendations.stream().limit(3).collect(Collectors.toList()))
                .summaryMessage(summaryMessage)
                .build();
    }
    
    // 헬퍼 메서드들
    
    private List<RecommendationResponseDTO> findBetterProducts(UserProduct userProduct) {
        List<RecommendationResponseDTO> recommendations = new ArrayList<>();
        
        if (userProduct.getProductType() == UserProduct.ProductType.DEPOSIT) {
            recommendations.addAll(findBetterDepositProducts(userProduct));
        } else {
            recommendations.addAll(findBetterSavingsProducts(userProduct));
        }
        
        return recommendations;
    }
    
    private List<RecommendationResponseDTO> findBetterDepositProducts(UserProduct userProduct) {
        // 활성 예금 상품 중 더 높은 금리의 상품들 조회
        List<DepositProducts> activeProducts = depositRepository.findByIsActiveTrue();
        List<RecommendationResponseDTO> recommendations = new ArrayList<>();
        
        for (DepositProducts product : activeProducts) {
            Optional<BigDecimal> maxRate = depositRatesRepository
                    .findMaxInterestRateByProductCode(product.getFinPrdtCd());
            
            if (maxRate.isPresent() && 
                maxRate.get().compareTo(userProduct.getInterestRate()) > 0) {
                
                BigDecimal rateDiff = maxRate.get().subtract(userProduct.getInterestRate());
                BigDecimal estimatedExtra = calculateExtraInterest(
                        userProduct.getDepositAmount(), rateDiff);
                
                RecommendationResponseDTO recommendation = RecommendationResponseDTO.builder()
                        .productCode(product.getFinPrdtCd())
                        .productName(product.getFinPrdtNm())
                        .bankName(product.getFinancialCompany().getKorCoNm())
                        .productType("DEPOSIT")
                        .maxInterestRate(maxRate.get())
                        .rateDifference(rateDiff)
                        .estimatedExtraInterest(estimatedExtra)
                        .reason(String.format("현재 상품보다 %.2f%% 더 높은 금리", rateDiff.doubleValue()))
                        .priority(calculatePriority(rateDiff, estimatedExtra))
                        .baseProductName(userProduct.getProductName())
                        .baseInterestRate(userProduct.getInterestRate())
                        .baseDepositAmount(userProduct.getDepositAmount())
                        .build();
                
                recommendations.add(recommendation);
            }
        }
        
        return recommendations.stream()
                .sorted(Comparator.comparing(RecommendationResponseDTO::getRateDifference, Comparator.reverseOrder()))
                .limit(5)
                .collect(Collectors.toList());
    }
    
    private List<RecommendationResponseDTO> findBetterSavingsProducts(UserProduct userProduct) {
        List<SavingsProducts> activeProducts = savingsRepository.findByIsActiveTrue();
        List<RecommendationResponseDTO> recommendations = new ArrayList<>();
        
        for (SavingsProducts product : activeProducts) {
            Optional<BigDecimal> maxRate = savingsRatesRepository
                    .findMaxInterestRateByProductCode(product.getFinPrdtCd());
            
            if (maxRate.isPresent() && 
                maxRate.get().compareTo(userProduct.getInterestRate()) > 0) {
                
                BigDecimal rateDiff = maxRate.get().subtract(userProduct.getInterestRate());
                BigDecimal estimatedExtra = calculateExtraInterest(
                        userProduct.getDepositAmount(), rateDiff);
                
                RecommendationResponseDTO recommendation = RecommendationResponseDTO.builder()
                        .productCode(product.getFinPrdtCd())
                        .productName(product.getFinPrdtNm())
                        .bankName(product.getFinancialCompany().getKorCoNm())
                        .productType("SAVINGS")
                        .maxInterestRate(maxRate.get())
                        .rateDifference(rateDiff)
                        .estimatedExtraInterest(estimatedExtra)
                        .reason(String.format("현재 상품보다 %.2f%% 더 높은 금리", rateDiff.doubleValue()))
                        .priority(calculatePriority(rateDiff, estimatedExtra))
                        .baseProductName(userProduct.getProductName())
                        .baseInterestRate(userProduct.getInterestRate())
                        .baseDepositAmount(userProduct.getDepositAmount())
                        .build();
                
                recommendations.add(recommendation);
            }
        }
        
        return recommendations.stream()
                .sorted(Comparator.comparing(RecommendationResponseDTO::getRateDifference, Comparator.reverseOrder()))
                .limit(5)
                .collect(Collectors.toList());
    }
    
    private CartComparisonDTO compareCartItemWithUserProducts(Cart cartItem, List<UserProduct> userProducts) {
        // 같은 타입의 보유 상품 중 가장 유사한 것 찾기
        Optional<UserProduct> bestMatch = userProducts.stream()
                .filter(up -> up.getProductType().name().equals(cartItem.getProductType().name()))
                .max(Comparator.comparing(UserProduct::getDepositAmount));
        
        if (bestMatch.isPresent()) {
            UserProduct userProduct = bestMatch.get();
            BigDecimal rateDiff = cartItem.getMaxInterestRate().subtract(userProduct.getInterestRate());
            BigDecimal estimatedExtra = calculateExtraInterest(userProduct.getDepositAmount(), rateDiff);
            
            String recommendation;
            String comparisonType;
            
            if (rateDiff.compareTo(BigDecimal.ZERO) > 0) {
                comparisonType = "BETTER";
                recommendation = String.format("현재 보유 상품보다 장바구니에 담긴 상품으로 변경 시, 연 이자 %,.0f원을 더 받을 수 있습니다.",
                        estimatedExtra.doubleValue());
            } else if (rateDiff.compareTo(BigDecimal.ZERO) < 0) {
                comparisonType = "WORSE";
                recommendation = "현재 보유 상품이 더 유리합니다.";
            } else {
                comparisonType = "SAME";
                recommendation = "현재 보유 상품과 금리가 동일합니다.";
            }
            
            return CartComparisonDTO.builder()
                    .cartProduct(CartResponseDTO.from(cartItem))
                    .userProductName(userProduct.getProductName())
                    .userProductRate(userProduct.getInterestRate())
                    .rateDifference(rateDiff)
                    .estimatedExtraInterest(estimatedExtra)
                    .recommendation(recommendation)
                    .comparisonType(comparisonType)
                    .build();
        }
        
        return null;
    }
    
    private MaturityNotificationDTO createMaturityNotification(UserProduct userProduct) {
        LocalDate now = LocalDate.now();
        long daysToMaturity = now.until(userProduct.getMaturityDate()).getDays();
        
        // 만기 상품과 유사한 새로운 상품들 추천
        List<RecommendationResponseDTO> alternatives = findBetterProducts(userProduct);
        
        String notificationMessage;
        if (daysToMaturity == 30) {
            notificationMessage = String.format("%s 상품이 30일 후 만료됩니다. 새로운 상품을 검토해보세요.", userProduct.getProductName());
        } else if (daysToMaturity == 7) {
            notificationMessage = String.format("%s 상품이 7일 후 만료됩니다. 갱신 또는 다른 상품으로의 이전을 고려하세요.", userProduct.getProductName());
        } else if (daysToMaturity == 1) {
            notificationMessage = String.format("%s 상품이 내일 만료됩니다. 즉시 조치가 필요합니다.", userProduct.getProductName());
        } else {
            notificationMessage = String.format("%s 상품이 곧 만료됩니다.", userProduct.getProductName());
        }
        
        return MaturityNotificationDTO.builder()
                .userProductId(userProduct.getUserProductId())
                .productName(userProduct.getProductName())
                .bankName(userProduct.getBankName())
                .currentRate(userProduct.getInterestRate())
                .depositAmount(userProduct.getDepositAmount())
                .maturityDate(userProduct.getMaturityDate().toString())
                .daysToMaturity((int) daysToMaturity)
                .alternativeProducts(alternatives)
                .notificationMessage(notificationMessage)
                .build();
    }
    
    private BigDecimal calculateExtraInterest(BigDecimal depositAmount, BigDecimal rateDifference) {
        // 연간 추가 이자 계산
        return depositAmount.multiply(rateDifference).divide(new BigDecimal("100"), 0, BigDecimal.ROUND_HALF_UP);
    }
    
    private Integer calculatePriority(BigDecimal rateDifference, BigDecimal estimatedExtra) {
        // 금리 차이와 예상 수익을 기반으로 우선순위 계산
        // 높은 수익일수록 낮은 우선순위 값 (1이 가장 높은 우선순위)
        if (estimatedExtra.compareTo(new BigDecimal("100000")) > 0) return 1;
        if (estimatedExtra.compareTo(new BigDecimal("50000")) > 0) return 2;
        if (estimatedExtra.compareTo(new BigDecimal("20000")) > 0) return 3;
        if (estimatedExtra.compareTo(new BigDecimal("10000")) > 0) return 4;
        return 5;
    }
}
