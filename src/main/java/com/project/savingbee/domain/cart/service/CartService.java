package com.project.savingbee.domain.cart.service;

import com.project.savingbee.common.entity.*;
import com.project.savingbee.common.repository.*;
import com.project.savingbee.domain.cart.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 장바구니 관리 서비스
 * 관심 상품 담기, 조회, 삭제 및 보유 상품과의 비교 분석
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CartService {
    
    private final CartRepository cartRepository;
    private final DepositProductsRepository depositRepository;
    private final SavingsProductsRepository savingsRepository;
    private final DepositInterestRatesRepository depositRatesRepository;
    private final SavingsInterestRatesRepository savingsRatesRepository;
    private final UserProductRepository userProductRepository;
    
    /**
     * 1. 목록조회 (필터/정렬/페이징): 사용자의 장바구니 상품 목록 조회
     */
    public CartPageResponseDTO getCartItems(Long userId, CartRequestDTO request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        Page<Cart> cartPage;
        
        // 은행명 필터링
        if (request.getBankName() != null && !request.getBankName().trim().isEmpty()) {
            cartPage = cartRepository.findByUserIdAndBankNameContainingIgnoreCaseOrderByCreatedAtDesc(
                userId, request.getBankName(), pageable);
        } else {
            cartPage = cartRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        
        List<CartResponseDTO> content = cartPage.getContent().stream()
                .map(CartResponseDTO::from)
                .collect(Collectors.toList());
        
        return CartPageResponseDTO.builder()
                .content(content)
                .page(cartPage.getNumber())
                .size(cartPage.getSize())
                .totalElements(cartPage.getTotalElements())
                .totalPages(cartPage.getTotalPages())
                .first(cartPage.isFirst())
                .last(cartPage.isLast())
                .build();
    }
    
    /**
     * 2. 담기: 상품을 장바구니에 추가
     */
    @Transactional
    public CartResponseDTO addToCart(Long userId, CartRequestDTO request) {
        // 중복 체크
        if (cartRepository.existsByUserIdAndProductCodeAndProductType(
                userId, request.getProductCode(), request.getProductType())) {
            throw new IllegalArgumentException("이미 장바구니에 담긴 상품입니다.");
        }
        
        // 상품 정보 조회
        ProductInfo productInfo = getProductInfo(request.getProductCode(), request.getProductType());
        
        Cart cart = Cart.builder()
                .userId(userId)
                .productCode(request.getProductCode())
                .productType(request.getProductType())
                .bankName(productInfo.getBankName())
                .productName(productInfo.getProductName())
                .maxInterestRate(productInfo.getMaxInterestRate())
                .termMonths(productInfo.getTermMonths())
                .build();
        
        Cart savedCart = cartRepository.save(cart);
        log.info("장바구니에 상품 추가: userId={}, productCode={}", userId, request.getProductCode());
        
        return CartResponseDTO.from(savedCart);
    }
    
    /**
     * 3. 삭제(단건): 장바구니에서 해당 항목 제거
     */
    @Transactional
    public void removeFromCart(Long userId, Long cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니 항목을 찾을 수 없습니다."));
        
        if (!cart.getUserId().equals(userId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }
        
        cartRepository.delete(cart);
        log.info("장바구니에서 상품 삭제: userId={}, cartId={}", userId, cartId);
    }
    
    /**
     * 4. 선택 삭제(복수): 선택 항목 일괄 삭제
     */
    @Transactional
    public int removeMultipleFromCart(Long userId, CartRequestDTO request) {
        int deletedCount = cartRepository.deleteByCartIdsAndUserId(request.getCartIds(), userId);
        log.info("장바구니에서 복수 상품 삭제: userId={}, count={}", userId, deletedCount);
        return deletedCount;
    }
    
    /**
     * 5. 전체 비우기: 장바구니 초기화
     */
    @Transactional
    public int clearCart(Long userId) {
        int deletedCount = cartRepository.deleteAllByUserId(userId);
        log.info("장바구니 전체 비우기: userId={}, count={}", userId, deletedCount);
        return deletedCount;
    }
    
    /**
     * 6. 요약/통계: 총 담은 상품 수, 최고 금리
     */
    public CartSummaryDTO getCartSummary(Long userId) {
        long totalCount = cartRepository.countByUserId(userId);
        long depositCount = cartRepository.findByUserIdAndProductType(userId, Cart.ProductType.DEPOSIT).size();
        long savingsCount = cartRepository.findByUserIdAndProductType(userId, Cart.ProductType.SAVINGS).size();
        
        Optional<BigDecimal> maxRate = cartRepository.findMaxInterestRateByUserId(userId);
        
        String maxRateProductName = null;
        BigDecimal avgRate = BigDecimal.ZERO;
        
        if (maxRate.isPresent()) {
            List<Cart> maxRateProducts = cartRepository.findByUserIdOrderByMaxInterestRateDesc(userId);
            if (!maxRateProducts.isEmpty()) {
                maxRateProductName = maxRateProducts.get(0).getProductName();
                
                // 평균 금리 계산
                avgRate = maxRateProducts.stream()
                        .map(Cart::getMaxInterestRate)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(new BigDecimal(maxRateProducts.size()), 2, BigDecimal.ROUND_HALF_UP);
            }
        }
        
        return CartSummaryDTO.builder()
                .totalCount(totalCount)
                .depositCount(depositCount)
                .savingsCount(savingsCount)
                .maxInterestRate(maxRate.orElse(BigDecimal.ZERO))
                .maxInterestProductName(maxRateProductName)
                .avgInterestRate(avgRate)
                .build();
    }
    
    /**
     * 7. 보유 상품과 비교: 장바구니 상품 vs 사용자가 등록한 보유 상품 금리/이자 비교(상위 n개)
     */
    public List<CartComparisonDTO> compareWithUserProducts(Long userId, Integer topN) {
        List<Cart> cartItems = cartRepository.findByUserIdOrderByMaxInterestRateDesc(userId);
        List<UserProduct> userProducts = userProductRepository.findByUserIdAndIsActiveTrue(userId);
        
        if (userProducts.isEmpty()) {
            return cartItems.stream()
                    .limit(topN)
                    .map(cartItem -> CartComparisonDTO.builder()
                            .cartProduct(CartResponseDTO.from(cartItem))
                            .userProductName("보유 상품 없음")
                            .userProductRate(BigDecimal.ZERO)
                            .rateDifference(cartItem.getMaxInterestRate())
                            .estimatedExtraInterest(calculateEstimatedInterest(cartItem.getMaxInterestRate(), BigDecimal.ZERO))
                            .recommendation("보유 상품이 없어 비교할 수 없습니다.")
                            .comparisonType("NEW")
                            .build())
                    .collect(Collectors.toList());
        }
        
        return cartItems.stream()
                .limit(topN)
                .map(cartItem -> {
                    // 같은 타입의 보유 상품 중 가장 유사한 것 찾기
                    Optional<UserProduct> similarProduct = userProducts.stream()
                            .filter(up -> up.getProductType().name().equals(cartItem.getProductType().name()))
                            .min((up1, up2) -> {
                                BigDecimal diff1 = up1.getInterestRate().subtract(cartItem.getMaxInterestRate()).abs();
                                BigDecimal diff2 = up2.getInterestRate().subtract(cartItem.getMaxInterestRate()).abs();
                                return diff1.compareTo(diff2);
                            });
                    
                    if (similarProduct.isPresent()) {
                        UserProduct userProduct = similarProduct.get();
                        BigDecimal rateDiff = cartItem.getMaxInterestRate().subtract(userProduct.getInterestRate());
                        BigDecimal estimatedExtra = calculateEstimatedInterest(cartItem.getMaxInterestRate(), userProduct.getInterestRate());
                        
                        String comparisonType;
                        String recommendation;
                        
                        if (rateDiff.compareTo(BigDecimal.ZERO) > 0) {
                            comparisonType = "BETTER";
                            recommendation = String.format("현재 보유 상품보다 연 %.2f%% 더 높은 금리로 약 %,.0f원을 더 받을 수 있습니다.",
                                    rateDiff.doubleValue(), estimatedExtra.doubleValue());
                        } else if (rateDiff.compareTo(BigDecimal.ZERO) < 0) {
                            comparisonType = "WORSE";
                            recommendation = String.format("현재 보유 상품이 연 %.2f%% 더 높은 금리입니다.",
                                    rateDiff.abs().doubleValue());
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
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * 8. 최근 담은 순 정렬
     */
    public List<CartResponseDTO> getCartItemsByRecent(Long userId) {
        return cartRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(CartResponseDTO::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 9. 은행명 필터
     */
    public List<CartResponseDTO> getCartItemsByBank(Long userId, String bankName) {
        return cartRepository.findByUserIdAndBankNameContainingIgnoreCase(userId, bankName).stream()
                .map(CartResponseDTO::from)
                .collect(Collectors.toList());
    }
    
    // 헬퍼 메서드들
    private ProductInfo getProductInfo(String productCode, Cart.ProductType productType) {
        if (productType == Cart.ProductType.DEPOSIT) {
            DepositProducts product = depositRepository.findById(productCode)
                    .orElseThrow(() -> new IllegalArgumentException("예금 상품을 찾을 수 없습니다."));
            
            BigDecimal maxRate = depositRatesRepository.findMaxInterestRateByProductCode(productCode)
                    .orElse(BigDecimal.ZERO);
            
            // 대표 기간 조회 (가장 일반적인 12개월로 설정)
            Integer termMonths = depositRatesRepository.findByFinPrdtCd(productCode).stream()
                    .map(DepositInterestRates::getSaveTrm)
                    .findFirst()
                    .orElse(12);
            
            return new ProductInfo(
                    product.getFinancialCompany().getKorCoNm(),
                    product.getFinPrdtNm(),
                    maxRate,
                    termMonths
            );
        } else {
            SavingsProducts product = savingsRepository.findById(productCode)
                    .orElseThrow(() -> new IllegalArgumentException("적금 상품을 찾을 수 없습니다."));
            
            BigDecimal maxRate = savingsRatesRepository.findMaxInterestRateByProductCode(productCode)
                    .orElse(BigDecimal.ZERO);
            
            Integer termMonths = savingsRatesRepository.findByFinPrdtCd(productCode).stream()
                    .map(SavingsInterestRates::getSaveTrm)
                    .findFirst()
                    .orElse(12);
            
            return new ProductInfo(
                    product.getFinancialCompany().getKorCoNm(),
                    product.getFinPrdtNm(),
                    maxRate,
                    termMonths
            );
        }
    }
    
    private BigDecimal calculateEstimatedInterest(BigDecimal newRate, BigDecimal currentRate) {
        // 1000만원 기준으로 연간 이자 차이 계산
        BigDecimal baseAmount = new BigDecimal("10000000");
        BigDecimal rateDifference = newRate.subtract(currentRate);
        return baseAmount.multiply(rateDifference).divide(new BigDecimal("100"), 0, BigDecimal.ROUND_HALF_UP);
    }
    
    // 내부 클래스
    @lombok.Getter
    @lombok.AllArgsConstructor
    private static class ProductInfo {
        private String bankName;
        private String productName;
        private BigDecimal maxInterestRate;
        private Integer termMonths;
    }
}
