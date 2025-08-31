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
import java.util.stream.Collectors;

/**
 * 장바구니 관리 서비스
 * 관심 상품 담기, 조회, 삭제 기능 제공
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
    
    /**
     * 1. 목록조회 (필터/정렬/페이징): 사용자의 장바구니 상품 목록 조회
     * - 은행명 필터링, 상품 타입 필터링 지원
     * - 정렬: recent(최근순), interest(금리순)
     * - 페이징 처리
     */
    public CartPageResponseDTO getCartItems(Long userId, CartRequestDTO request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        Page<Cart> cartPage;
        
        String bankName = request.getBankName();
        Cart.ProductType filterProductType = request.getFilterProductType();
        String sortBy = request.getSortBy() != null ? request.getSortBy() : "recent";
        
        boolean hasBankFilter = bankName != null && !bankName.trim().isEmpty();
        boolean hasProductTypeFilter = filterProductType != null;
        boolean isInterestSort = "interest".equals(sortBy);
        
        // 필터링 + 정렬 조합에 따른 쿼리 선택
        if (hasBankFilter && hasProductTypeFilter) {
            // 은행명 + 상품타입 필터링
            if (isInterestSort) {
                cartPage = cartRepository.findByUserIdAndBankNameContainingIgnoreCaseAndProductTypeOrderByMaxInterestRateDesc(
                    userId, bankName, filterProductType, pageable);
            } else {
                cartPage = cartRepository.findByUserIdAndBankNameContainingIgnoreCaseAndProductTypeOrderByCreatedAtDesc(
                    userId, bankName, filterProductType, pageable);
            }
        } else if (hasBankFilter) {
            // 은행명만 필터링
            if (isInterestSort) {
                cartPage = cartRepository.findByUserIdAndBankNameContainingIgnoreCaseOrderByMaxInterestRateDesc(
                    userId, bankName, pageable);
            } else {
                cartPage = cartRepository.findByUserIdAndBankNameContainingIgnoreCaseOrderByCreatedAtDesc(
                    userId, bankName, pageable);
            }
        } else if (hasProductTypeFilter) {
            // 상품타입만 필터링
            if (isInterestSort) {
                cartPage = cartRepository.findByUserIdAndProductTypeOrderByMaxInterestRateDesc(
                    userId, filterProductType, pageable);
            } else {
                cartPage = cartRepository.findByUserIdAndProductTypeOrderByCreatedAtDesc(
                    userId, filterProductType, pageable);
            }
        } else {
            // 필터링 없음
            if (isInterestSort) {
                cartPage = cartRepository.findByUserIdOrderByMaxInterestRateDesc(userId, pageable);
            } else {
                cartPage = cartRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
            }
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
